package app.parvaz.vpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import app.parvaz.vpn.MainActivity
import app.parvaz.vpn.R
import app.parvaz.vpn.core.XrayCore
import app.parvaz.vpn.data.LinkParser
import app.parvaz.vpn.data.XrayConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class VpnState { Disconnected, Connecting, Connected }

class ParvazVpnService : VpnService() {

    companion object {
        const val ACTION_START = "app.parvaz.vpn.START"
        const val ACTION_STOP = "app.parvaz.vpn.STOP"
        const val EXTRA_LINK = "link"
        private const val CHANNEL = "vpn"

        val state = MutableStateFlow(VpnState.Disconnected)
        val lastError = MutableStateFlow<String?>(null)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private var tun: ParcelFileDescriptor? = null
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> shutdown()
            ACTION_START -> startTunnel(intent.getStringExtra(EXTRA_LINK))
        }
        return START_NOT_STICKY
    }

    private fun startTunnel(link: String?) {
        val parsed = link?.let { LinkParser.parse(it) }
        startForeground(1, notification(parsed?.name ?: ""))
        if (parsed == null) {
            fail("Invalid server link")
            return
        }
        job?.cancel()
        lastError.value = null
        state.value = VpnState.Connecting
        job = scope.launch {
            try {
                val config = XrayConfig.build(parsed)
                val pfd = Builder()
                    .setSession(getString(R.string.app_name))
                    .setMtu(1500)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    // Our own sockets (to the proxy server) must bypass the tunnel.
                    .addDisallowedApplication(packageName)
                    .establish() ?: throw IllegalStateException("VPN permission not granted")
                tun = pfd
                XrayCore.start(applicationContext, config, pfd.fd)
                state.value = VpnState.Connected
            } catch (e: Throwable) {
                main.post { fail(e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    private fun fail(msg: String) {
        lastError.value = msg
        shutdown()
    }

    private fun shutdown() {
        job?.cancel()
        job = null
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanup() {
        try { XrayCore.stop() } catch (_: Throwable) {}
        try { tun?.close() } catch (_: Throwable) {}
        tun = null
        state.value = VpnState.Disconnected
    }

    override fun onRevoke() {
        shutdown()
    }

    override fun onDestroy() {
        cleanup()
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(name: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, ParvazVpnService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_text, name))
            .setContentIntent(open)
            .addAction(0, getString(R.string.notif_stop), stop)
            .setOngoing(true)
            .build()
    }
}
