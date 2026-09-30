package com.swill.vpn.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.swill.vpn.config.AppConfig
import com.swill.vpn.core.SubscriptionUpdater
import com.swill.vpn.databinding.ActivitySubscriptionBinding
import com.swill.vpn.model.Subscription
import com.swill.vpn.model.VpnConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var appConfig: AppConfig
    private var subscriptions: List<Subscription> = emptyList()
    private var subscriptionAdapter: SubscriptionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appConfig = AppConfig(this)

        setupUI()
        loadSubscriptions()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        subscriptionAdapter = SubscriptionAdapter(
            subscriptions = subscriptions,
            onSubscriptionUpdate = { subscription -> updateSubscription(subscription) },
            onSubscriptionDelete = { subscription -> deleteSubscription(subscription) }
        )

        binding.rvSubscriptions.apply {
            layoutManager = LinearLayoutManager(this@SubscriptionActivity)
            adapter = subscriptionAdapter
        }

        binding.btnAddSubscription.setOnClickListener { showAddSubscriptionDialog() }

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

    private fun loadSubscriptions() {
        subscriptions = appConfig.getAllSubscriptions()
        subscriptionAdapter?.updateSubscriptions(subscriptions)
    }

    private fun showAddSubscriptionDialog() {
        val url = binding.etSubscriptionUrl.text.toString()
        if (url.isBlank()) {
            Toast.makeText(this, "Please enter subscription URL", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.etSubscriptionName.text.toString().ifEmpty { "Subscription ${subscriptions.size + 1}" }
        val autoUpdate = binding.cbAutoUpdate.isChecked

        val subscription = Subscription(
            name = name,
            url = url,
            autoUpdate = autoUpdate
        )

        if (appConfig.saveSubscription(subscription)) {
            loadSubscriptions()
            binding.etSubscriptionUrl.setText("")
            binding.etSubscriptionName.setText("")
            binding.cbAutoUpdate.isChecked = false
            Toast.makeText(this, "Subscription added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save subscription", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSubscription(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.btnImport.isEnabled = false
            binding.btnImport.text = "Importing..."

            val result = withContext(Dispatchers.IO) {
                SubscriptionUpdater(this@SubscriptionActivity).updateSubscription(
                    Subscription(url = url)
                )
            }

            binding.btnImport.isEnabled = true
            binding.btnImport.text = "Import"

            when (result) {
                is SubscriptionUpdater.UpdateResult.Success -> {
                    var importedCount = 0
                    for (config in result.configs) {
                        if (appConfig.saveConfig(config)) {
                            importedCount++
                        }
                    }
                    Toast.makeText(this, "Imported $importedCount servers", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is SubscriptionUpdater.UpdateResult.Failure -> {
                    Toast.makeText(this, "Import failed: ${result.error}", Toast.LENGTH_SHORT).show()
                }
                is SubscriptionUpdater.UpdateResult.Empty -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateSubscription(subscription: Subscription) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.btnImport.isEnabled = false

            val result = withContext(Dispatchers.IO) {
                SubscriptionUpdater(this@SubscriptionActivity).updateSubscription(subscription)
            }

            binding.btnImport.isEnabled = true

            when (result) {
                is SubscriptionUpdater.UpdateResult.Success -> {
                    var importedCount = 0
                    for (config in result.configs) {
                        if (appConfig.saveConfig(config)) {
                            importedCount++
                        }
                    }

                    val updatedSubscription = subscription.copy(lastUpdated = System.currentTimeMillis())
                    appConfig.saveSubscription(updatedSubscription)
                    loadSubscriptions()

                    Toast.makeText(this, "Updated: $importedCount servers", Toast.LENGTH_SHORT).show()
                }
                is SubscriptionUpdater.UpdateResult.Failure -> {
                    Toast.makeText(this, "Update failed: ${result.error}", Toast.LENGTH_SHORT).show()
                }
                is SubscriptionUpdater.UpdateResult.Empty -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteSubscription(subscription: Subscription) {
        if (appConfig.deleteSubscription(subscription.id)) {
            loadSubscriptions()
            Toast.makeText(this, "Subscription deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to delete subscription", Toast.LENGTH_SHORT).show()
        }
    }
}
