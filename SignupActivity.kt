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
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private val auth   = Firebase.auth
    private val dbRef  = Firebase.database.reference
    private val gifDelayMillis = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etName           = findViewById<EditText>(R.id.etName)
        val etSurname        = findViewById<EditText>(R.id.etSurname)
        val etCellNumber     = findViewById<EditText>(R.id.etCellNumber)
        val etEmail          = findViewById<EditText>(R.id.etEmail)
        val etCurrentBalance = findViewById<EditText>(R.id.etCurrentBalance)
        val etPassword       = findViewById<EditText>(R.id.etPassword)
        val etConfirmPass    = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignup        = findViewById<Button>(R.id.btnSignup)
        val ivGif            = findViewById<ImageView>(R.id.ivSignupGif)

        btnSignup.setOnClickListener {
            val name    = etName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val cell    = etCellNumber.text.toString().trim()
            val email   = etEmail.text.toString().trim()
            val balance = etCurrentBalance.text.toString().toDoubleOrNull() ?: 0.0
            val pass    = etPassword.text.toString()
            val confirm = etConfirmPass.text.toString()

            if (name.isEmpty() || surname.isEmpty() || cell.isEmpty()
                || email.isEmpty() || pass.isEmpty() || pass != confirm
            ) {
                Toast.makeText(this,
                    "Please fill all fields correctly",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // Build profile (password omitted)
                    val userProfile = User(
                        id             = uid,
                        name           = name,
                        surname        = surname,
                        cellNumber     = cell,
                        email          = email,
                        currentBalance = balance
                    )

                    // Write profile to Realtime Database
                    dbRef.child("users")
                        .child(uid)
                        .setValue(userProfile)
                        .addOnSuccessListener {
                            // Show success GIF
                            Glide.with(this)
                                .asGif()
                                .load(R.drawable.gif17)
                                .transition(DrawableTransitionOptions.withCrossFade(300))
                                .into(ivGif)
                            ivGif.visibility = View.VISIBLE

                            // After a short delay, go to Dashboard
                            Handler(Looper.getMainLooper()).postDelayed({
                                Intent(this, DashboardActivity::class.java).also { intent ->
                                    intent.putExtra(LoginActivity.EXTRA_USER_ID, uid)
                                    intent.putExtra(LoginActivity.EXTRA_USER_NAME, "$name $surname")
                                    startActivity(intent)
                                    finishAffinity()
                                }
                            }, gifDelayMillis)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this,
                                "Error saving profile: ${e.message}",
                                Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        "Signup failed: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
        }
    }
}
