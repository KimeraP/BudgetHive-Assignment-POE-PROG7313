package com.example.budgethive

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class SetMonthlyGoalActivity : AppCompatActivity() {

    private lateinit var spinnerMonth: Spinner
    private lateinit var etMin: EditText
    private lateinit var etMax: EditText
    private lateinit var btnSave: Button

    // Firebase reference
    private val dbRef = Firebase.database.reference

    // Use String user IDs
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID) ?: ""

    private val months = listOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_monthly_goal)

        spinnerMonth = findViewById(R.id.spinnerMonth)
        etMin        = findViewById(R.id.etMinGoal)
        etMax        = findViewById(R.id.etMaxGoal)
        btnSave      = findViewById(R.id.btnSaveGoals)

        // Populate spinner
        ArrayAdapter(this, android.R.layout.simple_spinner_item, months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMonth.adapter = this
        }

        // Pre-load existing goal for this month/year
        val cal      = Calendar.getInstance()
        val thisYear = cal.get(Calendar.YEAR)
        val thisMonth = cal.get(Calendar.MONTH) + 1

        // Path: /monthly_goals/{userId}/{year-month}
        val goalKey = "$thisYear-$thisMonth"
        dbRef.child("monthly_goals")
            .child(currentUserId)
            .child(goalKey)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.getValue(MonthlyGoal::class.java)?.let { existing ->
                        // select spinner + fill fields
                        spinnerMonth.setSelection(existing.month - 1)
                        etMin.setText(existing.minAmount?.toString() ?: "")
                        etMax.setText(existing.maxAmount?.toString() ?: "")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SetMonthlyGoalActivity,
                        "Failed to load goal: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        // Save handler
        btnSave.setOnClickListener {
            val minVal = etMin.text.toString().toDoubleOrNull()
            val maxVal = etMax.text.toString().toDoubleOrNull()
            if (minVal == null && maxVal == null) {
                Toast.makeText(this, "Enter at least one goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selMonth = spinnerMonth.selectedItemPosition + 1
            val year     = thisYear
            val key      = "$year-$selMonth"

            val goal = MonthlyGoal(
                id        = key,               // use key as ID...
                userId    = currentUserId,
                year      = year,
                month     = selMonth,
                minAmount = minVal,
                maxAmount = maxVal
            )

            // Write under /monthly_goals/{userId}/{year-month}
            dbRef.child("monthly_goals")
                .child(currentUserId)
                .child(key)
                .setValue(goal)
                .addOnSuccessListener {
                    Toast.makeText(
                        this@SetMonthlyGoalActivity,
                        "Monthly goal saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@SetMonthlyGoalActivity,
                        "Error saving goal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
