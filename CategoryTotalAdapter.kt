package com.example.budgethive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryTotalAdapter(
    private var items: List<CategoryTotal>
) : RecyclerView.Adapter<CategoryTotalAdapter.VH>() {

    inner class VH(view: View): RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tvCategoryName)
        val total = view.findViewById<TextView>(R.id.tvTotalSpent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_total, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val ct = items[pos]
        holder.name.text  = ct.categoryName
        holder.total.text = "R %.2f".format(ct.totalSpent)
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<CategoryTotal>) {
        items = newItems
        notifyDataSetChanged()
    }
}
