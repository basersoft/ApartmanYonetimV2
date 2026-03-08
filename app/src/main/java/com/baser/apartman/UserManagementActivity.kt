package com.baser.apartman

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.android.material.navigation.NavigationView
import java.util.*
import kotlin.collections.ArrayList

class UserManagementActivity : BaseActivity() {

    private lateinit var userListView: ListView
    private lateinit var searchView: SearchView
    private lateinit var btnAddUser: Button
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private lateinit var userList: ArrayList<User>
    private lateinit var filteredUserList: ArrayList<User>
    private lateinit var userAdapter: BaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        initViews()
        setupCustomToolbar()
        setupCustomNavigation()
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

        userList = ArrayList()
        filteredUserList = ArrayList()

        userAdapter = object : BaseAdapter() {
            override fun getCount(): Int = filteredUserList.size
            override fun getItem(position: Int): User = filteredUserList[position]
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

                // Durum için arkaplan rengi
                when (user.status) {
                    "active" -> view.setBackgroundColor(resources.getColor(R.color.green_light, null))
                    "inactive" -> view.setBackgroundColor(resources.getColor(R.color.red_light, null))
                    "pending" -> view.setBackgroundColor(resources.getColor(R.color.yellow_light, null))
                }

                return view
            }
        }

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

    private fun setupCustomToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        if (toolbar == null) {
            println("❌ Toolbar bulunamadı")
            return
        }

        // Toolbar'ı ayarla
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Kullanıcı Yönetimi"

        // Hamburger ikonu için ActionBarDrawerToggle oluştur
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout != null) {
            val toggle = ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )

            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()

            // Hamburger ikonunu beyaz yap
            toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)
        }

        println("✅ UserManagementActivity: Toolbar ayarlandı")
    }

    private fun setupCustomNavigation() {
        // NavigationView'ı bul
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        if (navigationView == null) {
            println("❌ NavigationView bulunamadı")
            return
        }

        // BaseActivity'nin navigation listener'ını kullan
        navigationView.setNavigationItemSelectedListener(this)

        // Header'daki kullanıcı bilgilerini güncelle
        val headerView = navigationView.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            // Profil resmi view'larını bul
            ivProfileImage = it.findViewById(R.id.ivProfileImage)
            ivCameraIcon = it.findViewById(R.id.ivCameraIcon)
            btnChangeProfileImage = it.findViewById(R.id.btnChangeProfileImage)

            tvUserName.text = userName
            tvUserEmail.text = userEmail

            // ProfileImageManager ile profil resmini yükle
            ProfileImageManager.loadProfileImage(this, userEmail, ivProfileImage)

            // Tıklama dinleyicilerini ayarla
            setupProfileImageListeners()
        }

        println("✅ UserManagementActivity: Navigation ayarlandı")
    }

    // BaseActivity'deki setupNavigation'ı override et ve özel yapma
    override fun setupNavigation() {
        // BaseActivity'nin setupNavigation'ını çağırma
        // Çünkü burada özel toolbar ayarı yapıyoruz
    }

    private fun showAddUserDialog() {
        showEditUserDialog(null)
    }

    private fun showEditUserDialog(user: User?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_user, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)
        val tvPasswordLabel = dialogView.findViewById<TextView>(R.id.tvPasswordLabel)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etBlock = dialogView.findViewById<EditText>(R.id.etBlock)
        val etNumber = dialogView.findViewById<EditText>(R.id.etNumber)
        val spUserType = dialogView.findViewById<Spinner>(R.id.spUserType)
        val spResidentType = dialogView.findViewById<Spinner>(R.id.spResidentType)
        val spDuesResponsibility = dialogView.findViewById<Spinner>(R.id.spDuesResponsibility)
        val spStatus = dialogView.findViewById<Spinner>(R.id.spStatus)
        val tvStatusLabel = dialogView.findViewById<TextView>(R.id.tvStatusLabel)
        val spReceiveReminders = dialogView.findViewById<Spinner>(R.id.spReceiveReminders)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Başlık ayarla
        if (user == null) {
            tvDialogTitle.text = "👤 Yeni Kullanıcı Ekle"
            btnSave.text = "Ekle"
            // Yeni kullanıcıda şifre zorunlu
            tvPasswordLabel.text = "🔑 Şifre *"
            etPassword.hint = "Şifre giriniz"
        } else {
            tvDialogTitle.text = "✏️ Kullanıcıyı Düzenle"
            btnSave.text = "Güncelle"
            // Mevcut kullanıcıda şifre opsiyonel
            tvPasswordLabel.text = "🔑 Şifre (Değiştirmek için girin)"
            etPassword.hint = "Değişmeyecekse boş bırakın"
        }

        // Durum spinner'ını sadece admin ve manager görsün
        if (userType == "admin" || userType == "manager") {
            spStatus.visibility = View.VISIBLE
            tvStatusLabel.visibility = View.VISIBLE
        } else {
            spStatus.visibility = View.GONE
            tvStatusLabel.visibility = View.GONE
        }

        // Spinner adapter'ları - strings.xml'den al
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

        ArrayAdapter.createFromResource(
            this,
            R.array.status_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spStatus.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.receive_reminders,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spReceiveReminders.adapter = adapter
        }

        // Mevcut değerleri doldur
        if (user != null) {
            etName.setText(user.name)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)
            etBlock.setText(user.apartmentBlock)
            etNumber.setText(user.apartmentNumber)

            // Spinner'ları mevcut değerlere göre ayarla
            // user_type: "resident", "manager", "admin"
            spUserType.setSelection(when (user.userType) {
                "admin" -> 2      // 3. sıra: Yönetici
                "manager" -> 1    // 2. sıra: Sorumlu
                else -> 0         // 1. sıra: Sakin
            })

            // resident_type: "owner", "tenant"
            spResidentType.setSelection(if (user.residentType == "owner") 0 else 1)

            // is_responsible_for_dues: "yes", "no"
            spDuesResponsibility.setSelection(if (user.isResponsibleForDues == "yes") 0 else 1)

            // status: "active", "inactive", "pending"
            spStatus.setSelection(when (user.status) {
                "active" -> 0
                "inactive" -> 1
                else -> 2 // pending
            })

            // receive_reminders: "yes", "no"
            spReceiveReminders.setSelection(if (user.receiveReminders == "yes") 0 else 1)
        } else {
            // Yeni kullanıcı için varsayılan değerler
            spUserType.setSelection(0) // Sakin
            spResidentType.setSelection(0) // Ev Sahibi
            spDuesResponsibility.setSelection(0) // Öder
            spStatus.setSelection(0) // Aktif
            spReceiveReminders.setSelection(0) // Alır
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
            val password = etPassword.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val block = etBlock.text.toString().trim().uppercase()
            val number = etNumber.text.toString().trim()

            // user_type mapping
            val userTypeValue = when (spUserType.selectedItemPosition) {
                0 -> "resident"  // Sakin
                1 -> "manager"   // Sorumlu
                else -> "admin"  // Yönetici
            }

            // resident_type mapping
            val residentType = if (spResidentType.selectedItemPosition == 0) "owner" else "tenant"

            // dues_responsibility mapping
            val duesResponsibility = if (spDuesResponsibility.selectedItemPosition == 0) "yes" else "no"

            // status mapping
            val status = when (spStatus.selectedItemPosition) {
                0 -> "active"
                1 -> "inactive"
                else -> "pending"
            }

            // receive_reminders mapping
            val receiveReminders = if (spReceiveReminders.selectedItemPosition == 0) "yes" else "no"

            // Validasyon
            if (name.isEmpty() || email.isEmpty() || block.isEmpty() || number.isEmpty()) {
                Toast.makeText(this, "Lütfen zorunlu alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null && password.isEmpty()) {
                Toast.makeText(this, "Yeni kullanıcı için şifre gerekli", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user == null) {
                // Yeni kullanıcı ekle - Şifreyi hash'le
                val hashedPassword = hashPassword(password)
                addUserToAPI(name, email, hashedPassword, phone, block, number, userTypeValue, residentType, duesResponsibility, status, receiveReminders)
            } else {
                // Kullanıcıyı güncelle
                val hashedPassword = if (password.isNotEmpty()) hashPassword(password) else ""
                updateUserInAPI(user.id, name, email, hashedPassword, phone, block, number, userTypeValue, residentType, duesResponsibility, status, receiveReminders)
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

        // Durum kontrol butonları - Sadece admin ve manager görebilir
        if (userType == "admin" || userType == "manager") {
            // Buton container'ını bul
            val buttonsContainer = dialogView.findViewById<LinearLayout>(R.id.buttonsContainer)

            if (buttonsContainer != null) {
                // Durum butonlarını temizle
                for (i in buttonsContainer.childCount - 1 downTo 0) {
                    val child = buttonsContainer.getChildAt(i)
                    if (child is Button) {
                        buttonsContainer.removeView(child)
                    }
                }

                // Aktif/pasif butonları
                if (user.status == "pending") {
                    val btnActivate = Button(this).apply {
                        text = "Aktifleştir"
                        setBackgroundColor(resources.getColor(R.color.green, null))
                        setTextColor(resources.getColor(android.R.color.white, null))
                        setOnClickListener {
                            dialog.dismiss()
                            activateUser(user)
                        }
                    }
                    buttonsContainer.addView(btnActivate)
                }

                if (user.status == "active") {
                    val btnDeactivate = Button(this).apply {
                        text = "Pasifleştir"
                        setBackgroundColor(resources.getColor(R.color.red, null))
                        setTextColor(resources.getColor(android.R.color.white, null))
                        setOnClickListener {
                            dialog.dismiss()
                            deactivateUser(user)
                        }
                    }
                    buttonsContainer.addView(btnDeactivate)
                }

                if (user.status == "inactive") {
                    val btnActivate = Button(this).apply {
                        text = "Aktifleştir"
                        setBackgroundColor(resources.getColor(R.color.green, null))
                        setTextColor(resources.getColor(android.R.color.white, null))
                        setOnClickListener {
                            dialog.dismiss()
                            activateUser(user)
                        }
                    }
                    buttonsContainer.addView(btnActivate)
                }
            }
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
        val tvDetailStatus = dialogView.findViewById<TextView>(R.id.tvDetailStatus)
        val tvDetailReminders = dialogView.findViewById<TextView>(R.id.tvDetailReminders)

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
        tvDetailStatus.text = when (user.status) {
            "active" -> "Aktif"
            "inactive" -> "Pasif"
            "pending" -> "Beklemede"
            else -> user.status
        }
        tvDetailReminders.text = if (user.receiveReminders == "yes") "Alır" else "Almaz"

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

    private fun activateUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Kullanıcıyı Aktifleştir")
            .setMessage("${user.name} kullanıcısını aktifleştirmek istediğinizden emin misiniz?")
            .setPositiveButton("Aktifleştir") { dialog, which ->
                activateUserInAPI(user.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deactivateUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Kullanıcıyı Pasifleştir")
            .setMessage("${user.name} kullanıcısını pasifleştirmek istediğinizden emin misiniz?")
            .setPositiveButton("Pasifleştir") { dialog, which ->
                deactivateUserInAPI(user.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun loadUsersFromAPI() {
        loadingIndicator.visibility = View.VISIBLE

        val sqlQuery = if (userType == "admin" || userType == "manager") {
            "SELECT id, name, email, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, status, receive_reminders FROM apartman_users ORDER BY apartment_block, apartment_number"
        } else {
            "SELECT id, name, email, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, status, receive_reminders FROM apartman_users WHERE email = '$userEmail'"
        }

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                loadingIndicator.visibility = View.GONE
                parseUserData(result)
            },
            onError = { error ->
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
        )
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
                    if (fields.size >= 11) {
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
                            isResiding = fields[9],
                            status = if (fields.size > 10) fields[10] else "active",
                            receiveReminders = if (fields.size > 11) fields[11] else "yes"
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
        userAdapter.notifyDataSetChanged()

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

    private fun addUserToAPI(name: String, email: String, hashedPassword: String, phone: String, block: String, number: String, userType: String, residentType: String, duesResponsibility: String, status: String, receiveReminders: String) {
        // Şifre zaten hash'lenmiş olarak geliyor
        val sqlQuery = "INSERT INTO apartman_users (name, email, password, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, receive_reminders, status) VALUES ('$name', '$email', '$hashedPassword', '$phone', '$block', '$number', '$userType', '$residentType', '$duesResponsibility', 'yes', '$receiveReminders', '$status')"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kullanıcı başarıyla eklendi", Toast.LENGTH_SHORT).show()
                    loadUsersFromAPI()
                } else {
                    Toast.makeText(this, "Kullanıcı eklenemedi: $result", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateUserInAPI(userId: String, name: String, email: String, hashedPassword: String, phone: String, block: String, number: String, userType: String, residentType: String, duesResponsibility: String, status: String, receiveReminders: String) {
        // Şifre güncellenecek mi kontrol et
        val passwordUpdate = if (hashedPassword.isNotEmpty()) {
            // Şifre değiştirilecek - zaten hash'li
            "password='$hashedPassword', "
        } else {
            ""
        }

        val sqlQuery = "UPDATE apartman_users SET name='$name', email='$email', $passwordUpdate phone='$phone', apartment_block='$block', apartment_number='$number', user_type='$userType', resident_type='$residentType', is_responsible_for_dues='$duesResponsibility', status='$status', receive_reminders='$receiveReminders' WHERE id='$userId'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kullanıcı başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                    loadUsersFromAPI()
                } else {
                    Toast.makeText(this, "Kullanıcı güncellenemedi: $result", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun deleteUserFromAPI(userId: String) {
        val sqlQuery = "DELETE FROM apartman_users WHERE id = '$userId'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kullanıcı başarıyla silindi", Toast.LENGTH_SHORT).show()
                    loadUsersFromAPI()
                } else {
                    Toast.makeText(this, "Kullanıcı silinemedi: $result", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun activateUserInAPI(userId: String) {
        val sqlQuery = "UPDATE apartman_users SET status='active' WHERE id='$userId'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kullanıcı başarıyla aktifleştirildi", Toast.LENGTH_SHORT).show()
                    loadUsersFromAPI()
                } else {
                    Toast.makeText(this, "Kullanıcı aktifleştirilemedi: $result", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun deactivateUserInAPI(userId: String) {
        val sqlQuery = "UPDATE apartman_users SET status='inactive' WHERE id='$userId'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kullanıcı başarıyla pasifleştirildi", Toast.LENGTH_SHORT).show()
                    loadUsersFromAPI()
                } else {
                    Toast.makeText(this, "Kullanıcı pasifleştirilemedi: $result", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showEmptyState() {
        tvEmptyState.visibility = View.VISIBLE
        userListView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        tvEmptyState.visibility = View.GONE
        userListView.visibility = View.VISIBLE
    }

    // BCrypt ile şifre hash'leme fonksiyonu
    private fun hashPassword(password: String): String {
        // BCrypt ile şifreyi hash'le
        // $2a$12$ cost factor: 12 (2^12 iterations)
        val bcryptHashString = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        return bcryptHashString
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
        val isResiding: String,
        val status: String = "active",
        val receiveReminders: String = "yes"
    )
}