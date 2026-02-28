package com.listinglab.rowplus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AppPickerAdapter(
    private val apps: List<DockApp>,
    private val pinnedPackages: Set<String>,
    private val onAppClick: (DockApp) -> Unit,
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.pickerAppIcon)
        val label: TextView = view.findViewById(R.id.pickerAppLabel)
        val status: TextView = view.findViewById(R.id.pickerAppStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label

        if (app.packageName in pinnedPackages) {
            holder.status.text = "PINNED"
            holder.status.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.rowplus_teal),
            )
        } else {
            holder.status.text = "ADD"
            holder.status.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.rowplus_text_muted),
            )
        }

        holder.itemView.setOnClickListener { onAppClick(app) }
    }

    override fun getItemCount() = apps.size
}
