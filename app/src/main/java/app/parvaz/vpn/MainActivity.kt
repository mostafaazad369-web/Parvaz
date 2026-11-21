package app.parvaz.vpn

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import app.parvaz.vpn.ui.ParvazApp
import app.parvaz.vpn.ui.ParvazTheme
import app.parvaz.vpn.vpn.ParvazVpnService
import app.parvaz.vpn.vpn.VpnState

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) startVpn()
        }
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            ParvazTheme(vm.themeMode) {
                ParvazApp(vm, onToggle = { toggle() })
            }
        }
    }

    private fun toggle() {
        when (ParvazVpnService.state.value) {
            VpnState.Disconnected -> {
                if (vm.selectedNode == null) {
                    vm.message = Msg(R.string.no_server)
                    return
                }
                val prepare = VpnService.prepare(this)
                if (prepare != null) vpnPermission.launch(prepare) else startVpn()
            }
            else -> startService(
                Intent(this, ParvazVpnService::class.java).setAction(ParvazVpnService.ACTION_STOP),
            )
        }
    }

    private fun startVpn() {
        val node = vm.selectedNode ?: return
        val i = Intent(this, ParvazVpnService::class.java)
            .setAction(ParvazVpnService.ACTION_START)
            .putExtra(ParvazVpnService.EXTRA_LINK, node.link)
        ContextCompat.startForegroundService(this, i)
    }
}
