package com.swill.vpn.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.swill.vpn.R
import com.swill.vpn.config.AppConfig
import com.swill.vpn.databinding.ActivityServerEditBinding
import com.swill.vpn.model.VpnConfig
import com.swill.vpn.vpn.VpnService
import java.util.UUID

class ServerEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerEditBinding
    private lateinit var appConfig: AppConfig
    private var currentConfig: VpnConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityServerEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)
        currentConfig = intent.getParcelableExtra("server")

        setupUI()
        setupListeners()

        currentConfig?.let { loadConfig(it) }

        updateHysteria2FieldsVisibility()
    }

    private fun setupUI() {
        val protocols = listOf(
            VpnConfig.PROTOCOL_VLESS,
            VpnConfig.PROTOCOL_VMESS,
            VpnConfig.PROTOCOL_TROJAN,
            VpnConfig.PROTOCOL_SHADOWSOCKS,
            VpnConfig.PROTOCOL_HYSTERIA2
        )

        val protocolAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, protocols)
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProtocol.adapter = protocolAdapter

        val networks = listOf(
            VpnConfig.NETWORK_TCP,
            VpnConfig.NETWORK_WS,
            VpnConfig.NETWORK_GRPC,
            VpnConfig.NETWORK_KCP,
            VpnConfig.NETWORK_QUIC
        )

        val networkAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, networks)
        networkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNetwork.adapter = networkAdapter

        val securities = listOf(
            VpnConfig.SECURITY_NONE,
            VpnConfig.SECURITY_TLS,
            VpnConfig.SECURITY_REALITY
        )

        val securityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, securities)
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSecurity.adapter = securityAdapter

        val cores = listOf(VpnService.CORE_XRAY, VpnService.CORE_SINGBOX)
        val coreAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cores)
        coreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCore.adapter = coreAdapter

        // Hysteria2 Obfs spinner
        val hysteria2ObfsOptions = listOf(
            VpnConfig.ObfsNone,
            VpnConfig.ObfsSalamander,
            VpnConfig.ObfsFaketls
        )
        val hysteria2ObfsAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hysteria2ObfsOptions)
        hysteria2ObfsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHysteria2Obfs.adapter = hysteria2ObfsAdapter
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener { saveConfig() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnGenerateUuid.setOnClickListener {
            binding.etUuid.setText(UUID.randomUUID().toString())
        }

        binding.spinnerProtocol.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateHysteria2FieldsVisibility()
                // Hysteria2 only works with Sing-box core
                val selectedProtocol = parent?.getItemAtPosition(position).toString()
                if (selectedProtocol == VpnConfig.PROTOCOL_HYSTERIA2) {
                    // Auto-select Sing-box for Hysteria2
                    binding.spinnerCore.setSelection(
                        (binding.spinnerCore.adapter as ArrayAdapter<String>).getPosition(VpnService.CORE_SINGBOX)
                    )
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun updateHysteria2FieldsVisibility() {
        val selectedProtocol = binding.spinnerProtocol.selectedItem.toString()
        val isHysteria2 = selectedProtocol == VpnConfig.PROTOCOL_HYSTERIA2

        binding.tvHysteria2SettingsLabel.visibility = if (isHysteria2) android.view.View.VISIBLE else android.view.View.GONE
        binding.llHysteria2Obfs.visibility = if (isHysteria2) android.view.View.VISIBLE else android.view.View.GONE
        binding.llHysteria2AuthPassword.visibility = if (isHysteria2) android.view.View.VISIBLE else android.view.View.GONE
        binding.llHysteria2ObfsPassword.visibility = if (isHysteria2) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadConfig(config: VpnConfig) {
        binding.etName.setText(config.name)
        binding.etServerAddress.setText(config.serverAddress)
        binding.etServerPort.setText(config.serverPort.toString())
        binding.etUuid.setText(config.uuid)
        binding.etAlterId.setText(config.alterId?.toString() ?: "")
        binding.etRequestHost.setText(config.requestHost ?: "")
        binding.etPath.setText(config.path ?: "")
        binding.etSni.setText(config.sni ?: "")
        binding.cbAllowInsecure.isChecked = config.allowInsecure

        binding.spinnerProtocol.setSelection(
            (binding.spinnerProtocol.adapter as ArrayAdapter<String>).getPosition(config.protocol)
        )

        config.network?.let {
            binding.spinnerNetwork.setSelection(
                (binding.spinnerNetwork.adapter as ArrayAdapter<String>).getPosition(it)
            )
        }

        config.security?.let {
            binding.spinnerSecurity.setSelection(
                (binding.spinnerSecurity.adapter as ArrayAdapter<String>).getPosition(it)
            )
        }

        binding.spinnerCore.setSelection(
            (binding.spinnerCore.adapter as ArrayAdapter<String>).getPosition(config.coreType)
        )

        // Hysteria2 specific fields
        binding.etHysteria2AuthPassword.setText(config.hysteria2AuthPassword ?: "")
        binding.etHysteria2ObfsPassword.setText(config.hysteria2ObfsPassword ?: "")
        config.hysteria2Obfs?.let {
            binding.spinnerHysteria2Obfs.setSelection(
                (binding.spinnerHysteria2Obfs.adapter as ArrayAdapter<String>).getPosition(it)
            )
        }

        // Bypass fields
        binding.cbBypassEnabled.isChecked = config.bypassEnabled
        binding.etBypassDomains.setText(config.bypassDomains ?: "")
        binding.etBypassIps.setText(config.bypassIps ?: "")
        binding.etBypassGeoip.setText(config.bypassGeoip ?: "")
    }

    private fun saveConfig() {
        val name = binding.etName.text.toString().ifEmpty { "Untitled" }
        val serverAddress = binding.etServerAddress.text.toString()
        val serverPort = binding.etServerPort.text.toString().toIntOrNull() ?: 0
        val uuid = binding.etUuid.text.toString()
        val alterId = binding.etAlterId.text.toString().toIntOrNull()
        val requestHost = binding.etRequestHost.text.toString().ifEmpty { null }
        val path = binding.etPath.text.toString().ifEmpty { null }
        val sni = binding.etSni.text.toString().ifEmpty { null }
        val allowInsecure = binding.cbAllowInsecure.isChecked

        val protocol = binding.spinnerProtocol.selectedItem.toString()
        val network = if (binding.spinnerNetwork.selectedItemPosition > 0)
            binding.spinnerNetwork.selectedItem.toString() else null
        val security = if (binding.spinnerSecurity.selectedItemPosition > 0)
            binding.spinnerSecurity.selectedItem.toString() else null
        val coreType = binding.spinnerCore.selectedItem.toString()

        // Hysteria2 specific fields
        val hysteria2AuthPassword = binding.etHysteria2AuthPassword.text.toString().ifEmpty { null }
        val hysteria2ObfsPassword = binding.etHysteria2ObfsPassword.text.toString().ifEmpty { null }
        val hysteria2Obfs = if (binding.spinnerHysteria2Obfs.selectedItemPosition > 0)
            binding.spinnerHysteria2Obfs.selectedItem.toString() else null

        // Bypass fields
        val bypassEnabled = binding.cbBypassEnabled.isChecked
        val bypassDomains = binding.etBypassDomains.text.toString().ifEmpty { null }
        val bypassIps = binding.etBypassIps.text.toString().ifEmpty { null }
        val bypassGeoip = binding.etBypassGeoip.text.toString().ifEmpty { null }

        if (serverAddress.isEmpty()) {
            Toast.makeText(this, "Please enter server address", Toast.LENGTH_SHORT).show()
            return
        }

        if (serverPort == 0) {
            Toast.makeText(this, "Please enter server port", Toast.LENGTH_SHORT).show()
            return
        }

        if (uuid.isEmpty()) {
            Toast.makeText(this, "Please enter UUID/password", Toast.LENGTH_SHORT).show()
            return
        }

        val config = VpnConfig(
            id = currentConfig?.id ?: System.currentTimeMillis().toString(),
            name = name,
            serverAddress = serverAddress,
            serverPort = serverPort,
            protocol = protocol,
            uuid = uuid,
            alterId = alterId,
            security = security,
            network = network,
            headerType = "none",
            requestHost = requestHost,
            path = path,
            sni = sni,
            allowInsecure = allowInsecure,
            coreType = coreType,
            hysteria2AuthPassword = hysteria2AuthPassword,
            hysteria2Obfs = hysteria2Obfs,
            hysteria2ObfsPassword = hysteria2ObfsPassword,
            bypassEnabled = bypassEnabled,
            bypassDomains = bypassDomains,
            bypassIps = bypassIps,
            bypassGeoip = bypassGeoip
        )

        appConfig.saveConfig(config)

        Toast.makeText(this, "Server saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
