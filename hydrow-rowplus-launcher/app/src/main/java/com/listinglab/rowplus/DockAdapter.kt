package com.listinglab.rowplus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DockAdapter(
    private val onAppClick: (DockApp) -> Unit,
) : RecyclerView.Adapter<DockAdapter.ViewHolder>() {

    var apps: List<DockApp> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.dockItemIcon)
        val label: TextView = view.findViewById(R.id.dockItemLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dock_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        if (app.isAddTile) {
            holder.icon.setImageResource(R.drawable.ic_add_app)
            holder.icon.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.rowplus_text_muted),
            )
            holder.label.text = app.label
            holder.itemView.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.bg_add_app_tile,
            )
        } else {
            holder.icon.setImageDrawable(app.icon)
            holder.icon.clearColorFilter()
            holder.label.text = app.label
            holder.itemView.background = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.bg_dock_item,
            )
        }
        holder.itemView.setOnClickListener { onAppClick(app) }
    }

    override fun getItemCount() = apps.size
}
