package com.swill.vpn.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.swill.vpn.config.AppConfig
import com.swill.vpn.databinding.ActivitySubscriptionBinding
import com.swill.vpn.model.SubscriptionParser
import com.swill.vpn.model.VpnConfig

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var appConfig: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnImport.setOnClickListener {
            val url = binding.etSubscriptionUrl.text.toString()
            if (url.isBlank()) {
                Toast.makeText(this, "Please enter subscription URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            importSubscription(url)
        }

        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                text?.let { binding.etSubscriptionUrl.setText(it) }
            }
        }
    }

    private fun importSubscription(url: String) {
        val configs = SubscriptionParser.parseSubscription(url)

        if (configs.isEmpty()) {
            Toast.makeText(this, "Failed to parse subscription", Toast.LENGTH_SHORT).show()
            return
        }

        var importedCount = 0
        for (config in configs) {
            if (appConfig.saveConfig(config)) {
                importedCount++
            }
        }

        Toast.makeText(this, "Imported $importedCount servers", Toast.LENGTH_SHORT).show()
        finish()
    }
}
