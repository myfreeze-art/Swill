package com.swill.vpn.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.swill.vpn.R
import com.swill.vpn.core.ServerPinger
import com.swill.vpn.model.VpnConfig

class ServerAdapter(
    private var servers: List<VpnConfig> = emptyList(),
    private val onServerSelected: (VpnConfig) -> Unit,
    private val onServerEdit: (VpnConfig) -> Unit,
    private val onServerDelete: (VpnConfig) -> Unit,
    private val onServerPing: (VpnConfig) -> Unit
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private var selectedServer: VpnConfig? = null
    private var selectedPosition: Int = -1

    inner class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvServerName)
        val tvAddress: TextView = itemView.findViewById(R.id.tvServerAddress)
        val tvProtocol: TextView = itemView.findViewById(R.id.tvProtocol)
        val tvCore: TextView = itemView.findViewById(R.id.tvCore)
        val viewSelected: View = itemView.findViewById(R.id.viewSelected)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val server = servers[position]
                    selectedServer = server
                    selectedPosition = position
                    notifyDataSetChanged()
                    onServerSelected(server)
                }
            }

            itemView.findViewById<View>(R.id.btnEdit).setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onServerEdit(servers[position])
                }
            }

            itemView.findViewById<View>(R.id.btnDelete).setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onServerDelete(servers[position])
                }
            }

            itemView.findViewById<View>(R.id.btnPing).setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onServerPing(servers[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = servers[position]

        holder.tvName.text = server.name
        holder.tvAddress.text = "${server.serverAddress}:${server.serverPort}"
        holder.tvProtocol.text = server.protocol.uppercase()
        holder.tvCore.text = server.coreType.uppercase()

        val tvPing = holder.itemView.findViewById<TextView>(R.id.tvPing)
        tvPing?.text = server.pingResult?.let { "${it.latency}ms" } ?: "?"

        val isSelected = position == selectedPosition
        holder.viewSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.itemView.setBackgroundResource(
            if (isSelected) R.drawable.bg_server_selected else R.drawable.bg_server_item
        )
    }

    override fun getItemCount(): Int = servers.size

    fun updateServers(newServers: List<VpnConfig>) {
        servers = newServers
        notifyDataSetChanged()
    }

    fun selectServer(server: VpnConfig) {
        val position = servers.indexOfFirst { it.id == server.id }
        if (position >= 0) {
            selectedServer = server
            selectedPosition = position
            notifyDataSetChanged()
        }
    }

    fun getSelectedServer(): VpnConfig? = selectedServer

    fun updatePingResult(serverId: String, result: ServerPinger.PingResult) {
        val updatedServers = servers.map { server ->
            if (server.id == serverId) {
                server.copy(pingResult = result)
            } else {
                server
            }
        }
        servers = updatedServers
        notifyDataSetChanged()
    }
}
