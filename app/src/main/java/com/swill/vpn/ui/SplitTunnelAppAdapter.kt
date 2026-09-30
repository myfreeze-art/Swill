package com.swill.vpn.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swill.vpn.R
import com.swill.vpn.model.SplitTunnelApp

class SplitTunnelAppAdapter(
    private var apps: List<SplitTunnelApp> = emptyList(),
    private val onAppToggle: (SplitTunnelApp, Boolean) -> Unit,
    private val onRouteToggle: (SplitTunnelApp, Boolean) -> Unit
) : RecyclerView.Adapter<SplitTunnelAppAdapter.AppViewHolder>() {

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = itemView.findViewById(R.id.tvPackageName)
        val switchEnabled: Switch = itemView.findViewById(R.id.switchEnabled)
        val switchRoute: Switch = itemView.findViewById(R.id.switchRoute)

        init {
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAppToggle(apps[position], isChecked)
                }
            }

            switchRoute.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRouteToggle(apps[position], isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_split_tunnel_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.tvAppName.text = app.appName
        holder.tvPackageName.text = app.packageName
        holder.switchEnabled.isChecked = app.enabled
        holder.switchRoute.isChecked = app.routeThroughVpn
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<SplitTunnelApp>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
