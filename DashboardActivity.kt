package com.example.budgethive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class DashboardActivity : AppCompatActivity() {

    // changed this from Long → String (using firebase now...)
    private lateinit var userId: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Load & animate the GIF
        val ivGif = findViewById<ImageView>(R.id.ivDashboardGif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.gif11)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .into(ivGif)

        // Read user data from Intent
        // READs THE STRING ID...
        userId   = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
            ?: throw IllegalStateException("Missing userId")
        userName = intent.getStringExtra(LoginActivity.EXTRA_USER_NAME) ?: "User"

        // Show personalized greeting
        findViewById<TextView>(R.id.tvGreeting).text = "Welcome, $userName!"

        // Button: Create Expense
        findViewById<Button>(R.id.btnCreateExpense).setOnClickListener {
            Intent(this, ExpenseEntryActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }

        // Button: Set Monthly Goal
        findViewById<Button>(R.id.btnSetMonthlyGoal).setOnClickListener {
            Intent(this, SetMonthlyGoalActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }

        // Button: View Expense Entries
        findViewById<Button>(R.id.btnViewEntries).setOnClickListener {
            Intent(this, ViewEntriesActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }

        // Button: View Totals by Category
        findViewById<Button>(R.id.btnViewTotal).setOnClickListener {
            Intent(this, ViewCategoryTotalsActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }

        // Button: Create Category
        findViewById<Button>(R.id.btnCreateCategory).setOnClickListener {
            Intent(this, CategoryActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
            }
        findViewById<Button>(R.id.btnViewStats).setOnClickListener {
            Intent(this, MyStatisticsActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }
        // inside onCreate, after btnViewStats…
        findViewById<Button>(R.id.btnViewProgress).setOnClickListener {
            Intent(this, ViewProgressActivity::class.java).also { intent ->
                // pass along the userId if you need it
                intent.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(intent)
            }
        }
        // Button: Set Savings Goal (new feature 1)
        findViewById<Button>(R.id.btnSetSavingsGoal).setOnClickListener {
            Intent(this, SetSavingsGoalActivity::class.java).also {
                it.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(it)
            }
        }
        // Button: real time expenditure (new feature 2)
        findViewById<Button>(R.id.btnRealtimeexp).setOnClickListener {
            Intent(this, ExpenseClassificationActivity::class.java).also { intent ->
                intent.putExtra(LoginActivity.EXTRA_USER_ID, userId)
                startActivity(intent)
            }
        }

    }
}