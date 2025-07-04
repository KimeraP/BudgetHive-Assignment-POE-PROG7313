// ViewEntriesActivity.kt

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

class ViewEntriesActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnEnd:   Button
    private lateinit var btnLoad:  Button
    // RecyclerView to display the list of ExpenseEntry items
    private lateinit var rv:       RecyclerView
    private lateinit var adapter:  ExpenseEntryAdapter

    // Millisecond timestamps for the currently selected date range
    private var startMillis: Long = 0L
    private var endMillis:   Long = 0L

    private val dbRef = Firebase.database.reference

    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
            ?: throw IllegalStateException("Missing user ID")

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_entries)
// Bind UI views
        btnStart = findViewById(R.id.btnStartDate)
        btnEnd   = findViewById(R.id.btnEndDate)
        btnLoad  = findViewById(R.id.btnLoad)
        rv       = findViewById(R.id.rvEntries)

        // Initialize adapter with empty list and set up RecyclerView
        adapter = ExpenseEntryAdapter(emptyList())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter


        endMillis   = System.currentTimeMillis()
        startMillis = endMillis - 30L * 24 * 60 * 60 * 1000
        btnStart.text = dateFmt.format(Date(startMillis))
        btnEnd.text   = dateFmt.format(Date(endMillis))

        btnStart.setOnClickListener { pickDate(true) }
        btnEnd.setOnClickListener   { pickDate(false) }
        btnLoad.setOnClickListener  { loadEntries() }
    }
    // Initialize calendar with current selection
    private fun pickDate(isStart: Boolean) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (isStart) startMillis else endMillis
        }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
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

    private fun loadEntries() {
        Log.d("ViewEntries", "Loading entries under UID → '$currentUserId'")
        dbRef.child("expenses")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ViewEntries", "Snapshot keys: ${snapshot.children.map { it.key }}")
                    val entries = snapshot.children
                        .mapNotNull { it.getValue(ExpenseEntry::class.java) }
                        .filter { it.date in startMillis..endMillis }

                    if (entries.isEmpty()) {
                        Toast.makeText(
                            this@ViewEntriesActivity,
                            "No entries in that period",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    adapter.update(entries)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ViewEntriesActivity,
                        "Failed to load entries: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
