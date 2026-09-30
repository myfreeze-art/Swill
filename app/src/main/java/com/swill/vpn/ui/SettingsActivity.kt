package com.swill.vpn.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.swill.vpn.R
import com.swill.vpn.config.AppConfig
import com.swill.vpn.databinding.ActivitySettingsBinding
import com.swill.vpn.vpn.VpnService

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var appConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.switchAutoConnect.isChecked = appConfig.isAutoConnectOnBoot()

        when (appConfig.getDefaultCore()) {
            VpnService.CORE_SINGBOX -> binding.rbSingBox.isChecked = true
            else -> binding.rbXray.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.switchAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            appConfig.setAutoConnectOnBoot(isChecked)
            Toast.makeText(this, "Auto-connect ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.coreSelector.setOnCheckedChangeListener { _, checkedId ->
            val coreType = when (checkedId) {
                R.id.rbXray -> VpnService.CORE_XRAY
                R.id.rbSingBox -> VpnService.CORE_SINGBOX
                else -> VpnService.CORE_XRAY
            }
            appConfig.setDefaultCore(coreType)
            Toast.makeText(this, "Default core set to $coreType", Toast.LENGTH_SHORT).show()
        }

        binding.btnSplitTunnel.setOnClickListener {
            val intent = Intent(this, SplitTunnelActivity::class.java)
            startActivity(intent)
        }
    }
}
