package com.swill.vpn.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.swill.vpn.config.AppConfig
import com.swill.vpn.core.SplitTunnelManager
import com.swill.vpn.databinding.ActivitySplitTunnelBinding
import com.swill.vpn.model.SplitTunnelApp

class SplitTunnelActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplitTunnelBinding
    private lateinit var appConfig: AppConfig
    private lateinit var splitTunnelManager: SplitTunnelManager
    private var apps: List<SplitTunnelApp> = emptyList()
    private var appAdapter: SplitTunnelAppAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplitTunnelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)
        splitTunnelManager = SplitTunnelManager(this)

        setupUI()
        loadApps()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rgMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbAllThroughVpn -> SplitTunnelManager.SplitTunnelMode.ALL_THROUGH_VPN
                R.id.rbAllBypass -> SplitTunnelManager.SplitTunnelMode.ALL_BYPASS
                else -> SplitTunnelManager.SplitTunnelMode.CUSTOM
            }
            appConfig.setSplitTunnelMode(mode.name)
            updateAppListVisibility()
        }

        when (appConfig.getSplitTunnelMode()) {
            SplitTunnelManager.SplitTunnelMode.ALL_THROUGH_VPN.name -> binding.rbAllThroughVpn.isChecked = true
            SplitTunnelManager.SplitTunnelMode.ALL_BYPASS.name -> binding.rbAllBypass.isChecked = true
            else -> binding.rbCustom.isChecked = true
        }

        binding.btnSave.setOnClickListener { saveSettings() }
    }

    private fun loadApps() {
        apps = splitTunnelManager.getInstalledApps()
        val savedApps = appConfig.getSplitTunnelApps()

        val mergedApps = apps.map { app ->
            val savedApp = savedApps.find { it.packageName == app.packageName }
            if (savedApp != null) {
                app.copy(enabled = savedApp.enabled, routeThroughVpn = savedApp.routeThroughVpn)
            } else {
                app
            }
        }

        apps = mergedApps
        appAdapter = SplitTunnelAppAdapter(
            apps = apps,
            onAppToggle = { app, enabled -> toggleApp(app, enabled) },
            onRouteToggle = { app, routeThroughVpn -> toggleRoute(app, routeThroughVpn) }
        )

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@SplitTunnelActivity)
            adapter = appAdapter
        }

        updateAppListVisibility()
    }

    private fun updateAppListVisibility() {
        val isCustomMode = binding.rbCustom.isChecked
        binding.rvApps.visibility = if (isCustomMode) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvAppsLabel.visibility = if (isCustomMode) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleApp(app: SplitTunnelApp, enabled: Boolean) {
        apps = apps.map {
            if (it.packageName == app.packageName) {
                it.copy(enabled = enabled)
            } else {
                it
            }
        }
        appAdapter?.updateApps(apps)
    }

    private fun toggleRoute(app: SplitTunnelApp, routeThroughVpn: Boolean) {
        apps = apps.map {
            if (it.packageName == app.packageName) {
                it.copy(routeThroughVpn = routeThroughVpn)
            } else {
                it
            }
        }
        appAdapter?.updateApps(apps)
    }

    private fun saveSettings() {
        val mode = when {
            binding.rbAllThroughVpn.isChecked -> SplitTunnelManager.SplitTunnelMode.ALL_THROUGH_VPN
            binding.rbAllBypass.isChecked -> SplitTunnelManager.SplitTunnelMode.ALL_BYPASS
            else -> SplitTunnelManager.SplitTunnelMode.CUSTOM
        }

        appConfig.setSplitTunnelMode(mode.name)
        appConfig.saveSplitTunnelApps(apps.filter { it.enabled })

        Toast.makeText(this, "Split tunnel settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
