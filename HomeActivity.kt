package com.example.budgethive

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

// HomeActivity.kt
class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1) Find the background ImageView
        val bg = findViewById<ImageView>(R.id.ivGifBackground)

        // 2) Using Glide to load GIF resource
        Glide.with(this)
            .asGif()
            .load(R.drawable.gif3)
            .into(bg)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            // don’t call finish() here because want Home to remain under the Login
        }

        findViewById<Button>(R.id.btnSignup).setOnClickListener {
            startActivity(
                Intent(this, SignupActivity::class.java)
            )
        }
    }
}
