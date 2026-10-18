package com.baser.apartman

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.*
import android.util.Log

class OdemeDialogActivity : AppCompatActivity() {
    private val TAG = "OdemeDialogActivity"

    private var dueId: Int = 0
    private lateinit var userEmail: String
    private lateinit var userType: String
    private var totalAmount: Double = 0.0
    private var remainingAmount: Double = 0.0
    private var lateFeeAmount: Double = 0.0
    private lateinit var description: String
    private var siteId: Int = 1
    private lateinit var userName: String
    private lateinit var apartmentCode: String

    // UI Elements
    private lateinit var tvPaymentTypeInfo: TextView
    private lateinit var rgPaymentType: RadioGroup
    private lateinit var rbFullPayment: RadioButton
    private lateinit var rbPartialPayment: RadioButton
    private lateinit var etPaymentAmount: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent'ten verileri al
        dueId = intent.getIntExtra("due_id", 0)
        userEmail = intent.getStringExtra("user_email") ?: ""
        userType = intent.getStringExtra("user_type") ?: ""
        totalAmount = intent.getDoubleExtra("total_amount", 0.0)
        remainingAmount = intent.getDoubleExtra("remaining_amount", 0.0)
        lateFeeAmount = intent.getDoubleExtra("late_fee_amount", 0.0)
        description = intent.getStringExtra("description") ?: ""
        siteId = intent.getIntExtra("site_id", 1)
        userName = intent.getStringExtra("user_name") ?: ""
        apartmentCode = intent.getStringExtra("apartment_code") ?: ""

        // KONTROL: Sadece admin/manager ödeme yapabilir
        if (userType != "admin" && userType != "manager") {
            AlertDialog.Builder(this)
                .setTitle("Yetki Hatası")
                .setMessage("Ödeme işlemleri sadece yöneticiler tarafından yapılabilir.")
                .setPositiveButton("Tamam") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }

        // Dialog göster
        showPaymentDialog()
    }

    private fun showPaymentDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_odeme, null)

        // UI elementlerini bul
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvTotalAmount)
        val tvRemaining = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        etPaymentAmount = dialogView.findViewById(R.id.etPaymentAmount)
        val spPaymentMethod = dialogView.findViewById<Spinner>(R.id.spPaymentMethod)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val btnOdemeYap = dialogView.findViewById<Button>(R.id.btnOdemeYap)
        val btnIptal = dialogView.findViewById<Button>(R.id.btnIptal)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        // Ödeme tipi UI elementleri
        rgPaymentType = dialogView.findViewById(R.id.rgPaymentType)
        rbFullPayment = dialogView.findViewById(R.id.rbFullPayment)
        rbPartialPayment = dialogView.findViewById(R.id.rbPartialPayment)
        tvPaymentTypeInfo = dialogView.findViewById(R.id.tvPaymentTypeInfo)

        // Değerleri ayarla
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        tvAmount.text = "Toplam Borç: ${numberFormat.format(totalAmount)}"
        tvRemaining.text = "Kalan Borç: ${numberFormat.format(remainingAmount)}"
        tvDescription.text = "Açıklama: $description"

        // Site bilgisi
        dialogView.findViewById<TextView>(R.id.tvSiteInfo)?.text = "Site: $siteId"

        // Kullanıcı bilgisi
        dialogView.findViewById<TextView>(R.id.tvUserInfo)?.text = "$userName - $apartmentCode"

        // Gecikme zammı bilgisi
        if (lateFeeAmount > 0) {
            dialogView.findViewById<TextView>(R.id.tvLateFeeInfo)?.text =
                "Gecikme Zammı: ${numberFormat.format(lateFeeAmount)}"
            dialogView.findViewById<TextView>(R.id.tvLateFeeInfo)?.visibility = View.VISIBLE
        }

        // Ödeme yöntemi spinner'ını ayarla
        val paymentMethods = listOf("Nakit", "Banka Havalesi", "EFT", "Kredi Kartı", "Çek", "Diğer")
        val adapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            paymentMethods
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPaymentMethod.adapter = adapter

        // İLK YÜKLEME: Tam ödeme seçili, tutar KİLİTLİ
        rbFullPayment.isChecked = true
        etPaymentAmount.setText(String.format(Locale.US, "%.2f", remainingAmount))

        // BU SATIRLAR KESİN: İlk yüklemede MUTLAKA KİLİTLİ
        etPaymentAmount.isEnabled = false
        etPaymentAmount.isFocusable = false
        etPaymentAmount.isFocusableInTouchMode = false
        etPaymentAmount.isClickable = false
        etPaymentAmount.isCursorVisible = false

        // DEBUG: Kontrol et
        Log.d(TAG, "İlk yükleme - isEnabled: ${etPaymentAmount.isEnabled}")
        Log.d(TAG, "İlk yükleme - isFocusable: ${etPaymentAmount.isFocusable}")

        updatePaymentTypeInfo(true, remainingAmount)

        // RadioGroup değişiklik dinleyicisi - KESİN ÇALIŞAN
        rgPaymentType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFullPayment -> {
                    // Tam ödeme: KİLİTLİ
                    etPaymentAmount.setText(String.format(Locale.US, "%.2f", remainingAmount))
                    etPaymentAmount.isEnabled = false
                    etPaymentAmount.isFocusable = false
                    etPaymentAmount.isFocusableInTouchMode = false
                    etPaymentAmount.isClickable = false
                    etPaymentAmount.isCursorVisible = false
                    updatePaymentTypeInfo(true, remainingAmount)
                }
                R.id.rbPartialPayment -> {
                    // Kısmi ödeme: AÇIK
                    etPaymentAmount.isEnabled = true
                    etPaymentAmount.isFocusable = true
                    etPaymentAmount.isFocusableInTouchMode = true
                    etPaymentAmount.isClickable = true
                    etPaymentAmount.isCursorVisible = true

                    // Eğer tutar tam borç ise sıfırla
                    val currentText = etPaymentAmount.text.toString()
                    val currentAmount = currentText.toDoubleOrNull() ?: 0.0
                    if (currentAmount >= remainingAmount) {
                        etPaymentAmount.setText("")
                        updatePaymentTypeInfo(false, 0.0)
                    } else {
                        updatePaymentTypeInfo(false, currentAmount)
                    }

                    etPaymentAmount.requestFocus()
                }
            }
        }

        // Hızlı ödeme butonları
        setupQuickAmountButtons(dialogView)

        // Dialog oluştur
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ödeme Yap")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Buton tıklamaları
        btnOdemeYap.setOnClickListener {
            val paymentAmountStr = etPaymentAmount.text.toString()
            val paymentMethodIndex = spPaymentMethod.selectedItemPosition
            val notes = etNotes.text.toString()

            // Validasyon
            if (paymentAmountStr.isEmpty()) {
                Toast.makeText(this, "Lütfen ödeme tutarını girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentAmount = paymentAmountStr.toDoubleOrNull()
            if (paymentAmount == null || paymentAmount <= 0) {
                Toast.makeText(this, "Geçersiz ödeme tutarı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paymentAmount > remainingAmount) {
                Toast.makeText(this, "Ödeme tutarı kalan borçtan fazla olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paymentMethodIndex == -1) {
                Toast.makeText(this, "Lütfen ödeme yöntemi seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isFullPayment = rbFullPayment.isChecked

            // Onay dialog'u göster
            showConfirmationDialog(
                paymentAmount,
                paymentMethods[paymentMethodIndex],
                notes,
                isFullPayment,
                dialog,
                progressBar,
                btnOdemeYap
            )
        }

        btnIptal.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun updatePaymentTypeInfo(isFullPayment: Boolean, amount: Double) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val newRemaining = remainingAmount - amount

        tvPaymentTypeInfo.text = if (isFullPayment) {
            "✅ Tam ödeme yapılacak\n💰 Tutar: ${numberFormat.format(amount)}"
        } else {
            val percentage = if (remainingAmount > 0) (amount / remainingAmount * 100).toInt() else 0
            "⚡ Kısmi ödeme: ${numberFormat.format(amount)} (%$percentage)\n📉 Kalan borç: ${numberFormat.format(newRemaining)}"
        }
    }

    private fun setupQuickAmountButtons(dialogView: View) {
        val btnFullAmount = dialogView.findViewById<Button>(R.id.btnFullAmount)
        val btnHalfAmount = dialogView.findViewById<Button>(R.id.btnHalfAmount)
        val btnQuarterAmount = dialogView.findViewById<Button>(R.id.btnQuarterAmount)

        btnFullAmount.setOnClickListener {
            // Tam ödemeye geç - KİLİTLİ
            rbFullPayment.isChecked = true
            etPaymentAmount.setText(String.format(Locale.US, "%.2f", remainingAmount))
            etPaymentAmount.isEnabled = false
            etPaymentAmount.isFocusable = false
            etPaymentAmount.isFocusableInTouchMode = false
            etPaymentAmount.isClickable = false
            updatePaymentTypeInfo(true, remainingAmount)
        }

        btnHalfAmount.setOnClickListener {
            // Kısmi ödemeye geç - AÇIK
            rbPartialPayment.isChecked = true
            etPaymentAmount.isEnabled = true
            etPaymentAmount.isFocusable = true
            etPaymentAmount.isFocusableInTouchMode = true
            etPaymentAmount.isClickable = true
            val halfAmount = remainingAmount / 2
            etPaymentAmount.setText(String.format(Locale.US, "%.2f", halfAmount))
            updatePaymentTypeInfo(false, halfAmount)
        }

        btnQuarterAmount.setOnClickListener {
            // Kısmi ödemeye geç - AÇIK
            rbPartialPayment.isChecked = true
            etPaymentAmount.isEnabled = true
            etPaymentAmount.isFocusable = true
            etPaymentAmount.isFocusableInTouchMode = true
            etPaymentAmount.isClickable = true
            val quarterAmount = remainingAmount / 4
            etPaymentAmount.setText(String.format(Locale.US, "%.2f", quarterAmount))
            updatePaymentTypeInfo(false, quarterAmount)
        }
    }

    private fun showConfirmationDialog(
        paymentAmount: Double,
        paymentMethod: String,
        notes: String,
        isFullPayment: Boolean,
        parentDialog: AlertDialog,
        progressBar: ProgressBar,
        btnOdemeYap: Button
    ) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val newRemaining = remainingAmount - paymentAmount

        val message = buildString {
            append("Ödeme işlemini onaylıyor musunuz?\n\n")
            append("👤 **Sakin:** $userName\n")
            append("🏠 **Daire:** $apartmentCode\n")
            append("💰 **Kalan Borç:** ${numberFormat.format(remainingAmount)}\n")
            append("💳 **Ödeme Tutarı:** ${numberFormat.format(paymentAmount)}\n")
            append("📋 **Ödeme Tipi:** ${if (isFullPayment) "Tam Ödeme ✅" else "Kısmi Ödeme ⚡"}\n")
            append("💸 **Ödeme Yöntemi:** $paymentMethod\n")

            if (isFullPayment) {
                append("\n✅ **Sonuç:** Tüm borç ödenecek")
            } else {
                append("\n⚡ **Sonuç:** Kalan borç: ${numberFormat.format(newRemaining)}")
            }

            if (notes.isNotEmpty()) {
                append("\n📝 **Not:** $notes")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Onayla")
            .setMessage(message)
            .setPositiveButton("EVET, ÖDEMEYİ YAP") { dialog, _ ->
                dialog.dismiss()
                parentDialog.dismiss()
                processPayment(paymentAmount, paymentMethod, notes, isFullPayment, progressBar, btnOdemeYap)
            }
            .setNegativeButton("HAYIR, DÜZENLE") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun processPayment(
        paymentAmount: Double,
        paymentMethodText: String,
        notes: String,
        isFullPayment: Boolean,
        progressBar: ProgressBar,
        btnOdemeYap: Button
    ) {
        val paymentMethodCode = when (paymentMethodText) {
            "Nakit" -> "cash"
            "Banka Havalesi" -> "bank_transfer"
            "EFT" -> "eft"
            "Kredi Kartı" -> "credit_card"
            "Çek" -> "check"
            else -> "other"
        }

        // Progress bar göster
        progressBar.visibility = View.VISIBLE
        btnOdemeYap.isEnabled = false

        Log.d(TAG, "Ödeme başlatılıyor: Due ID: $dueId, Tutar: $paymentAmount, Tam Ödeme: $isFullPayment")

        // API'yi çağır
        AidatApiService.makePayment(
            dueId = dueId,
            paymentAmount = paymentAmount,
            paymentMethod = paymentMethodCode,
            notes = notes,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { result ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnOdemeYap.isEnabled = true

                    if (result.success) {
                        showSuccessDialog(result, isFullPayment)
                    } else {
                        showErrorDialog("Ödeme hatası: ${result.message}")
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnOdemeYap.isEnabled = true
                    showErrorDialog("Sunucu hatası: $error")
                }
            }
        )
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Hata")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun showSuccessDialog(result: AidatApiService.PaymentResult, isFullPayment: Boolean) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val paymentDetails = result.payment_details

        val message = buildString {
            append("✅ Ödeme Başarılı!\n\n")
            append("💳 Ödeme Tutarı: ${numberFormat.format(paymentDetails?.payment_amount ?: 0.0)}\n")
            append("📋 Ödeme Tipi: ${if (isFullPayment) "Tam Ödeme ✅" else "Kısmi Ödeme ⚡"}\n")

            if ((paymentDetails?.paid_late_fee ?: 0.0) > 0) {
                append("⚠️ Gecikme Zammı: ${numberFormat.format(paymentDetails?.paid_late_fee)}\n")
            }

            append("📋 Fiş Numarası: ${paymentDetails?.receipt_number ?: ""}\n")
            append("🔢 İşlem No: ${paymentDetails?.transaction_id ?: ""}\n")

            if ((paymentDetails?.site_id ?: 1) > 1) {
                append("🏢 Site: ${paymentDetails?.site_id}\n")
            }

            if ((paymentDetails?.new_remaining ?: 0.0) > 0) {
                append("\n📉 Yeni Kalan Borç: ${numberFormat.format(paymentDetails?.new_remaining)}")
            } else {
                append("\n🎉 Tüm borç ödendi!")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Ödeme Tamamlandı")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}