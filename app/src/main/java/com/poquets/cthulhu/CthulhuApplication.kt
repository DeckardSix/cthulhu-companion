package com.poquets.cthulhu

import android.app.Application
import android.util.Log
import com.poquets.cthulhu.shared.database.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CthulhuApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()
        Log.d("CthulhuApplication", "Cthulhu Companion initialized")
        
        // Initialize database in background
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                Log.d("CthulhuApplication", "Initializing unified database...")
                val (arkhamCount, eldritchCount) = DatabaseInitializer.initializeDatabase(
                    this@CthulhuApplication,
                    forceReinit = false
                )
                Log.d("CthulhuApplication", "Database initialized: $arkhamCount Arkham cards, $eldritchCount Eldritch cards")
            } catch (e: Exception) {
                Log.e("CthulhuApplication", "Error initializing database: ${e.message}", e)
            }
        }
    }
}

