package com.example.wodifyplus

import android.app.Application

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WodifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si es necesario
    }
}

