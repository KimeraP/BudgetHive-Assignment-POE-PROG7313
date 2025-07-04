package com.example.budgethive

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.util.*

class ViewProgressActivity : AppCompatActivity() {

    private lateinit var llSegments: LinearLayout
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var tvOverLimit: TextView
    private lateinit var ivArrow: ImageView

    private lateinit var tvSummaryMin: TextView
    private lateinit var tvSummaryMax: TextView
    private lateinit var tvSummarySpent: TextView

    // Firebase
    private val dbRef = Firebase.database.reference

    // String ID
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_progress)

        llSegments     = findViewById(R.id.llSegments)
        tvMinGoal      = findViewById(R.id.tvMinGoal)
        tvMaxGoal      = findViewById(R.id.tvMaxGoal)
        tvOverLimit    = findViewById(R.id.tvOverLimit)
        ivArrow        = findViewById(R.id.ivArrow)
        tvSummaryMin   = findViewById(R.id.tvSummaryMin)
        tvSummaryMax   = findViewById(R.id.tvSummaryMax)
        tvSummarySpent = findViewById(R.id.tvSummarySpent)

        // Compute last calendar month range
        val calStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -1)
        }
        val startMillis = calStart.timeInMillis

        val calEnd = calStart.clone() as Calendar
        calEnd.add(Calendar.MONTH, 1)
        val endMillis = calEnd.timeInMillis - 1

        val goalYear  = calStart.get(Calendar.YEAR)
        val goalMonth = calStart.get(Calendar.MONTH) + 1
        val goalKey   = "$goalYear-$goalMonth"

        // 1) Read the goal
        dbRef.child("monthly_goals")
            .child(currentUserId)
            .child(goalKey)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(goalSnap: DataSnapshot) {
                    val goal = goalSnap.getValue(MonthlyGoal::class.java)
                    val minGoal = goal?.minAmount ?: 0.0
                    val maxGoal = goal?.maxAmount ?: 0.0

                    // 2) Read all expenses
                    dbRef.child("expenses")
                        .child(currentUserId)
                        .addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(expSnap: DataSnapshot) {
                                val spent = expSnap.children
                                    .mapNotNull { it.getValue(ExpenseEntry::class.java) }
                                    .filter { it.date in startMillis..endMillis }
                                    .sumOf { it.amount }
                                    .coerceAtLeast(0.0)

                                // 3) Render on main thread
                                renderProgress(minGoal, maxGoal, spent)
                            }
                            override fun onCancelled(err: DatabaseError) {
                                Toast.makeText(this@ViewProgressActivity,
                                    "Failed loading expenses: ${err.message}",
                                    Toast.LENGTH_LONG).show()
                            }
                        })
                }
                override fun onCancelled(err: DatabaseError) {
                    Toast.makeText(this@ViewProgressActivity,
                        "Failed loading goal: ${err.message}",
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun renderProgress(minGoal: Double, maxGoal: Double, spent: Double) {
        // Compute over/under
        val overUsed = (spent - maxGoal - minGoal).coerceAtLeast(0.0)

        tvMinGoal   .text = "Min: R%.2f".format(minGoal)
        tvMaxGoal   .text = "Max: R%.2f".format(maxGoal)
        tvOverLimit .text = "Over: R%.2f".format(overUsed)

        // Equal thirds
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        tvMinGoal   .layoutParams = lp
        tvMaxGoal   .layoutParams = lp
        tvOverLimit .layoutParams = lp
        llSegments.requestLayout()

        // Arrow position
        llSegments.post {
            val frac = if (maxGoal > 0.0) (spent / maxGoal).coerceAtMost(1.0).toFloat() else 0f
            val barWidth = llSegments.width.toFloat()
            ivArrow.x = llSegments.left + frac * barWidth - ivArrow.width / 2f
        }

        tvSummaryMin  .text = "Min goal = R%.2f".format(minGoal)
        tvSummaryMax  .text = "Max goal = R%.2f".format(maxGoal)
        tvSummarySpent.text = "Total spent = R%.2f".format(spent)
    }
}
