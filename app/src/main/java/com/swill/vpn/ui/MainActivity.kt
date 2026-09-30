package com.swill.vpn.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.swill.vpn.R
import com.swill.vpn.config.AppConfig
import com.swill.vpn.core.AutoUpdater
import com.swill.vpn.core.ServerPinger
import com.swill.vpn.databinding.ActivityMainBinding
import com.swill.vpn.jni.NativeUtils
import com.swill.vpn.model.VpnConfig
import com.swill.vpn.vpn.VpnService
import com.swill.vpn.vpn.VpnService.VpnBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VPN_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private var vpnService: VpnService? = null
    private var isBound = false
    private lateinit var appConfig: AppConfig
    private var serverAdapter: ServerAdapter? = null
    private val serverPinger = lazy { ServerPinger(this) }
    private val autoUpdater = lazy { AutoUpdater(this) }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VpnBinder
            vpnService = binder.getService()
            isBound = true
            updateUI()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            vpnService = null
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)

        checkNativeLibraries()
        setupUI()
        bindToVpnService()
        loadServers()

        autoUpdateSubscriptionsOnStartup()
        autoPingServersOnStartup()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun checkNativeLibraries() {
        if (!NativeUtils.areNativeLibrariesAvailable()) {
            Toast.makeText(this, "Native libraries not available", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        serverAdapter = ServerAdapter(
            servers = appConfig.getAllConfigs(),
            onServerSelected = { server -> onServerSelected(server) },
            onServerEdit = { server -> openServerEdit(server) },
            onServerDelete = { server -> deleteServer(server) },
            onServerPing = { server -> pingServer(server) }
        )

        binding.rvServers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = serverAdapter
        }

        binding.btnConnect.setOnClickListener {
            if (isBound && vpnService?.getStatus()?.isRunning == true) {
                stopVpn()
            } else {
                val selectedServer = serverAdapter?.getSelectedServer()
                if (selectedServer != null) {
                    requestVpnPermission(selectedServer)
                } else {
                    Toast.makeText(this, "Please select a server", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAddServer.setOnClickListener { openServerEdit(null) }
        binding.btnSettings.setOnClickListener { openSettings() }
        binding.btnImport.setOnClickListener { openSubscriptionImport() }

        binding.coreSelector.setOnCheckedChangeListener { _, checkedId ->
            val coreType = when (checkedId) {
                R.id.rbXray -> VpnService.CORE_XRAY
                R.id.rbSingBox -> VpnService.CORE_SINGBOX
                else -> VpnService.CORE_XRAY
            }
            appConfig.setDefaultCore(coreType)
        }

        when (appConfig.getDefaultCore()) {
            VpnService.CORE_SINGBOX -> binding.rbSingBox.isChecked = true
            else -> binding.rbXray.isChecked = true
        }
    }

    private fun loadServers() {
        val servers = appConfig.getAllConfigs()
        serverAdapter?.updateServers(servers)

        if (servers.isNotEmpty()) {
            serverAdapter?.selectServer(servers.first())
        }
    }

    private fun bindToVpnService() {
        val intent = Intent(this, VpnService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun onServerSelected(server: VpnConfig) {
        serverAdapter?.selectServer(server)
    }

    private fun openServerEdit(server: VpnConfig?) {
        val intent = Intent(this, ServerEditActivity::class.java).apply {
            server?.let { putExtra("server", it) }
        }
        startActivity(intent)
    }

    private fun deleteServer(server: VpnConfig) {
        appConfig.deleteConfig(server.id)
        loadServers()
        Toast.makeText(this, "Server deleted", Toast.LENGTH_SHORT).show()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun openSubscriptionImport() {
        val intent = Intent(this, SubscriptionActivity::class.java)
        startActivity(intent)
    }

    private fun requestVpnPermission(server: VpnConfig) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpn(server)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedServer = serverAdapter?.getSelectedServer()
            selectedServer?.let { startVpn(it) }
        }
    }

    private fun startVpn(server: VpnConfig) {
        if (!isBound) {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            return
        }

        val coreType = if (binding.rbXray.isChecked) VpnService.CORE_XRAY else VpnService.CORE_SINGBOX

        val intent = Intent(this, VpnService::class.java).apply {
            action = VpnService.ACTION_START
            putExtra(VpnService.EXTRA_CONFIG, server)
            putExtra(VpnService.EXTRA_CORE_TYPE, coreType)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        appConfig.saveLastConfig(server, coreType)
        updateUI()
    }

    private fun stopVpn() {
        if (!isBound) {
            Toast.makeText(this, "Service not bound", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, VpnService::class.java).apply {
            action = VpnService.ACTION_STOP
        }

        startService(intent)
        updateUI()
    }

    private fun updateUI() {
        val isRunning = vpnService?.getStatus()?.isRunning ?: false

        if (isRunning) {
            binding.btnConnect.text = getString(R.string.btn_disconnect)
            binding.btnConnect.setBackgroundColor(getColor(R.color.colorError))
            binding.statusText.text = getString(R.string.status_connected)
            binding.statusText.setTextColor(getColor(R.color.colorConnected))
        } else {
            binding.btnConnect.text = getString(R.string.btn_connect)
            binding.btnConnect.setBackgroundColor(getColor(R.color.colorPrimary))
            binding.statusText.text = getString(R.string.status_disconnected)
            binding.statusText.setTextColor(getColor(R.color.colorDisconnected))
        }
    }

    private fun pingServer(server: VpnConfig) {
        Toast.makeText(this, "Pinging ${server.name}...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val pingMethod = appConfig.getPingMethod()
            val results = withContext(Dispatchers.IO) {
                serverPinger.value.pingServer(
                    server.serverAddress,
                    server.serverPort,
                    listOf(pingMethod)
                )
            }

            val bestResult = results.find { it is ServerPinger.PingResult.Success } as? ServerPinger.PingResult.Success
            serverAdapter?.updatePingResult(
                server.id,
                bestResult ?: ServerPinger.PingResult.Failure("All methods failed", pingMethod)
            )

            if (bestResult != null) {
                Toast.makeText(
                    this@MainActivity,
                    "Ping: ${bestResult.latency}ms (${bestResult.method})",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val error = (results.firstOrNull() as? ServerPinger.PingResult.Failure)?.error ?: "Unknown error"
                Toast.makeText(this@MainActivity, "Ping failed: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun autoUpdateSubscriptionsOnStartup() {
        autoUpdater.value.updateSubscriptionsOnStartup()
    }

    private fun autoPingServersOnStartup() {
        val servers = appConfig.getAllConfigs()
        if (servers.isEmpty()) return

        autoUpdater.value.pingServersOnStartup(servers) { updatedServers ->
            serverAdapter?.updateServers(updatedServers)
            if (updatedServers.isNotEmpty()) {
                serverAdapter?.selectServer(updatedServers.first())
            }
        }
    }
}
