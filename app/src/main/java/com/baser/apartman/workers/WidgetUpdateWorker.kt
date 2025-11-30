package com.baser.apartman.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.baser.apartman.widgets.AidatWidgetUtils
import com.baser.apartman.widgets.DuyuruWidgetUtils
import kotlinx.coroutines.delay

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WidgetUpdateWorker", "🔄 Widget otomatik güncelleme başlatıldı...")
            
            // Kısa bir bekleme (opsiyonel)
            delay(1000)
            
            // Her iki widget'ı da güncelle
            AidatWidgetUtils.updateWidgets(applicationContext)
            DuyuruWidgetUtils.updateWidgets(applicationContext)
            
            Log.d("WidgetUpdateWorker", "✅ Widget'lar başarıyla güncellendi")
            Result.success()
            
        } catch (e: Exception) {
            Log.e("WidgetUpdateWorker", "❌ Widget güncelleme hatası: ${e.message}", e)
            Result.failure()
        }
    }
    
    companion object {
        private const val WORK_NAME = "widget_auto_update_work"
        
        // Work'ü başlatmak için yardımcı fonksiyon
        fun scheduleWidgetUpdate(context: Context) {
            try {
                val updateRequest = androidx.work.PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                    java.time.Duration.ofMinutes(15) // Her 30 dakikada bir
                )
                    .setInitialDelay(java.time.Duration.ofMinutes(2)) // İlk 2 dakika sonra başla
                    .build()
                
                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP, // Zaten varsa koru
                    updateRequest
                )
                
                Log.d("WidgetUpdateWorker", "📅 Widget otomatik güncelleme zamanlayıcısı başlatıldı")
            } catch (e: Exception) {
                Log.e("WidgetUpdateWorker", "❌ Zamanlayıcı başlatma hatası: ${e.message}")
            }
        }
        
        // Work'ü durdurmak için
        fun cancelWidgetUpdate(context: Context) {
            try {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d("WidgetUpdateWorker", "⏹️ Widget otomatik güncelleme durduruldu")
            } catch (e: Exception) {
                Log.e("WidgetUpdateWorker", "❌ Zamanlayıcı durdurma hatası: ${e.message}")
            }
        }
    }
}