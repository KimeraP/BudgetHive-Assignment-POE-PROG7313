package com.example.budgethive

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class MyStatisticsActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnEnd:   Button
    private lateinit var btnEnter: Button
    private lateinit var chart:    BarChart

    private var startMillis: Long = 0L
    private var endMillis:   Long = 0L

    // Firebase
    private val dbRef = Firebase.database.reference

    // use String IDs now
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID) ?: ""

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_statistics)

        btnStart = findViewById(R.id.btnStatsStartDate)
        btnEnd   = findViewById(R.id.btnStatsEndDate)
        btnEnter = findViewById(R.id.btnStatsEnter)
        chart    = findViewById(R.id.chartCategoryTotals)

        // default last 30 days
        endMillis   = System.currentTimeMillis()
        startMillis = endMillis - 30L * 24 * 60 * 60 * 1000
        btnStart.text = dateFmt.format(Date(startMillis))
        btnEnd  .text = dateFmt.format(Date(endMillis))

        btnStart.setOnClickListener { pickDate(true) }
        btnEnd  .setOnClickListener { pickDate(false) }
        btnEnter.setOnClickListener { loadAndDrawChart() }

        chart.apply {
            description.isEnabled = false
            axisRight.isEnabled   = false
            legend.isEnabled      = false
            xAxis.position        = XAxis.XAxisPosition.BOTTOM
            setExtraTopOffset(20f)
            axisLeft.setDrawLimitLinesBehindData(true)
        }
    }

    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (isStart) startMillis else endMillis
        }
        DatePickerDialog(this,
            { _, y, m, d ->
                cal.set(y, m, d)
                if (isStart) {
                    startMillis = cal.timeInMillis
                    btnStart.text = dateFmt.format(cal.time)
                } else {
                    endMillis = cal.timeInMillis
                    btnEnd.text = dateFmt.format(cal.time)
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadAndDrawChart() {
        // 1) Read all expenses for this user
        dbRef.child("expenses")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(expSnap: DataSnapshot) {
                    val entries = expSnap.children.mapNotNull { it.getValue(ExpenseEntry::class.java) }
                        .filter { it.date in startMillis..endMillis }

                    if (entries.isEmpty()) {
                        Toast.makeText(this@MyStatisticsActivity,
                            "No spending in that period", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // sum per categoryId
                    val totalsByCatId = entries.groupBy { it.categoryId ?: "" }
                        .mapValues { it.value.sumOf { e -> e.amount }.toFloat() }

                    // get category names map
                    dbRef.child("categories")
                        .child(currentUserId)
                        .addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(catSnap: DataSnapshot) {
                                val catMap = catSnap.children.mapNotNull { it.getValue(Category::class.java) }
                                    .associateBy { it.id }

                                // prepare BarEntries & labels
                                val dataEntries = mutableListOf<BarEntry>()
                                val labels = mutableListOf<String>()
                                totalsByCatId.entries.forEachIndexed { idx, (catId, tot) ->
                                    val name = catMap[catId]?.name ?: "(unknown)"
                                    labels += name
                                    dataEntries += BarEntry(idx.toFloat(), tot)
                                }

                                // Now fetch goals in date range
                                fetchAndDrawGoals(dataEntries, labels)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@MyStatisticsActivity,
                                    "Failed loading categories: ${error.message}",
                                    Toast.LENGTH_LONG).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MyStatisticsActivity,
                        "Failed loading expenses: ${error.message}",
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun fetchAndDrawGoals(entries: List<BarEntry>, labels: List<String>) {
        // 2) Figure out all year-month pairs in range
        val months = mutableListOf<Pair<Int,Int>>()
        Calendar.getInstance().apply {
            timeInMillis = startMillis
            while (timeInMillis <= endMillis) {
                months += get(Calendar.YEAR) to (get(Calendar.MONTH)+1)
                add(Calendar.MONTH, 1)
            }
        }
        // 3) Read all goals once
        dbRef.child("monthly_goals")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(goalSnap: DataSnapshot) {
                    val allGoals = goalSnap.children.mapNotNull { it.getValue(MonthlyGoal::class.java) }
                    // match goals by year/month
                    val selectedGoals = months.mapNotNull { ym ->
                        allGoals.find { it.year==ym.first && it.month==ym.second }
                    }
                    // compute avg lines
                    val avgMin = selectedGoals.mapNotNull { it.minAmount }.average().toFloat()
                    val avgMax = selectedGoals.mapNotNull { it.maxAmount }.average().toFloat()
                    drawChart(entries, labels, avgMin, avgMax)
                }
                override fun onCancelled(err: DatabaseError) {
                    Toast.makeText(this@MyStatisticsActivity,
                        "Failed loading goals: ${err.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun drawChart(
        entries: List<BarEntry>,
        labels: List<String>,
        avgMin: Float,
        avgMax: Float
    ) {
        // run on UI thread
        runOnUiThread {
            // merge duplicates by label (unlikely) and prepare DataSet
            val dataSet = BarDataSet(entries, "Spent").apply {
                setDrawValues(true)
                color = Color.CYAN
            }
            val barWidth = if (entries.size == 1) 0.3f else 0.9f
            chart.data = BarData(dataSet).apply { this.barWidth = barWidth }

            // limit lines
            chart.axisLeft.removeAllLimitLines()
            if (avgMin > 0f) chart.axisLeft.addLimitLine(
                LimitLine(avgMin, "Min: R${"%.2f".format(avgMin)}").apply {
                    lineColor = Color.GREEN; textColor = Color.GREEN; textSize = 12f
                }
            )
            if (avgMax > 0f) chart.axisLeft.addLimitLine(
                LimitLine(avgMax, "Max: R${"%.2f".format(avgMax)}").apply {
                    lineColor = Color.RED; textColor = Color.RED; textSize = 12f
                }
            )

            // expand axis
            val maxBar = entries.maxOfOrNull { it.y } ?: 0f
            val overallMax = maxOf(maxBar, avgMin, avgMax)
            chart.axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = overallMax * 1.1f
            }

            // X axis
            chart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                setDrawGridLines(false)
                labelRotationAngle = -45f
                granularity = 1f
            }

            chart.invalidate()
        }
    }
}
