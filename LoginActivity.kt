package com.example.budgethive

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_USER_ID   = "EXTRA_USER_ID"
        const val EXTRA_USER_NAME = "EXTRA_USER_NAME"
        private const val GIF_DELAY = 3000L
    }

    private val auth   = Firebase.auth
    private val dbRef  = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail   = findViewById<EditText>(R.id.etUsername)
        val etPass    = findViewById<EditText>(R.id.etPassword)
        val btnLogin  = findViewById<Button>(R.id.btnLogin)
        val ivSuccess = findViewById<ImageView>(R.id.ivSuccessGif)
        val tvSuccess = findViewById<TextView>(R.id.tvSuccessMsg)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass  = etPass.text.toString()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter both email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase Auth sign in
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // Show GIF & text
                    Glide.with(this)
                        .asGif()
                        .load(R.drawable.gif1)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .into(ivSuccess)
                    ivSuccess.visibility = View.VISIBLE
                    tvSuccess.visibility = View.VISIBLE

                    // Fetch profile to get name & surname
                    dbRef.child("users")
                        .child(uid)
                        .addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val userProfile = snapshot.getValue(User::class.java)
                                val fullName = if (userProfile != null) {
                                    "${userProfile.name} ${userProfile.surname}"
                                } else {
                                    "User"
                                }
                                // Delay then go to the Dashboard...
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startActivity(
                                        Intent(this@LoginActivity, DashboardActivity::class.java)
                                            .putExtra(EXTRA_USER_ID,   uid)
                                            .putExtra(EXTRA_USER_NAME, fullName)
                                    )
                                    finishAffinity()
                                }, GIF_DELAY)
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Failed to load profile: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Authentication failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
