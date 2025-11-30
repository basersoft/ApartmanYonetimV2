package com.baser.apartman

import android.app.Application
import com.baser.apartman.workers.WidgetUpdateWorker

class MyApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Uygulama başladığında widget güncelleme zamanlayıcısını başlat
        WidgetUpdateWorker.scheduleWidgetUpdate(this)
        
        println("🔍 MY APPLICATION BAŞLATILDI - Widget güncelleme aktif")
    }
}