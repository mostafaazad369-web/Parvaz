package app.parvaz.vpn.core

import android.content.Context
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

/**
 * Thin wrapper around the Xray-core Go library (libv2ray.aar from AndroidLibXrayLite).
 * Every call into the native library lives in this single file, so if the upstream
 * API changes in a newer release, this is the only file to adjust.
 */
object XrayCore {
    private var controller: CoreController? = null
    private var initialised = false

    private val callback = object : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long = 0
        override fun onEmitStatus(l: Long, s: String?): Long = 0
    }

    @Synchronized
    fun start(ctx: Context, config: String, tunFd: Int) {
        if (!initialised) {
            Libv2ray.initCoreEnv(ctx.filesDir.absolutePath, "parvaz-xudp-key")
            initialised = true
        }
        stop()
        val c = Libv2ray.newCoreController(callback)
        c.startLoop(config, tunFd)
        controller = c
    }

    @Synchronized
    fun stop() {
        try {
            controller?.stopLoop()
        } catch (_: Throwable) {
        }
        controller = null
    }

    val isRunning: Boolean get() = controller?.isRunning ?: false
}
