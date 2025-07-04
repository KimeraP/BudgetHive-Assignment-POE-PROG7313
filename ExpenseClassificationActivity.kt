package com.example.budgethive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class ExpenseClassificationActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var rvBreakdown: RecyclerView
    private lateinit var insightsCard: MaterialCardView
    private lateinit var tvInsights: TextView

    private val dbRef: DatabaseReference = FirebaseDatabase.getInstance().reference

    private val currentUserId: String
        get() = intent.getStringExtra("EXTRA_USER_ID")
            ?: throw IllegalStateException("Missing user ID")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_classification)

        pieChart     = findViewById(R.id.pieChartClassification)
        rvBreakdown  = findViewById(R.id.rvCategoryBreakdown)
        insightsCard = findViewById(R.id.insights_card)
        tvInsights   = findViewById(R.id.tvInsightsBody)

        // RecyclerView setup
        rvBreakdown.layoutManager = LinearLayoutManager(this)
        rvBreakdown.adapter = CategoryBreakdownAdapter(emptyList())

        // PieChart setup
        pieChart.apply {
            setUsePercentValues(true)
            description = Description().apply { text = "" }
            isDrawHoleEnabled = true
            setEntryLabelTextSize(12f)
            legend.isEnabled = false
        }

        // Classifies everything in one pass...
        classifyAllExpenses()
    }

    private fun classifyAllExpenses() {
        dbRef.child("expenses")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(expSnap: DataSnapshot) {
                    val entries = expSnap.children.mapNotNull {
                        it.getValue(ExpenseEntry::class.java)
                    }
                    if (entries.isEmpty()) {
                        showEmptyState()
                        return
                    }

                    // Sum by categoryId
                    val sumsByCat: Map<String, Double> = entries
                        .groupBy { it.categoryId ?: "(uncategorized)" }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    // Now loading category names...
                    dbRef.child("categories")
                        .child(currentUserId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(catSnap: DataSnapshot) {
                                val nameMap: Map<String, String> = catSnap.children
                                    .mapNotNull { it.getValue(Category::class.java) }
                                    .associateBy({ it.id }, { it.name })

                                val breakdown: List<CategoryBreakdown> = sumsByCat.map { (catId, total) ->
                                    CategoryBreakdown(nameMap[catId] ?: "(unknown)", total)
                                }

                                renderPieChart(breakdown)
                                renderList(breakdown)
                                renderInsights(breakdown)
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@ExpenseClassificationActivity,
                                    "Error loading categories: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ExpenseClassificationActivity,
                        "Error loading expenses: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun showEmptyState() {
        pieChart.clear()
        (rvBreakdown.adapter as CategoryBreakdownAdapter).update(emptyList())
        tvInsights.text = "No expenses to classify."
    }

    private fun renderPieChart(data: List<CategoryBreakdown>) {
        if (data.isEmpty()) {
            pieChart.clear()
            return
        }

        // Total as Float
        val totalAmount = data.sumOf { it.amount }.toFloat()

        // Build PieEntry list, to ensure Float values
        val pieEntries = data.map { item ->
            val percent = (item.amount.toFloat() / totalAmount) * 100f
            PieEntry(percent, item.name)
        }

        // Creates and styles the dataset
        val dataSet = PieDataSet(pieEntries, "").apply {
            sliceSpace = 2f
            valueTextSize = 12f
        }

        // Feed into the chart
        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun renderList(data: List<CategoryBreakdown>) {
        (rvBreakdown.adapter as CategoryBreakdownAdapter).update(data)
    }

    private fun renderInsights(data: List<CategoryBreakdown>) {
        val total = data.sumOf { it.amount }
        val top = data.maxByOrNull { it.amount } ?: return
        val pct = top.amount / total * 100
        tvInsights.text = "You spend ${"%.1f".format(pct)}% on ${top.name}. " +
                "Try cutting by 10% (≈R${"%.2f".format(top.amount * 0.1)})."
    }
}

// RecyclerView model + adapter

data class CategoryBreakdown(val name: String, val amount: Double)

class CategoryBreakdownAdapter(
    private var items: List<CategoryBreakdown>
) : RecyclerView.Adapter<CategoryBreakdownAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView   = view.findViewById(R.id.tvName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_classification_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvAmount.text = "R %.2f".format(item.amount)
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<CategoryBreakdown>) {
        items = newItems
        notifyDataSetChanged()
    }
}
