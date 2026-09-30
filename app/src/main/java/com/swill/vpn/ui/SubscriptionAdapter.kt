package com.swill.vpn.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swill.vpn.R
import com.swill.vpn.model.Subscription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionAdapter(
    private var subscriptions: List<Subscription> = emptyList(),
    private val onSubscriptionUpdate: (Subscription) -> Unit,
    private val onSubscriptionDelete: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

    inner class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvSubscriptionName)
        val tvUrl: TextView = itemView.findViewById(R.id.tvSubscriptionUrl)
        val tvLastUpdated: TextView = itemView.findViewById(R.id.tvSubscriptionLastUpdated)

        init {
            itemView.findViewById<View>(R.id.btnUpdate).setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSubscriptionUpdate(subscriptions[position])
                }
            }

            itemView.findViewById<View>(R.id.btnDelete).setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSubscriptionDelete(subscriptions[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val subscription = subscriptions[position]

        holder.tvName.text = subscription.name
        holder.tvUrl.text = subscription.url

        val lastUpdated = if (subscription.lastUpdated > 0) {
            val date = Date(subscription.lastUpdated)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            format.format(date)
        } else {
            "Never"
        }

        holder.tvLastUpdated.text = "Updated: $lastUpdated"
    }

    override fun getItemCount(): Int = subscriptions.size

    fun updateSubscriptions(newSubscriptions: List<Subscription>) {
        subscriptions = newSubscriptions
        notifyDataSetChanged()
    }
}
