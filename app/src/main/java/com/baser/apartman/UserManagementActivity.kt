package com.baser.apartman

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.navigation.NavigationView
import java.util.*
import kotlin.collections.ArrayList

class UserManagementActivity : AppCompatActivity() {

    private lateinit var userListView: ListView
    private lateinit var searchView: SearchView
    private lateinit var btnAddUser: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var userList: ArrayList<User>
    private lateinit var filteredUserList: ArrayList<User>
    private lateinit var userAdapter: UserAdapter

    private lateinit var userEmail: String
    private lateinit var userType: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        userEmail = intent.getStringExtra("user_email") ?: ""
        userType = intent.getStringExtra("user_type") ?: "resident"
        userName = intent.getStringExtra("user_name") ?: ""

        initViews()
        setupNavigation()
        loadUsersFromAPI()
    }

    private fun initViews() {
        userListView = findViewById(R.id.userListView)
        searchView = findViewById(R.id.searchView)
        btnAddUser = findViewById(R.id.btnAddUser)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        toolbar = findViewById(R.id.toolbar)

        userList = ArrayList()
        filteredUserList = ArrayList()

        userAdapter = UserAdapter(userList)
        userListView.adapter = userAdapter

        // Arama işlevselliği
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText ?: "")
                return true
            }
        })

        // Kullanıcı ekleme butonu
        if (userType == "admin" || userType == "manager") {
            btnAddUser.visibility = View.VISIBLE
            btnAddUser.setOnClickListener {
                showAddUserDialog()
            }
        } else {
            btnAddUser.visibility = View.GONE
        }

        // Kullanıcı tıklama
        userListView.setOnItemClickListener { parent, view, position, id ->
            val user = filteredUserList[position]
            showUserActionsDialog(user)
        }
    }

    private fun setupNavigation() {
        setSupportActionBar(toolbar)

        // HAMBURGER İKONU
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation listener
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                }
                R.id.nav_profil -> {
                    finish()
                }
                R.id.nav_user_management -> {
                    drawerLayout.closeDrawer(navigationView)
                }
            }
            drawerLayout.closeDrawer(navigationView)
            true
        }

        // Header bilgilerini güncelle
        val headerView = navigationView.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName.text = userName
            tvUserEmail.text = userEmail
        }
    }

    // CUSTOM ADAPTER
    private inner class UserAdapter(private val users: ArrayList<User>) : BaseAdapter() {
        override fun getCount(): Int = users.size
        override fun getItem(position: Int): User = users[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_user_card, parent, false)
            val user = getItem(position)

            val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
            val tvUserApartment = view.findViewById<TextView>(R.id.tvUserApartment)
            val tvUserType = view.findViewById<TextView>(R.id.tvUserType)
            val tvResidentType = view.findViewById<TextView>(R.id.tvResidentType)
            val tvDuesResponsibility = view.findViewById<TextView>(R.id.tvDuesResponsibility)
            val tvResidingStatus = view.findViewById<TextView>(R.id.tvResidingStatus)

            // Temel bilgiler
            tvUserName.text = user.name
            tvUserEmail.text = user.email
            tvUserApartment.text = "${user.apartmentBlock}-${user.apartmentNumber}"

            // Kullanıcı tipi
            val userTypeText = when (user.userType) {
                "admin" -> "Yönetici"
                "manager" -> "Sorumlu"
                else -> "Sakin"
            }
            tvUserType.text = userTypeText

            // Badge renkleri
            val badgeColor = when (user.userType) {
                "admin" -> R.drawable.badge_admin
                "manager" -> R.drawable.badge_manager
                else -> R.drawable.badge_resident
            }
            tvUserType.setBackgroundResource(badgeColor)

            // Diğer bilgiler
            tvResidentType.text = if (user.residentType == "owner") "Ev Sahibi" else "Kiracı"
            tvDuesResponsibility.text = if (user.isResponsibleForDues == "yes") "Öder" else "Ödemez"
            tvResidingStatus.text = if (user.isResiding == "yes") "Oturuyor" else "Ayrıldı"

            return view
        }
    }

    private fun showAddUserDialog() {
        showEditUserDialog(null)
    }

    private fun showEditUserDialog(user: User?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etBlock = dialogView.findViewById<EditText>(R.id.etBlock)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)
        val spUserType = dialogView.findViewById<Spinner>(R.id.spUserType)
        val spResidentType = dialogView.findViewById<Spinner>(R.id.spResidentType)
        val spDuesResponsibility = dialogView.findViewById<Spinner>(R.id.spDuesResponsibility)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Başlık ayarla
        if (user == null) {
            tvDialogTitle.text = "👤 Yeni Kullanıcı Ekle"
            btnSave.text = "Ekle"
        } else {
            tvDialogTitle.text = "✏️ Kullanıcıyı Düzenle"
            btnSave.text = "Güncelle"

            // Mevcut değerleri doldur
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)
            etBlock.setText(user.apartmentBlock)
            etNumber.setText(user.apartmentNumber)
        }

        // Spinner adapter'ları
        ArrayAdapter.createFromResource(
            this,
            R.array.user_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spUserType.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.resident_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spResidentType.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.dues_responsibility,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spDuesResponsibility.adapter = adapter
        }

        // Spinner'ları mevcut değerlere göre ayarla
        if (user != null) {
            spUserType.setSelection(when (user.userType) {
                "admin" -> 2
                "manager" -> 1
                else -> 0
            })

            spResidentType.setSelection(if (user.residentType == "owner") 0 else 1)
            spDuesResponsibility.setSelection(if (user.isResponsibleForDues == "yes") 0 else 1)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Buton tıklamaları
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val block = etBlock.text.toString().trim()
            val number = etNumber.text.toString().trim()
            val userType = when (spUserType.selectedItemPosition) {
                0 -> "resident"
                1 -> "manager"
                else -> "admin"
            }
            val residentType = if (spResidentType.selectedItemPosition == 0) "owner" else "tenant"
            val duesResponsibility = if (spDuesResponsibility.selectedItemPosition == 0) "yes" else "no"

            if (name.isEmpty() || email.isEmpty() || block.isEmpty() || number.isEmpty()) {
                Toast.makeText(this, "Lütfen zorunlu alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                // Yeni kullanıcı ekle
                addUserToAPI(name, email, phone, block, number, userType, residentType, duesResponsibility)
            } else {
                // Kullanıcıyı güncelle
                updateUserInAPI(user.id, name, email, phone, block, number, userType, residentType, duesResponsibility)
            }

            dialog.dismiss()
        }

        // Dialog arkaplanını şeffaf yap
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showUserActionsDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_actions, null)

        // Kullanıcı bilgilerini doldur
        val tvDialogUserName = dialogView.findViewById<TextView>(R.id.tvDialogUserName)
        val tvDialogUserEmail = dialogView.findViewById<TextView>(R.id.tvDialogUserEmail)
        val tvDialogApartment = dialogView.findViewById<TextView>(R.id.tvDialogApartment)
        val tvDialogUserType = dialogView.findViewById<TextView>(R.id.tvDialogUserType)

        tvDialogUserName.text = user.name
        tvDialogUserEmail.text = user.email
        tvDialogApartment.text = "${user.apartmentBlock}-${user.apartmentNumber}"

        val userTypeText = when (user.userType) {
            "admin" -> "Yönetici"
            "manager" -> "Sorumlu"
            else -> "Sakin"
        }
        tvDialogUserType.text = userTypeText

        // Badge rengini ayarla
        val badgeColor = when (user.userType) {
            "admin" -> R.drawable.badge_admin
            "manager" -> R.drawable.badge_manager
            else -> R.drawable.badge_resident
        }
        tvDialogUserType.setBackgroundResource(badgeColor)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Buton tıklamaları
        dialogView.findViewById<LinearLayout>(R.id.btnDetails).setOnClickListener {
            dialog.dismiss()
            showUserDetails(user)
        }

        dialogView.findViewById<LinearLayout>(R.id.btnEdit).setOnClickListener {
            dialog.dismiss()
            showEditUserDialog(user)
        }

        dialogView.findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmation(user)
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        // Dialog arkaplanını şeffaf yap
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showUserDetails(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_details, null)

        // Bilgileri doldur
        val tvDetailName = dialogView.findViewById<TextView>(R.id.tvDetailName)
        val tvDetailEmail = dialogView.findViewById<TextView>(R.id.tvDetailEmail)
        val tvDetailPhone = dialogView.findViewById<TextView>(R.id.tvDetailPhone)
        val tvDetailApartment = dialogView.findViewById<TextView>(R.id.tvDetailApartment)
        val tvDetailUserType = dialogView.findViewById<TextView>(R.id.tvDetailUserType)
        val tvDetailResidentType = dialogView.findViewById<TextView>(R.id.tvDetailResidentType)
        val tvDetailDues = dialogView.findViewById<TextView>(R.id.tvDetailDues)
        val tvDetailResiding = dialogView.findViewById<TextView>(R.id.tvDetailResiding)

        tvDetailName.text = user.name
        tvDetailEmail.text = user.email
        tvDetailPhone.text = if (user.phone.isNotEmpty()) user.phone else "Belirtilmemiş"
        tvDetailApartment.text = "${user.apartmentBlock} Blok - ${user.apartmentNumber}"
        tvDetailUserType.text = when (user.userType) {
            "admin" -> "Yönetici"
            "manager" -> "Sorumlu"
            else -> "Sakin"
        }
        tvDetailResidentType.text = if (user.residentType == "owner") "Ev Sahibi" else "Kiracı"
        tvDetailDues.text = if (user.isResponsibleForDues == "yes") "Öder" else "Ödemez"
        tvDetailResiding.text = if (user.isResiding == "yes") "Oturuyor" else "Evi Boşalttı"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnOk).setOnClickListener {
            dialog.dismiss()
        }

        // Dialog arkaplanını şeffaf yap
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showDeleteConfirmation(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Kullanıcı Sil")
            .setMessage("${user.name} kullanıcısını silmek istediğinizden emin misiniz? Bu işlem geri alınamaz!")
            .setPositiveButton("Sil") { dialog, which ->
                deleteUserFromAPI(user.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun loadUsersFromAPI() {
        loadingIndicator.visibility = View.VISIBLE

        val sqlQuery = if (userType == "admin" || userType == "manager") {
            "SELECT id, name, email, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing FROM apartman_users ORDER BY apartment_block, apartment_number"
        } else {
            "SELECT id, name, email, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing FROM apartman_users WHERE email = '$userEmail'"
        }

        executeSQLQuery(sqlQuery) { result ->
            loadingIndicator.visibility = View.GONE
            parseUserData(result)
        }
    }

    private fun parseUserData(csvData: String) {
        userList.clear()

        val lines = csvData.trim().split("\n")
        if (lines.size <= 1) {
            showEmptyState()
            return
        }

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isNotEmpty()) {
                try {
                    val fields = parseCSVLine(line)
                    if (fields.size >= 10) {
                        val user = User(
                            id = fields[0],
                            name = fields[1],
                            email = fields[2],
                            phone = fields[3],
                            apartmentBlock = fields[4],
                            apartmentNumber = fields[5],
                            userType = fields[6],
                            residentType = fields[7],
                            isResponsibleForDues = fields[8],
                            isResiding = fields[9]
                        )
                        userList.add(user)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        filteredUserList.clear()
        filteredUserList.addAll(userList)
        updateUserList()
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = ArrayList<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            when {
                line[i] == '"' -> inQuotes = !inQuotes
                line[i] == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(line[i])
            }
        }
        result.add(current.toString())
        return result
    }

    private fun updateUserList() {
        userAdapter = UserAdapter(filteredUserList)
        userListView.adapter = userAdapter

        if (filteredUserList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun filterUsers(query: String) {
        filteredUserList.clear()

        if (query.isEmpty()) {
            filteredUserList.addAll(userList)
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            for (user in userList) {
                if (user.name.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    user.email.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    user.apartmentBlock.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                    user.apartmentNumber.lowercase(Locale.getDefault()).contains(lowerQuery)) {
                    filteredUserList.add(user)
                }
            }
        }

        updateUserList()
    }

    private fun addUserToAPI(name: String, email: String, phone: String, block: String, number: String, userType: String, residentType: String, duesResponsibility: String) {
        val password = "123456"
        val sqlQuery = "INSERT INTO apartman_users (name, email, password, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, receive_reminders) VALUES ('$name', '$email', '$password', '$phone', '$block', '$number', '$userType', '$residentType', '$duesResponsibility', 'yes', 'yes')"

        executeSQLQuery(sqlQuery) { result ->
            if (result.contains("AFFECTED ROWS: 1")) {
                Toast.makeText(this, "Kullanıcı başarıyla eklendi", Toast.LENGTH_SHORT).show()
                loadUsersFromAPI()
            } else {
                Toast.makeText(this, "Kullanıcı eklenemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserInAPI(userId: String, name: String, email: String, phone: String, block: String, number: String, userType: String, residentType: String, duesResponsibility: String) {
        val sqlQuery = "UPDATE apartman_users SET name='$name', email='$email', phone='$phone', apartment_block='$block', apartment_number='$number', user_type='$userType', resident_type='$residentType', is_responsible_for_dues='$duesResponsibility' WHERE id='$userId'"

        executeSQLQuery(sqlQuery) { result ->
            if (result.contains("AFFECTED ROWS: 1")) {
                Toast.makeText(this, "Kullanıcı başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                loadUsersFromAPI()
            } else {
                Toast.makeText(this, "Kullanıcı güncellenemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteUserFromAPI(userId: String) {
        val sqlQuery = "DELETE FROM apartman_users WHERE id = '$userId'"

        executeSQLQuery(sqlQuery) { result ->
            if (result.contains("AFFECTED ROWS: 1")) {
                Toast.makeText(this, "Kullanıcı başarıyla silindi", Toast.LENGTH_SHORT).show()
                loadUsersFromAPI()
            } else {
                Toast.makeText(this, "Kullanıcı silinemedi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeSQLQuery(query: String, callback: (String) -> Unit) {
        val apiUrl = "http://baser.org/apartman/api/api_hepsi.php"
        val SQLKEY = "randomkey"

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response ->
                callback(response)
            },
            { error ->
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this, "Sunucu hatası: ${error.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["query"] = query
                params["key"] = SQLKEY
                return params
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun showEmptyState() {
        tvEmptyState.visibility = View.VISIBLE
        userListView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        tvEmptyState.visibility = View.GONE
        userListView.visibility = View.VISIBLE
    }

    // User data class
    data class User(
        val id: String,
        val name: String,
        val email: String,
        val phone: String,
        val apartmentBlock: String,
        val apartmentNumber: String,
        val userType: String,
        val residentType: String,
        val isResponsibleForDues: String,
        val isResiding: String
    )
}