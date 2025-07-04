// app/src/main/java/com/example/budgethive/CategoryActivity.kt
package com.example.budgethive

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * Activity for creating and listing user-defined categories.
 */

class CategoryActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var btnAdd: Button
    private lateinit var lvCats: ListView
    // Adapter backing the ListView
    private lateinit var adapter: ArrayAdapter<String>
    // Root reference to Firebase Realtime Database
    private val dbRef = Firebase.database.reference

    //keeping userId as String for Firebase keys:
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
             ?: throw IllegalStateException("Missing user ID in Intent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // 1) Bind views
        etName = findViewById(R.id.etNewCategoryName)
        btnAdd = findViewById(R.id.btnAddCategory)
        lvCats = findViewById(R.id.lvCategories)

        // 2) Initialize adapter with empty data
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf<String>()
        )
        lvCats.adapter = adapter

        // 3) Load and display existing categories

        loadCategories()

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // push new Category under /categories/{userId}/{newId}
            val newId = dbRef.child("categories")
                .child(currentUserId)
                .push()
                .key!!
            val cat = Category(
                id     = newId,
                userId = currentUserId,
                name   = name
            )
            dbRef.child("categories")
                .child(currentUserId)
                .child(newId)
                .setValue(cat)
                .addOnSuccessListener {
                    etName.text.clear()
                    loadCategories()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        "Error saving category: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadCategories() {
        dbRef.child("categories")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val names = snapshot.children.mapNotNull { child ->
                        child.getValue(Category::class.java)?.name
                    }
                    adapter.clear()
                    adapter.addAll(names)
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CategoryActivity,
                        "Load failed: ${error.message}",
                        Toast.LENGTH_LONG).show()
                }
            })
    }
}
