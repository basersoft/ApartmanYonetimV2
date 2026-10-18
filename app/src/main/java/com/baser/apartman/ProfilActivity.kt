package com.baser.apartman

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

class ProfilActivity : BaseActivity() {

    private lateinit var ivProfilResmi: ImageView
    private lateinit var tvAdSoyad: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTelefon: TextView
    private lateinit var tvBlok: TextView
    private lateinit var tvDaireNo: TextView
    private lateinit var tvKullaniciTipi: TextView
    private lateinit var tvSakinTipi: TextView
    private lateinit var tvAidatSorumlu: TextView
    private lateinit var tvIkametDurumu: TextView
    private lateinit var tvKayitTarihi: TextView
    private lateinit var tvTarihFormatli: TextView
    private lateinit var tvHatirlatici: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        ApiManager.initialize(this)

        // BaseActivity'deki navigation'ı kur
        super.setupNavigation()

        setupViews()
        loadUserProfile()
    }

    private fun setupViews() {
        ivProfilResmi = findViewById(R.id.ivProfilResmi)
        tvAdSoyad = findViewById(R.id.tvAdSoyad)
        tvEmail = findViewById(R.id.tvEmail)
        tvTelefon = findViewById(R.id.tvTelefon)
        tvBlok = findViewById(R.id.tvBlok)
        tvDaireNo = findViewById(R.id.tvDaireNo)
        tvKullaniciTipi = findViewById(R.id.tvKullaniciTipi)
        tvSakinTipi = findViewById(R.id.tvSakinTipi)
        tvAidatSorumlu = findViewById(R.id.tvAidatSorumlu)
        tvIkametDurumu = findViewById(R.id.tvIkametDurumu)
        tvKayitTarihi = findViewById(R.id.tvKayitTarihi)
        tvTarihFormatli = findViewById(R.id.tvTarihFormatli)
        tvHatirlatici = findViewById(R.id.tvHatirlatici)

        tvAdSoyad.text = userName
        tvEmail.text = userEmail

        ProfileImageManager.loadProfileImage(this, userEmail, ivProfilResmi)
    }

    private fun loadUserProfile() {
        val sqlQuery = "SELECT * FROM apartman_users WHERE email = '$userEmail'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { response ->
                parseUserProfile(response)
            },
            onError = { error ->
                Toast.makeText(this, "Profil yüklenemedi: $error", Toast.LENGTH_SHORT).show()
                showDefaultProfile()
            }
        )
    }

    private fun parseUserProfile(csvResponse: String) {
        try {
            val lines = csvResponse.trim().split("\n")
            if (lines.size > 1) {
                val headers = lines[0].split(",")
                val userData = lines[1].split(",")

                val userMap = mutableMapOf<String, String>()
                for (i in headers.indices) {
                    if (i < userData.size) {
                        userMap[headers[i]] = userData[i]
                    }
                }

                runOnUiThread {
                    tvAdSoyad.text = userMap["name"] ?: userName
                    tvEmail.text = userMap["email"] ?: userEmail
                    tvTelefon.text = userMap["phone"] ?: "Belirtilmemiş"
                    tvBlok.text = userMap["apartment_block"] ?: "Belirtilmemiş"
                    tvDaireNo.text = userMap["apartment_number"] ?: "Belirtilmemiş"

                    val userTypeFromDB = userMap["user_type"] ?: userType
                    tvKullaniciTipi.text = when (userTypeFromDB) {
                        "admin" -> "Yönetici"
                        "manager" -> "Site Sorumlusu"
                        "resident" -> "Sakin"
                        else -> userTypeFromDB
                    }

                    tvSakinTipi.text = when (userMap["resident_type"]) {
                        "owner" -> "Ev Sahibi"
                        "tenant" -> "Kiracı"
                        else -> "Belirtilmemiş"
                    }

                    tvAidatSorumlu.text = when (userMap["is_responsible_for_dues"]) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> "Belirtilmemiş"
                    }

                    tvIkametDurumu.text = when (userMap["is_residing"]) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> "Belirtilmemiş"
                    }

                    tvHatirlatici.text = when (userMap["receive_reminders"]) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> "Belirtilmemiş"
                    }

                    val kayitTarihi = userMap["created_at"] ?: ""
                    tvKayitTarihi.text = kayitTarihi.ifEmpty { "Belirtilmemiş" }
                    tvTarihFormatli.text = formatDate(kayitTarihi)

                    ProfileImageManager.loadProfileImage(this@ProfilActivity, userEmail, ivProfilResmi)

                    Toast.makeText(this, "Profil bilgileri güncellendi", Toast.LENGTH_SHORT).show()
                }
            } else {
                showDefaultProfile()
            }
        } catch (e: Exception) {
            showDefaultProfile()
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            if (dateString.isNotEmpty()) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } else {
                "Belirtilmemiş"
            }
        } catch (e: Exception) {
            "Tarih formatlanamadı"
        }
    }

    private fun showDefaultProfile() {
        runOnUiThread {
            tvAdSoyad.text = userName
            tvEmail.text = userEmail
            tvTelefon.text = "Bulunamadı"
            tvBlok.text = "Bulunamadı"
            tvDaireNo.text = "Bulunamadı"
            tvKullaniciTipi.text = when (userType) {
                "admin" -> "Yönetici"
                "manager" -> "Site Sorumlusu"
                "resident" -> "Sakin"
                else -> userType
            }
            tvSakinTipi.text = "Bulunamadı"
            tvAidatSorumlu.text = "Bulunamadı"
            tvIkametDurumu.text = "Bulunamadı"
            tvKayitTarihi.text = "Bulunamadı"
            tvTarihFormatli.text = "Bulunamadı"
            tvHatirlatici.text = "Bulunamadı"
            loadDefaultProfileImage()
        }
    }

    private fun loadDefaultProfileImage() {
        runOnUiThread {
            ivProfilResmi.setImageResource(R.drawable.ic_profile_placeholder)
            ivProfilResmi.background = resources.getDrawable(R.drawable.circle_background, null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ivProfilResmi.setImageBitmap(null)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ivProfilResmi.setImageBitmap(null)
        System.gc()
    }
}