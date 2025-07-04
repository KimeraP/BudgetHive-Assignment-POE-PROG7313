// ViewCategoryTotalsActivity.kt

package com.example.budgethive

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class ViewCategoryTotalsActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnEnd:   Button
    private lateinit var btnLoad:  Button
    private lateinit var rv:       RecyclerView
    private lateinit var adapter:  CategoryTotalAdapter

    private var startMillis: Long = 0L
    private var endMillis:   Long = 0L

    // Firebase root reference
    private val dbRef = Firebase.database.reference

    // Always read the UID as a String
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
            ?: throw IllegalStateException("Missing user ID")

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_category_totals)

        btnStart = findViewById(R.id.btnStartDate)
        btnEnd   = findViewById(R.id.btnEndDate)
        btnLoad  = findViewById(R.id.btnLoadTotals)
        rv       = findViewById(R.id.rvCategoryTotals)

        adapter = CategoryTotalAdapter(emptyList())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        // default last 30 days
        endMillis   = System.currentTimeMillis()
        startMillis = endMillis - 30L * 24 * 60 * 60 * 1000
        btnStart.text = dateFmt.format(Date(startMillis))
        btnEnd.text   = dateFmt.format(Date(endMillis))

        btnStart.setOnClickListener { pickDate(isStart = true) }
        btnEnd.setOnClickListener   { pickDate(isStart = false) }
        btnLoad.setOnClickListener  { loadTotals() }
    }

    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (isStart) startMillis else endMillis
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day)
                if (isStart) {
                    startMillis = cal.timeInMillis
                    btnStart.text = dateFmt.format(cal.time)
                } else {
                    // include entire day
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    endMillis = cal.timeInMillis - 1
                    btnEnd.text = dateFmt.format(Date(endMillis))
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadTotals() {
        Log.d("ViewTotals", "Loading totals under UID → '$currentUserId'")
        dbRef.child("expenses")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val all = snapshot.children.mapNotNull {
                        it.getValue(ExpenseEntry::class.java)
                    }

                    Log.d("ViewTotals", "Fetched ${all.size} entries:")
                    all.forEach { e ->
                        Log.d("ViewTotals",
                            " • id=${e.id} date=${Date(e.date)} (${e.date}) cat=${e.categoryId}"
                        )
                    }

                    val filtered = all.filter { it.date in startMillis..endMillis }
                    Log.d("ViewTotals", " → ${filtered.size} entries in range")

                    Toast.makeText(
                        this@ViewCategoryTotalsActivity,
                        "Found ${filtered.size} entries in that period",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (filtered.isEmpty()) {
                        adapter.update(emptyList())
                        return
                    }

                    val sumsByCat: Map<String, Double> = filtered
                        .groupBy { it.categoryId ?: "" }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }

                    loadCategoryNamesAndDisplay(sumsByCat)
                }
                // Show error if database read fails
                override fun onCancelled(err: DatabaseError) {
                    Toast.makeText(
                        this@ViewCategoryTotalsActivity,
                        "Load failed: ${err.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun loadCategoryNamesAndDisplay(sumsByCat: Map<String, Double>) {
        dbRef.child("categories")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Build map of id -> name
                    val nameMap = snapshot.children
                        .mapNotNull { it.getValue(Category::class.java) }
                        .associateBy({ it.id }, { it.name })


                    // Create list of CategoryTotal objects for display
                    val results = sumsByCat.mapNotNull { (catId, total) ->
                        nameMap[catId]?.let { CategoryTotal(it, total) }
                    }
                    // Update adapter and notify empty state
                    adapter.update(results)
                    if (results.isEmpty()) {
                        Toast.makeText(
                            this@ViewCategoryTotalsActivity,
                            "No spending in that period",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                // Show error if category load fails
                override fun onCancelled(err: DatabaseError) {
                    Toast.makeText(
                        this@ViewCategoryTotalsActivity,
                        "Failed loading categories: ${err.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
