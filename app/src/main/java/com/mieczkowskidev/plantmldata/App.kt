package com.mieczkowskidev.plantmldata

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Created by Patryk Mieczkowski on 31.05.2018
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

    }
}