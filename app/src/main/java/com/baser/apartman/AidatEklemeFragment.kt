package com.baser.apartman

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class AidatEklemeFragment : Fragment() {

    private lateinit var btnYillikAidatEkle: Button
    private lateinit var btnAylikAidatEkle: Button
    private lateinit var btnTopluAidatEkle: Button
    private lateinit var btnAddBulkDues: Button
    private lateinit var spinnerBulkType: Spinner
    private lateinit var etBulkAmount: EditText
    private lateinit var etBulkDueDate: EditText
    private lateinit var etBulkDescription: EditText
    private lateinit var switchExcludePaid: Switch
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_aidat_ekleme, container, false)
        setupViews(view)
        setupSpinner()
        setupDatePicker()
        setDefaultDates()
        setupButtons()
        return view
    }

    private fun setupViews(view: View) {
        btnYillikAidatEkle = view.findViewById(R.id.btnYillikAidatEkle)
        btnAylikAidatEkle = view.findViewById(R.id.btnAylikAidatEkle)
        btnTopluAidatEkle = view.findViewById(R.id.btnTopluAidatEkle)
        btnAddBulkDues = view.findViewById(R.id.btnAddBulkDues)
        spinnerBulkType = view.findViewById(R.id.spinnerBulkType)
        etBulkAmount = view.findViewById(R.id.etBulkAmount)
        etBulkDueDate = view.findViewById(R.id.etBulkDueDate)
        etBulkDescription = view.findViewById(R.id.etBulkDescription)
        switchExcludePaid = view.findViewById(R.id.switchExcludePaid)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val types = arrayOf("Tüm Kullanıcılar", "Sadece Ödemeyenler")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBulkType.adapter = adapter
    }

    private fun setupDatePicker() {
        etBulkDueDate.setOnClickListener {
            showDatePicker { selectedDate ->
                etBulkDueDate.setText(selectedDate)
            }
        }
    }

    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 15)
        val defaultDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        etBulkDueDate.setText(defaultDate)
    }

    private fun setupButtons() {
        btnYillikAidatEkle.setOnClickListener {
            showYearlyAidatDialog()
        }

        btnAylikAidatEkle.setOnClickListener {
            showMonthlyAidatDialog()
        }

        btnTopluAidatEkle.setOnClickListener {
            showBulkAidatDialog()
        }

        btnAddBulkDues.setOnClickListener {
            addBulkAidatFromForm()
        }
    }

    // YILLIK AIDAT EKLEME DIALOG
    private fun showYearlyAidatDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        // Yıl input
        val tvYear = TextView(requireContext())
        tvYear.text = "Yıl"
        tvYear.textSize = 14f
        layout.addView(tvYear)

        val etYear = EditText(requireContext())
        etYear.hint = "2024"
        etYear.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etYear.setBackgroundResource(R.drawable.edittext_background)
        etYear.setPadding(40, 30, 40, 30)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        etYear.setText(currentYear.toString())
        layout.addView(etYear)

        // Aylık tutar input
        val tvMonthlyAmount = TextView(requireContext())
        tvMonthlyAmount.text = "Aylık Tutar (₺)"
        tvMonthlyAmount.textSize = 14f
        tvMonthlyAmount.setPadding(0, 30, 0, 0)
        layout.addView(tvMonthlyAmount)

        val etMonthlyAmount = EditText(requireContext())
        etMonthlyAmount.hint = "500.00"
        etMonthlyAmount.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        etMonthlyAmount.setBackgroundResource(R.drawable.edittext_background)
        etMonthlyAmount.setPadding(40, 30, 40, 30)
        layout.addView(etMonthlyAmount)

        // Açıklama input
        val tvDescription = TextView(requireContext())
        tvDescription.text = "Açıklama"
        tvDescription.textSize = 14f
        tvDescription.setPadding(0, 30, 0, 0)
        layout.addView(tvDescription)

        val etDescription = EditText(requireContext())
        etDescription.hint = "2024 Yılı Aidatları"
        etDescription.setBackgroundResource(R.drawable.edittext_background)
        etDescription.setPadding(40, 30, 40, 30)
        layout.addView(etDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Yıllık Aidat Ekle")
            .setView(layout)
            .setPositiveButton("Ekle") { dialogInterface, _ ->
                val year = etYear.text.toString().toIntOrNull()
                val monthlyAmount = etMonthlyAmount.text.toString().toDoubleOrNull()
                val description = etDescription.text.toString()

                if (year == null || monthlyAmount == null || description.isEmpty()) {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                } else {
                    addYearlyAidat(year, monthlyAmount, description)
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    // AYLIK AIDAT EKLEME DIALOG
    private fun showMonthlyAidatDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        // Ay input
        val tvMonth = TextView(requireContext())
        tvMonth.text = "Ay (YYYY-MM)"
        tvMonth.textSize = 14f
        layout.addView(tvMonth)

        val etMonth = EditText(requireContext())
        etMonth.hint = "2024-01"
        etMonth.isFocusable = false
        etMonth.isClickable = true
        etMonth.setBackgroundResource(R.drawable.edittext_background)
        etMonth.setPadding(40, 30, 40, 30)

        // Varsayılan değer
        val calendar = Calendar.getInstance()
        val currentMonth = "${calendar.get(Calendar.YEAR)}-${(calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}"
        etMonth.setText(currentMonth)

        etMonth.setOnClickListener {
            showMonthYearPicker { selectedMonth ->
                etMonth.setText(selectedMonth)
            }
        }
        layout.addView(etMonth)

        // Tutar input
        val tvAmount = TextView(requireContext())
        tvAmount.text = "Tutar (₺)"
        tvAmount.textSize = 14f
        tvAmount.setPadding(0, 30, 0, 0)
        layout.addView(tvAmount)

        val etAmount = EditText(requireContext())
        etAmount.hint = "500.00"
        etAmount.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        etAmount.setBackgroundResource(R.drawable.edittext_background)
        etAmount.setPadding(40, 30, 40, 30)
        layout.addView(etAmount)

        // Açıklama input
        val tvDescription = TextView(requireContext())
        tvDescription.text = "Açıklama"
        tvDescription.textSize = 14f
        tvDescription.setPadding(0, 30, 0, 0)
        layout.addView(tvDescription)

        val etDescription = EditText(requireContext())
        etDescription.hint = "Ocak 2024 Aidatı"
        etDescription.setBackgroundResource(R.drawable.edittext_background)
        etDescription.setPadding(40, 30, 40, 30)
        layout.addView(etDescription)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Aylık Aidat Ekle")
            .setView(layout)
            .setPositiveButton("Ekle") { dialogInterface, _ ->
                val month = etMonth.text.toString()
                val amount = etAmount.text.toString().toDoubleOrNull()
                val description = etDescription.text.toString()

                if (month.isEmpty() || amount == null || description.isEmpty()) {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                } else {
                    addMonthlyAidat(month, amount, description)
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    // TOPLU AIDAT EKLEME DIALOG
    private fun showBulkAidatDialog() {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        // Tip seçimi
        val tvType = TextView(requireContext())
        tvType.text = "Tip"
        tvType.textSize = 14f
        layout.addView(tvType)

        val spinnerType = Spinner(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("Tüm Kullanıcılar", "Sadece Ödemeyenler"))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter
        layout.addView(spinnerType)

        // Tutar input
        val tvAmount = TextView(requireContext())
        tvAmount.text = "Tutar (₺)"
        tvAmount.textSize = 14f
        tvAmount.setPadding(0, 30, 0, 0)
        layout.addView(tvAmount)

        val etAmount = EditText(requireContext())
        etAmount.hint = "500.00"
        etAmount.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        etAmount.setBackgroundResource(R.drawable.edittext_background)
        etAmount.setPadding(40, 30, 40, 30)
        layout.addView(etAmount)

        // Son tarih input
        val tvDueDate = TextView(requireContext())
        tvDueDate.text = "Son Tarih"
        tvDueDate.textSize = 14f
        tvDueDate.setPadding(0, 30, 0, 0)
        layout.addView(tvDueDate)

        val etDueDate = EditText(requireContext())
        etDueDate.hint = "YYYY-AA-GG"
        etDueDate.isFocusable = false
        etDueDate.isClickable = true
        etDueDate.setBackgroundResource(R.drawable.edittext_background)
        etDueDate.setPadding(40, 30, 40, 30)

        // Varsayılan tarih
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 15)
        val defaultDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        etDueDate.setText(defaultDate)

        etDueDate.setOnClickListener {
            showDatePicker { selectedDate ->
                etDueDate.setText(selectedDate)
            }
        }
        layout.addView(etDueDate)

        // Açıklama input
        val tvDescription = TextView(requireContext())
        tvDescription.text = "Açıklama"
        tvDescription.textSize = 14f
        tvDescription.setPadding(0, 30, 0, 0)
        layout.addView(tvDescription)

        val etDescription = EditText(requireContext())
        etDescription.hint = "Aidat Açıklaması"
        etDescription.setBackgroundResource(R.drawable.edittext_background)
        etDescription.setPadding(40, 30, 40, 30)
        layout.addView(etDescription)

        // Ödenenleri hariç tut switch
        val switchExcludePaid = Switch(requireContext())
        switchExcludePaid.text = "Ödenen aidatları hariç tut"
        switchExcludePaid.isChecked = true
        switchExcludePaid.setPadding(0, 30, 0, 0)
        layout.addView(switchExcludePaid)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Toplu Aidat Ekle")
            .setView(layout)
            .setPositiveButton("Ekle") { dialogInterface, _ ->
                val type = if (spinnerType.selectedItemPosition == 0) "all" else "unpaid"
                val amount = etAmount.text.toString().toDoubleOrNull()
                val dueDate = etDueDate.text.toString()
                val description = etDescription.text.toString()
                val excludePaid = switchExcludePaid.isChecked

                if (amount == null || dueDate.isEmpty() || description.isEmpty()) {
                    Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                } else {
                    addBulkAidat(type, amount, dueDate, description, excludePaid)
                    dialogInterface.dismiss()
                }
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    // FORM İLE TOPLU AIDAT EKLEME
    private fun addBulkAidatFromForm() {
        val type = if (spinnerBulkType.selectedItemPosition == 0) "all" else "unpaid"
        val amount = etBulkAmount.text.toString().toDoubleOrNull()
        val dueDate = etBulkDueDate.text.toString()
        val description = etBulkDescription.text.toString()
        val excludePaid = switchExcludePaid.isChecked

        if (amount == null || dueDate.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        addBulkAidat(type, amount, dueDate, description, excludePaid)
    }

    // API İŞLEMLERİ - DÜZELTİLMİŞ
    private fun addYearlyAidat(year: Int, monthlyAmount: Double, description: String) {
        progressBar.visibility = View.VISIBLE

        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        AidatApiService.addYearlyAidatAndroid(
            year = year,
            monthlyAmount = monthlyAmount,
            description = description,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { message ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Hata: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun addMonthlyAidat(month: String, amount: Double, description: String) {
        progressBar.visibility = View.VISIBLE

        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        AidatApiService.addMonthlyAidatAndroid(
            month = month,
            amount = amount,
            description = description,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { message ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Hata: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun addBulkAidat(type: String, amount: Double, dueDate: String, description: String, excludePaid: Boolean) {
        progressBar.visibility = View.VISIBLE

        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        AidatApiService.addBulkAidat(
            bulkType = type,
            bulkAmount = amount,
            bulkDueDate = dueDate,
            bulkDescription = description,
            excludePaid = excludePaid,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { message ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                    // Formu temizle
                    etBulkAmount.text.clear()
                    etBulkDueDate.text.clear()
                    etBulkDescription.text.clear()
                    setDefaultDates()
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Hata: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // TARİH SEÇİCİLER
    private fun showMonthYearPicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, _ ->
            val formattedMonth = "${selectedYear}-${(selectedMonth + 1).toString().padStart(2, '0')}"
            onDateSelected(formattedMonth)
        }, year, month, 1).show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(selectedDate)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}