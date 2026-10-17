package com.baser.apartman

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object NamazVaktiHelper {

    private const val TAG = "NamazVaktiHelper"

    const val REQUEST_IMSAK = 100
    const val REQUEST_OGLE = 102
    const val REQUEST_IKINDI = 103
    const val REQUEST_AKSAM = 104
    const val REQUEST_YATSI = 105
    const val REQUEST_GUNLUK_YENILE = 999

    private val VAKIT_LISTESI = listOf("İmsak", "Öğle", "İkindi", "Akşam", "Yatsı")

    fun getRequestCode(vakitAdi: String): Int {
        return when (vakitAdi) {
            "İmsak" -> REQUEST_IMSAK
            "Öğle" -> REQUEST_OGLE
            "İkindi" -> REQUEST_IKINDI
            "Akşam" -> REQUEST_AKSAM
            "Yatsı" -> REQUEST_YATSI
            else -> 100
        }
    }

    /**
     * Tüm kurulmuş namaz alarmlarını eksiksiz iptal eder.
     * Intent action'ları birebir eşleşecek şekilde PendingIntent'ler temizlenir.
     */
    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for (vakit in VAKIT_LISTESI) {
            val reqCode = getRequestCode(vakit)

            // 1. ERKEN Intent iptal
            val erkenIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                action = "ERKEN_$vakit"
            }
            PendingIntent.getBroadcast(
                context, reqCode + 2000, erkenIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            // 2. ACMA (Tam vakit) Intent iptal
            val acmaIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                action = "ACMA_$vakit"
            }
            PendingIntent.getBroadcast(
                context, reqCode, acmaIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            // 3. KAPATMA Intent iptal
            val kapatmaIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                action = "KAPATMA_$vakit"
            }
            PendingIntent.getBroadcast(
                context, reqCode + 1000, kapatmaIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        // Günlük yenileme alarmını da iptal et
        val gunlukIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
            action = "YENILE_GUNLUK"
        }
        PendingIntent.getBroadcast(
            context, REQUEST_GUNLUK_YENILE, gunlukIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        Log.d(TAG, "🧹 Tüm namaz alarmları başarıyla temizlendi.")
    }

    /**
     * Namaz alarmlarını kurar.
     */
    fun scheduleNamazAlarms(context: Context, vakit: NamazVakti) {
        val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val isAutoEnabled = prefs.getBoolean("otomatik_aktif", false)
        if (!isAutoEnabled) {
            Log.d(TAG, "⚠️ Otomatik mod pasif, alarm kurulmadı.")
            return
        }

        cancelAllAlarms(context)

        val dndSuresi = prefs.getInt("dnd_suresi", 30)
        val erkenSure = prefs.getInt("erken_baslatma_suresi", 5)

        scheduleVakitAlarm(context, vakit.imsak, "İmsak", REQUEST_IMSAK, dndSuresi, erkenSure)
        scheduleVakitAlarm(context, vakit.ogle, "Öğle", REQUEST_OGLE, dndSuresi, erkenSure)
        scheduleVakitAlarm(context, vakit.ikindi, "İkindi", REQUEST_IKINDI, dndSuresi, erkenSure)
        scheduleVakitAlarm(context, vakit.aksam, "Akşam", REQUEST_AKSAM, dndSuresi, erkenSure)
        scheduleVakitAlarm(context, vakit.yatsi, "Yatsı", REQUEST_YATSI, dndSuresi, erkenSure)

        scheduleDailyRefreshAlarm(context)
        calculateNextPrayerTime(context, vakit)

        Log.d(TAG, "✅ 5 vakit ve günlük yenileme alarmları kuruldu.")
    }

    private fun scheduleVakitAlarm(
        context: Context,
        saatStr: String,
        vakitAdi: String,
        requestCode: Int,
        dndSuresi: Int,
        erkenSure: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        try {
            val parts = saatStr.split(":")
            if (parts.size < 2) return

            val now = System.currentTimeMillis()

            val vakitCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                set(Calendar.MINUTE, parts[1].trim().toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Vakit geçtiyse yarın için kur
            if (vakitCal.timeInMillis <= now) {
                vakitCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val vakitMillis = vakitCal.timeInMillis
            val bitisMillis = vakitMillis + (dndSuresi * 60 * 1000L)
            val erkenMillis = vakitMillis - (erkenSure * 60 * 1000L)

            // 1. Erken Başlatma Alarmı (Sadece erkenSure > 0 ise ve gelecekteyse)
            if (erkenSure > 0 && erkenMillis > now) {
                val erkenIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                    action = "ERKEN_$vakitAdi"
                    putExtra("vakit", vakitAdi)
                    putExtra("dnd_suresi", dndSuresi + erkenSure)
                    putExtra("erken_baslatma", true)
                    putExtra("bitis_zamani", bitisMillis)
                }
                val erkenPendingIntent = PendingIntent.getBroadcast(
                    context, requestCode + 2000, erkenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                setExactAlarm(alarmManager, erkenMillis, erkenPendingIntent)
            }

            // 2. Tam Vakit Alarmı
            val acmaIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                action = "ACMA_$vakitAdi"
                putExtra("vakit", vakitAdi)
                putExtra("dnd_suresi", dndSuresi)
                putExtra("erken_baslatildi", erkenSure > 0 && erkenMillis <= now)
                putExtra("bitis_zamani", bitisMillis)
            }
            val acmaPendingIntent = PendingIntent.getBroadcast(
                context, requestCode, acmaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(alarmManager, vakitMillis, acmaPendingIntent)

            // 3. Kapatma Alarmı
            val kapatmaIntent = Intent(context, NamazVaktiReceiver::class.java).apply {
                action = "KAPATMA_$vakitAdi"
                putExtra("vakit", vakitAdi)
            }
            val kapatmaPendingIntent = PendingIntent.getBroadcast(
                context, requestCode + 1000, kapatmaIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(alarmManager, bitisMillis, kapatmaPendingIntent)

            Log.d(TAG, "⏰ [$vakitAdi] Alarmlar kuruldu: Vakit=${formatTime(vakitMillis)}, Bitiş=${formatTime(bitisMillis)}")

        } catch (e: Exception) {
            Log.e(TAG, "Alarm kurma hatası ($vakitAdi): ${e.message}")
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Her gece saat 00:05'te vakitleri API'den tazelemek için günlük alarm kurar.
     */
    private fun scheduleDailyRefreshAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, NamazVaktiReceiver::class.java).apply {
            action = "YENILE_GUNLUK"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_GUNLUK_YENILE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(alarmManager, cal.timeInMillis, pendingIntent)
        Log.d(TAG, "📅 Günlük yenileme alarmı kuruldu: ${formatTime(cal.timeInMillis)}")
    }

    fun calculateNextPrayerTime(context: Context, vakit: NamazVakti) {
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance()

        val vakitler = listOf(
            Pair("İmsak", parseTimeToMillis(vakit.imsak, today)),
            Pair("Öğle", parseTimeToMillis(vakit.ogle, today)),
            Pair("İkindi", parseTimeToMillis(vakit.ikindi, today)),
            Pair("Akşam", parseTimeToMillis(vakit.aksam, today)),
            Pair("Yatsı", parseTimeToMillis(vakit.yatsi, today))
        )

        val gelecekVakitler = vakitler.map { (name, time) ->
            if (time > now) {
                Triple(name, time, time)
            } else {
                val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                val tomorrowTime = parseTimeToMillis(getTimeString(name, vakit), tomorrow)
                Triple(name, tomorrowTime, tomorrowTime)
            }
        }.filter { it.second > now }

        val nextPrayer = gelecekVakitler.minByOrNull { it.second }

        nextPrayer?.let {
            val sonrakiVakitAdi = it.first
            val sonrakiVakitZamani = it.second
            val sonrakiVakitText = "${it.first}: ${formatTime(it.second)}"

            val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("sonraki_vakit_adi", sonrakiVakitAdi)
                .putLong("sonraki_vakit_zamani", sonrakiVakitZamani)
                .putString("sonraki_vakit_text", sonrakiVakitText)
                .apply()

            NamazVaktiServisi.sonrakiVakitAdi = sonrakiVakitAdi
            NamazVaktiServisi.sonrakiVakitZamani = sonrakiVakitZamani
            NamazVaktiServisi.sonrakiVakit = sonrakiVakitText

            Log.d(TAG, "🕌 SONRAKİ VAKİT: $sonrakiVakitText")
        }
    }

    fun getVakitlerFromPrefs(context: Context): NamazVakti? {
        val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val imsak = prefs.getString("kayitli_imsak", null) ?: return null
        val ogle = prefs.getString("kayitli_ogle", null) ?: return null
        val ikindi = prefs.getString("kayitli_ikindi", null) ?: return null
        val aksam = prefs.getString("kayitli_aksam", null) ?: return null
        val yatsi = prefs.getString("kayitli_yatsi", null) ?: return null
        val gunes = prefs.getString("kayitli_gunes", "07:00") ?: "07:00"
        val ilce = prefs.getString("ilce", "Yenişehir") ?: "Yenişehir"
        val tarih = prefs.getString("kayitli_tarih", SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()))

        return NamazVakti(
            ilce = ilce,
            imsak = imsak,
            gunes = gunes,
            ogle = ogle,
            ikindi = ikindi,
            aksam = aksam,
            yatsi = yatsi,
            tarih = tarih ?: ""
        )
    }

    fun saveVakitlerToPrefs(context: Context, vakit: NamazVakti) {
        val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("kayitli_imsak", vakit.imsak)
            putString("kayitli_gunes", vakit.gunes)
            putString("kayitli_ogle", vakit.ogle)
            putString("kayitli_ikindi", vakit.ikindi)
            putString("kayitli_aksam", vakit.aksam)
            putString("kayitli_yatsi", vakit.yatsi)
            putString("kayitli_tarih", vakit.tarih)
            putLong("kayitli_zaman", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "💾 Vakitler SharedPreferences'a kaydedildi.")
    }

    /**
     * Vakitleri internetten (API) çeker ve alarmları kurar.
     */
    fun refreshVakitlerFromApi(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val il = prefs.getString("il", "MERSİN") ?: "MERSİN"
        val ilce = prefs.getString("ilce", "Yenişehir") ?: "Yenişehir"

        Log.d(TAG, "🌐 API'den vakitler yenileniyor: $il / $ilce")

        NamazVaktiApi(context).getNamazVakitleri(
            il = il,
            ilce = ilce,
            onSuccess = { vakitler ->
                if (vakitler.isNotEmpty()) {
                    val bugun = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())
                    val bugununVakti = vakitler.find { it.tarih == bugun } ?: vakitler[0]

                    saveVakitlerToPrefs(context, bugununVakti)
                    scheduleNamazAlarms(context, bugununVakti)
                    onComplete?.invoke(true)
                } else {
                    onComplete?.invoke(false)
                }
            },
            onError = { error ->
                Log.e(TAG, "API hatası: $error. Kayıtlı vakitler kullanılıyor.")
                val kayitli = getVakitlerFromPrefs(context)
                if (kayitli != null) {
                    scheduleNamazAlarms(context, kayitli)
                }
                onComplete?.invoke(false)
            }
        )
    }

    private fun getTimeString(vakitAdi: String, vakit: NamazVakti): String {
        return when (vakitAdi) {
            "İmsak" -> vakit.imsak
            "Öğle" -> vakit.ogle
            "İkindi" -> vakit.ikindi
            "Akşam" -> vakit.aksam
            "Yatsı" -> vakit.yatsi
            else -> "00:00"
        }
    }

    private fun parseTimeToMillis(timeStr: String, calendar: Calendar): Long {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0

        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
        cal.set(Calendar.MINUTE, parts[1].trim().toInt())
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}
