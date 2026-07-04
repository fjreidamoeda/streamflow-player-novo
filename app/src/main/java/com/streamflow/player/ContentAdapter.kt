package com.streamflow.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ContentAdapter(
    private var items: List<Any>,
    private val onClick: (Any) -> Unit
) : RecyclerView.Adapter<ContentAdapter.ViewHolder>() {

    private var filtered = items

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvItemName)
        val ivIcon: ImageView = view.findViewById(R.id.ivItemIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_content, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filtered[position]
        val name = when (item) {
            is LiveStream -> item.name
            is VodStream -> item.name
            is SeriesItem -> item.name
            else -> ""
        }
        val icon = when (item) {
            is LiveStream -> item.streamIcon
            is VodStream -> item.streamIcon
            is SeriesItem -> item.cover
            else -> ""
        }
        holder.tvName.text = name
        if (icon.isNotBlank()) {
            Picasso.get().load(icon).placeholder(0).into(holder.ivIcon)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = filtered.size

    fun filter(query: String) {
        filtered = if (query.isEmpty()) items
        else items.filter {
            val name = when (it) {
                is LiveStream -> it.name
                is VodStream -> it.name
                is SeriesItem -> it.name
                else -> ""
            }
            name.lowercase().contains(query)
        }
        notifyDataSetChanged()
    }
}
