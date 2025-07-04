package com.example.budgethive

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class SetSavingsGoalActivity : AppCompatActivity() {

    private lateinit var etGoalName: EditText
    private lateinit var etTargetAmount: EditText
    private lateinit var btnPickDeadline: Button
    private lateinit var btnSaveSavingsGoal: Button
    private lateinit var llGoalsList: LinearLayout
    private lateinit var btnGoalAccomplished: Button

    private var deadlineMillis: Long = 0L
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Firebase root reference
    private val dbRef = Firebase.database.reference

    // Read the UID from the Intent
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
            ?: throw IllegalStateException("Missing user ID")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_savings_goal)

        etGoalName          = findViewById(R.id.etGoalName)
        etTargetAmount      = findViewById(R.id.etTargetAmount)
        btnPickDeadline     = findViewById(R.id.btnPickDeadline)
        btnSaveSavingsGoal  = findViewById(R.id.btnSaveSavingsGoal)
        llGoalsList         = findViewById(R.id.llGoalsList)
        btnGoalAccomplished = findViewById(R.id.btnGoalAccomplished)

        btnPickDeadline.text = "Select date"
        btnPickDeadline.setOnClickListener { pickDeadline() }
        btnSaveSavingsGoal.setOnClickListener { saveNewGoal() }
        btnGoalAccomplished.setOnClickListener { processCompletedGoals() }

        loadGoals()
    }

    private fun pickDeadline() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(y, m, d, 0, 0)
                deadlineMillis = cal.timeInMillis
                btnPickDeadline.text = dateFmt.format(cal.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveNewGoal() {
        val name   = etGoalName.text.toString().trim()
        val amount = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a goal name", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount <= 0.0) {
            Toast.makeText(this, "Enter a valid target amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (deadlineMillis == 0L) {
            Toast.makeText(this, "Pick a deadline date", Toast.LENGTH_SHORT).show()
            return
        }

        val newId = dbRef.child("savings_goals")
            .child(currentUserId)
            .push()
            .key ?: UUID.randomUUID().toString()

        // Use `completed` field (not isCompleted)
        val goal = SavingsGoal(
            id            = newId,
            userId        = currentUserId,
            name          = name,
            targetAmount  = amount,
            deadline      = deadlineMillis,
            completed     = false     // match DB key
        )

        dbRef.child("savings_goals")
            .child(currentUserId)
            .child(newId)
            .setValue(goal)
            .addOnSuccessListener {
                Toast.makeText(this, "Savings goal saved!", Toast.LENGTH_SHORT).show()
                etGoalName.text.clear()
                etTargetAmount.text.clear()
                deadlineMillis = 0L
                btnPickDeadline.text = "Select date"
                loadGoals()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadGoals() {
        llGoalsList.removeAllViews()
        dbRef.child("savings_goals")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children
                        .mapNotNull { it.getValue(SavingsGoal::class.java) }
                        .filter { !it.completed }       // use `completed`
                        .forEach { goal -> addGoalRow(goal) }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SetSavingsGoalActivity,
                        "Failed loading goals: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun addGoalRow(goal: SavingsGoal) {
        val cb = CheckBox(this).apply {
            text = "${goal.name} — R${"%.2f".format(goal.targetAmount)} — ${dateFmt.format(Date(goal.deadline))}"
            isChecked = false
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 8, 0, 8) }
            layoutParams = lp
            tag = goal
        }
        llGoalsList.addView(cb)
    }

    private fun processCompletedGoals() {
        val toComplete = (0 until llGoalsList.childCount)
            .mapNotNull { llGoalsList.getChildAt(it) as? CheckBox }
            .filter { it.isChecked }
            .mapNotNull { it.tag as? SavingsGoal }

        if (toComplete.isEmpty()) {
            Toast.makeText(this, "No goals selected", Toast.LENGTH_SHORT).show()
            return
        }

        // batch the writes against the same `completed` key
        val tasks = toComplete.map { goal ->
            dbRef.child("savings_goals")
                .child(currentUserId)
                .child(goal.id)
                .child("completed")           // ← use `completed`
                .setValue(true)
        }

        Tasks.whenAll(tasks)
            .addOnSuccessListener {
                Toast.makeText(this, "Marked ${toComplete.size} goals completed", Toast.LENGTH_SHORT).show()
                loadGoals()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
