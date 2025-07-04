package com.example.budgethive

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExpenseEntryActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_GALLERY = 1001
        private const val REQUEST_CAMERA  = 1002
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
    }

    // UI
    private lateinit var etDescription: EditText
    private lateinit var etAmount: EditText
    private lateinit var btnPickDate: Button
    private lateinit var btnPickStart: Button
    private lateinit var btnPickEnd: Button
    private lateinit var spinnerCat: Spinner
    private lateinit var btnTakePhoto: Button
    private lateinit var btnAttach: Button
    private lateinit var ivPreview: ImageView
    private lateinit var btnSave: Button

    // Firebase
    private val dbRef = Firebase.database.reference

    // State
    private var pickedDate: Long = System.currentTimeMillis()
    private var startTime: Long   = System.currentTimeMillis()
    private var endTime: Long     = System.currentTimeMillis()
    private var photoUri: Uri?    = null

    // String user ID
    private val currentUserId: String
        get() = intent.getStringExtra(LoginActivity.EXTRA_USER_ID)
            ?: throw IllegalStateException("Missing user ID in Intent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_entry)

        // bind views
        etDescription = findViewById(R.id.etDescription)
        etAmount      = findViewById(R.id.etAmount)
        btnPickDate   = findViewById(R.id.btnPickDate)
        btnPickStart  = findViewById(R.id.btnPickStartTime)
        btnPickEnd    = findViewById(R.id.btnPickEndTime)
        spinnerCat    = findViewById(R.id.spinnerCategory)
        btnTakePhoto  = findViewById(R.id.btnTakePhoto)
        btnAttach     = findViewById(R.id.btnAttachPhoto)
        ivPreview     = findViewById(R.id.ivPhotoPreview)
        btnSave       = findViewById(R.id.btnSaveEntry)

        // load category list
        loadCategories()

        // date picker
        btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this,
                { _, y, m, d ->
                    cal.set(y, m, d)
                    pickedDate = cal.timeInMillis
                    btnPickDate.text = DATE_FORMAT.format(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // start time picker
        btnPickStart.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this,
                { _, hh, mm ->
                    cal.set(Calendar.HOUR_OF_DAY, hh)
                    cal.set(Calendar.MINUTE, mm)
                    startTime = cal.timeInMillis
                    btnPickStart.text = TIME_FORMAT.format(cal.time)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // end time picker
        btnPickEnd.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this,
                { _, hh, mm ->
                    cal.set(Calendar.HOUR_OF_DAY, hh)
                    cal.set(Calendar.MINUTE, mm)
                    endTime = cal.timeInMillis
                    btnPickEnd.text = TIME_FORMAT.format(cal.time)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        // launch camera
        btnTakePhoto.setOnClickListener {
            val photoFile = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Camera error: ${ex.message}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(intent, REQUEST_CAMERA)
            }
        }

        // pick from gallery
        btnAttach.setOnClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
                startActivityForResult(this, REQUEST_GALLERY)
            }
        }

        // save expense
        btnSave.setOnClickListener {
            val desc = etDescription.text.toString().trim()
            if (desc.isBlank()) {
                Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedCatId = spinnerCat.selectedItemTag as? String
            if (selectedCatId.isNullOrEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newId = dbRef.child("expenses")
                .child(currentUserId)
                .push()
                .key!!
            val entry = ExpenseEntry(
                id          = newId,
                userId      = currentUserId,
                categoryId  = selectedCatId,
                date        = pickedDate,
                startTime   = startTime,
                endTime     = endTime,
                description = desc,
                photoUri    = photoUri?.toString(),
                amount      = amount
            )

            dbRef.child("expenses")
                .child(currentUserId)
                .child(newId)
                .setValue(entry)
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    // handles both the camera & gallery results...
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GALLERY -> {
                data?.data?.let { uri ->
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    photoUri = uri
                    ivPreview.setImageURI(uri)
                    ivPreview.visibility = ImageView.VISIBLE
                }
            }
            REQUEST_CAMERA -> {
                // photoUri already points to our temp file
                ivPreview.setImageURI(photoUri)
                ivPreview.visibility = ImageView.VISIBLE
            }
        }
    }

    // create a temp file for the camera
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())
        val storageDir = cacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun loadCategories() {
        dbRef.child("categories")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val cats = snapshot.children.mapNotNull {
                        it.getValue(Category::class.java)
                    }
                    val names = cats.map { it.name }
                    val ids   = cats.map { it.id }
                    val adapter = ArrayAdapter(
                        this@ExpenseEntryActivity,
                        android.R.layout.simple_spinner_item,
                        names
                    ).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                    spinnerCat.adapter = adapter
                    spinnerCat.tag = ids
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ExpenseEntryActivity,
                        "Failed to load categories: ${error.message}",
                        Toast.LENGTH_LONG).show()
                }
            })
    }

    // helper to retrieve the underlying ID list from the spinner’s tag
    private val Spinner.selectedItemTag: Any?
        get() {
            val ids = tag as? List<String> ?: return null
            return ids.getOrNull(selectedItemPosition)
        }
}
