package com.example.budgethive

import android.app.Application
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //can enable offline caching
        Firebase.database.setPersistenceEnabled(true)
    }
}
