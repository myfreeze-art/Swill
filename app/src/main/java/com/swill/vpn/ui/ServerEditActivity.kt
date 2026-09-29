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
        
        // Load existing config if provided
        currentConfig = intent.getParcelableExtra("server")
        
        setupUI()
        setupListeners()
        
        if (currentConfig != null) {
            loadConfig(currentConfig!!)
        }
    }
    
    private fun setupUI() {
        // Setup protocol spinner
        val protocols = listOf(
            VpnConfig.PROTOCOL_VLESS,
            VpnConfig.PROTOCOL_VMESS,
            VpnConfig.PROTOCOL_TROJAN,
            VpnConfig.PROTOCOL_SHADOWSOCKS
        )
        
        val protocolAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, protocols)
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProtocol.adapter = protocolAdapter
        
        // Setup network spinner
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
        
        // Setup security spinner
        val securities = listOf(
            VpnConfig.SECURITY_NONE,
            VpnConfig.SECURITY_TLS,
            VpnConfig.SECURITY_REALITY
        )
        
        val securityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, securities)
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSecurity.adapter = securityAdapter
        
        // Setup core spinner
        val cores = listOf(VpnService.CORE_XRAY, VpnService.CORE_SINGBOX)
        val coreAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cores)
        coreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCore.adapter = coreAdapter
    }
    
    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveConfig()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnGenerateUuid.setOnClickListener {
            binding.etUuid.setText(UUID.randomUUID().toString())
        }
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
        
        // Set spinners
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
        
        // Validate
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
            coreType = coreType
        )
        
        appConfig.saveConfig(config)
        
        Toast.makeText(this, "Server saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
