package com.baser.apartman.widgets

import android.content.Context

object DuyuruWidgetUtils {

    fun updateWidgets(context: Context) {
        try {
            val appWidget = DuyuruAppWidget()
            appWidget.updateAllWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
            // Widget henüz eklenmemiş olabilir
        }
    }

    // Yeni duyuru eklendiğinde çağrılacak metod
    fun onNewDuyuruAdded(context: Context) {
        try {
            updateWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}