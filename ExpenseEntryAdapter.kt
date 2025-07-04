package com.example.budgethive

import android.app.AlertDialog
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


/**
 * ViewHolder holds references to the views in each row
 */

class ExpenseEntryAdapter(
    private var items: List<ExpenseEntry>
) : RecyclerView.Adapter<ExpenseEntryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivThumb) // Thumbnail of receipt photo
        val tvDesc: TextView   = view.findViewById(R.id.tvDesc)
        val tvDate: TextView   = view.findViewById(R.id.tvDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    /**
     * Inflates the row layout and creates a ViewHolder
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_entry, parent, false)
        return ViewHolder(v)
    }

    /**
     * Bind an ExpenseEntry to the row at [position]
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = items[position]
        holder.tvDesc.text = entry.description
        holder.tvDate.text = android.text.format.DateFormat
            .format("yyyy-MM-dd HH:mm", entry.date).toString()
        holder.tvAmount.text = "R %.2f".format(entry.amount)

// Show thumbnail if a photo URI exists, otherwise hides the ImageView...
        if (!entry.photoUri.isNullOrEmpty()) {
            holder.ivThumb.visibility = View.VISIBLE
            holder.ivThumb.setImageURI(Uri.parse(entry.photoUri))
            holder.ivThumb.setOnClickListener {
                val imageView = ImageView(it.context).apply {
                    setImageURI(Uri.parse(entry.photoUri))
                    adjustViewBounds = true
                }


                AlertDialog.Builder(it.context)
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show()
            }
        } else {
            // On click, show full-screen AlertDialog with the image
            holder.ivThumb.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ExpenseEntry>) {
        items = newItems
        notifyDataSetChanged()
    }
}
