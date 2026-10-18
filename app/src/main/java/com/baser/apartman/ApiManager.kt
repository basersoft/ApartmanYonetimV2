package com.baser.apartman

import android.content.Context
import android.content.SharedPreferences
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.net.URLEncoder

object ApiManager {

    private const val PREFS_NAME = "api_settings"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_PATH = "api_path"
    private const val KEY_SQL_KEY = "sql_key"
    private const val KEY_BACKUP_URL = "backup_url"
    private const val KEY_USE_BACKUP = "use_backup"

    private const val DEFAULT_BASE_URL = "http://baser.org"
    private const val DEFAULT_API_PATH = "/site/api/api_hepsi.php"
    private const val DEFAULT_SQL_KEY = "randomkey"
    private const val DEFAULT_BACKUP_URL = "http://www.baser.org"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_BASE_URL)) {
            setBaseUrl(DEFAULT_BASE_URL)
            setApiPath(DEFAULT_API_PATH)
            setSqlKey(DEFAULT_SQL_KEY)
            setBackupUrl(DEFAULT_BACKUP_URL)
            setUseBackup(false)
        }
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!
    fun getApiPath(): String = prefs.getString(KEY_API_PATH, DEFAULT_API_PATH)!!
    fun getSqlKey(): String = prefs.getString(KEY_SQL_KEY, DEFAULT_SQL_KEY)!!
    fun getBackupUrl(): String = prefs.getString(KEY_BACKUP_URL, DEFAULT_BACKUP_URL)!!
    fun isUsingBackup(): Boolean = prefs.getBoolean(KEY_USE_BACKUP, false)

    fun setBaseUrl(url: String) = prefs.edit().putString(KEY_BASE_URL, url).apply()
    fun setApiPath(path: String) = prefs.edit().putString(KEY_API_PATH, path).apply()
    fun setSqlKey(key: String) = prefs.edit().putString(KEY_SQL_KEY, key).apply()
    fun setBackupUrl(url: String) = prefs.edit().putString(KEY_BACKUP_URL, url).apply()
    fun setUseBackup(useBackup: Boolean) = prefs.edit().putBoolean(KEY_USE_BACKUP, useBackup).apply()

    fun getFullApiUrl(): String {
        val baseUrl = if (isUsingBackup()) getBackupUrl() else getBaseUrl()
        return baseUrl + getApiPath()
    }

    // MEVCUT executeSQLQuery FONKSİYONUNU KULLAN
    fun executeSQLQuery(
        context: Context,
        query: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = { error -> }
    ) {
        val apiUrl = getFullApiUrl()
        val sqlKey = getSqlKey()

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response -> onSuccess(response) },
            { error ->
                if (!isUsingBackup()) {
                    setUseBackup(true)
                    executeSQLQuery(context, query, onSuccess, onError)
                } else {
                    onError("Sunucu hatası: ${error.message ?: "Bilinmeyen hata"}")
                }
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["query"] = query
                params["key"] = sqlKey
                return params
            }

            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }
        }

        Volley.newRequestQueue(context).add(stringRequest)
    }

    // MESAJ FONKSİYONLARI - YENİ EKLENEN

    /**
     * Mesajları getir (CSV formatında)
     */
    fun getMessages(
        context: Context,
        userId: Int,
        siteId: Int = 1,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val query = """
            SELECT m.*, u.name as sender_name, u2.name as receiver_name,
                   CASE WHEN m.sender_id = $userId THEN 1 ELSE 0 END as is_own_message,
                   CASE WHEN m.parent_id IS NOT NULL THEN 1 ELSE 0 END as is_reply,
                   CASE WHEN m.subject LIKE '%Borç%' OR m.subject LIKE '%Aidat%' THEN 1 ELSE 0 END as is_reminder,
                   CASE WHEN m.sender_id != $userId AND m.parent_id IS NULL AND 
                        NOT (m.subject LIKE '%Borç%' OR m.subject LIKE '%Aidat%') THEN 1 ELSE 0 END as can_reply
            FROM apartman_messages m 
            LEFT JOIN apartman_users u ON m.sender_id = u.id 
            LEFT JOIN apartman_users u2 ON m.receiver_id = u2.id 
            WHERE m.site_id = $siteId 
            AND (m.sender_id = $userId OR m.receiver_id = $userId)
            ORDER BY m.created_at DESC
            LIMIT 100
        """.trimIndent()

        executeSQLQuery(context, query, onSuccess, onError)
    }

    /**
     * Yeni mesaj gönder
     */
    fun sendMessage(
        context: Context,
        senderId: Int,
        receiverId: Int,
        subject: String,
        message: String,
        siteId: Int = 1,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val encodedSubject = URLEncoder.encode(subject, "UTF-8")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        val query = """
            INSERT INTO apartman_messages (site_id, sender_id, receiver_id, subject, message, status) 
            VALUES ($siteId, $senderId, $receiverId, '$encodedSubject', '$encodedMessage', 'unread')
        """.trimIndent()

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = {
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Mesaj cevapla
     */
    fun sendReply(
        context: Context,
        originalMessageId: Int,
        senderId: Int,
        subject: String,
        message: String,
        receiverId: Int,
        siteId: Int = 1,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val encodedSubject = URLEncoder.encode(subject, "UTF-8")
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        val query = """
            INSERT INTO apartman_messages (site_id, sender_id, receiver_id, subject, message, status, parent_id) 
            VALUES ($siteId, $senderId, $receiverId, '$encodedSubject', '$encodedMessage', 'unread', $originalMessageId)
        """.trimIndent()

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = {
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Mesajı okundu olarak işaretle
     */
    fun markMessageAsRead(
        context: Context,
        messageId: Int,
        userId: Int,
        siteId: Int = 1,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val query = """
            UPDATE apartman_messages 
            SET status = 'read' 
            WHERE id = $messageId 
            AND receiver_id = $userId 
            AND site_id = $siteId
        """.trimIndent()

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = {
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Mesaj sil
     */
    fun deleteMessage(
        context: Context,
        messageId: Int,
        userId: Int,
        isAdmin: Boolean,
        siteId: Int = 1,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val query = if (isAdmin) {
            "DELETE FROM apartman_messages WHERE id = $messageId AND site_id = $siteId"
        } else {
            "DELETE FROM apartman_messages WHERE id = $messageId AND sender_id = $userId AND site_id = $siteId"
        }

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = {
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Mesaj istatistiklerini getir
     */
    fun getMessageStats(
        context: Context,
        userId: Int,
        userType: String,
        siteId: Int = 1,
        onSuccess: (MessageStats) -> Unit,
        onError: (String) -> Unit
    ) {
        val isAdmin = userType == "admin"

        val query = if (isAdmin) {
            """
                SELECT 
                    (SELECT COUNT(*) FROM apartman_messages WHERE site_id = $siteId) as total,
                    (SELECT COUNT(*) FROM apartman_messages WHERE status = 'unread' AND receiver_id = $userId AND site_id = $siteId) as unread,
                    (SELECT COUNT(*) FROM apartman_messages WHERE sender_id = $userId AND site_id = $siteId) as sent,
                    (SELECT COUNT(*) FROM apartman_messages WHERE status = 'answered' AND sender_id != $userId AND receiver_id = $userId AND site_id = $siteId) as answered
            """.trimIndent()
        } else {
            """
                SELECT 
                    (SELECT COUNT(*) FROM apartman_messages WHERE (sender_id = $userId OR receiver_id = $userId) AND site_id = $siteId) as total,
                    (SELECT COUNT(*) FROM apartman_messages WHERE receiver_id = $userId AND status = 'unread' AND site_id = $siteId) as unread,
                    (SELECT COUNT(*) FROM apartman_messages WHERE sender_id = $userId AND site_id = $siteId) as sent,
                    (SELECT COUNT(*) FROM apartman_messages WHERE status = 'answered' AND sender_id = $userId AND site_id = $siteId) as answered
            """.trimIndent()
        }

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = { result ->
                try {
                    // CSV formatını parse et
                    val lines = result.trim().split("\n")
                    if (lines.size > 1) {
                        val fields = parseCSVLine(lines[1])
                        if (fields.size >= 4) {
                            val stats = MessageStats(
                                totalMessages = fields[0].toIntOrNull() ?: 0,
                                unreadMessages = fields[1].toIntOrNull() ?: 0,
                                sentMessages = fields[2].toIntOrNull() ?: 0,
                                answeredMessages = fields[3].toIntOrNull() ?: 0
                            )
                            onSuccess(stats)
                        } else {
                            onError("Geçersiz istatistik verisi")
                        }
                    } else {
                        onSuccess(MessageStats(0, 0, 0, 0))
                    }
                } catch (e: Exception) {
                    onError("İstatistikler parse edilemedi: ${e.message}")
                }
            },
            onError = onError
        )
    }

    /**
     * Kullanıcı listesi getir (yeni mesaj için)
     */
    fun getUsersForNewMessage(
        context: Context,
        currentUserId: Int,
        userType: String,
        siteId: Int = 1,
        onSuccess: (List<Map<String, String>>) -> Unit,
        onError: (String) -> Unit
    ) {
        val query = if (userType == "admin") {
            """
                SELECT id, name, apartment_block, apartment_number, user_type 
                FROM apartman_users 
                WHERE is_residing = 'yes' 
                AND id != $currentUserId 
                AND site_id = $siteId
                ORDER BY user_type DESC, name ASC
            """.trimIndent()
        } else {
            """
                SELECT id, name, apartment_block, apartment_number, user_type 
                FROM apartman_users 
                WHERE is_residing = 'yes' 
                AND id != $currentUserId 
                AND site_id = $siteId
                AND (user_type = 'admin' OR user_type = 'resident')
                ORDER BY user_type DESC, name ASC
            """.trimIndent()
        }

        executeSQLQuery(
            context = context,
            query = query,
            onSuccess = { result ->
                try {
                    val users = parseUsersFromCSV(result)
                    onSuccess(users)
                } catch (e: Exception) {
                    onError("Kullanıcı listesi parse edilemedi: ${e.message}")
                }
            },
            onError = onError
        )
    }

    // YARDIMCI FONKSİYONLAR

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

    private fun parseUsersFromCSV(csvData: String): List<Map<String, String>> {
        val users = mutableListOf<Map<String, String>>()
        val lines = csvData.trim().split("\n")

        if (lines.size > 1) {
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isNotEmpty()) {
                    val fields = parseCSVLine(line)
                    if (fields.size >= 5) {
                        val user = mapOf(
                            "id" to fields[0],
                            "name" to fields[1],
                            "apartment_block" to fields[2],
                            "apartment_number" to fields[3],
                            "user_type" to fields[4]
                        )
                        users.add(user)
                    }
                }
            }
        }

        return users
    }

    // JSON TABANLI MESAJ FONKSİYONLARI (Alternatif)

    /**
     * JSON formatında mesajları getir
     */
    fun getMessagesJson(
        context: Context,
        userId: Int,
        siteId: Int = 1,
        onSuccess: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ) {
        getMessages(context, userId, siteId,
            onSuccess = { csvData ->
                try {
                    val messages = parseMessagesFromCSV(csvData)
                    onSuccess(messages)
                } catch (e: Exception) {
                    onError("Mesajlar parse edilemedi: ${e.message}")
                }
            },
            onError = onError
        )
    }

    private fun parseMessagesFromCSV(csvData: String): List<Message> {
        val messages = mutableListOf<Message>()
        val lines = csvData.trim().split("\n")

        if (lines.size > 1) {
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isNotEmpty()) {
                    val fields = parseCSVLine(line)

                    if (fields.size >= 16) {
                        val message = Message(
                            id = fields[0].toIntOrNull() ?: 0,
                            siteId = fields[1].toIntOrNull() ?: 1,
                            senderId = fields[2].toIntOrNull() ?: 0,
                            receiverId = fields[3].toIntOrNull() ?: 0,
                            senderName = fields[4],
                            receiverName = fields[5],
                            subject = fields[6],
                            message = fields[7],
                            status = fields[8],
                            parentId = fields[9].toIntOrNull(),
                            createdAt = fields[10],
                            updatedAt = fields[11],
                            isOwnMessage = fields[12].toBoolean(),
                            isReply = fields[13].toBoolean(),
                            isReminder = fields[14].toBoolean(),
                            canReply = fields[15].toBoolean()
                        )
                        messages.add(message)
                    }
                }
            }
        }

        return messages
    }

    // MEVCUT FONKSİYONLAR (Aynı kalacak)

    // TAM ÇALIŞAN ŞİFRE SIFIRLAMA FONKSİYONU
    fun resetPassword(
        context: Context,
        email: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiUrl = getFullApiUrl()
        val sqlKey = getSqlKey()

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response ->
                when {
                    response.startsWith("SUCCESS:") -> {
                        val successMessage = response.substringAfter("SUCCESS:")
                        onSuccess(successMessage)
                    }
                    response.startsWith("ERROR:") -> {
                        val errorMessage = response.substringAfter("ERROR:")
                        onError(errorMessage)
                    }
                    else -> {
                        onError("Beklenmeyen yanıt: $response")
                    }
                }
            },
            { error ->
                if (error.networkResponse?.statusCode == 400) {
                    val errorBody = error.networkResponse?.data?.toString(Charsets.UTF_8) ?: "No error details"
                    onError("Sunucu hatası (400): $errorBody")
                } else {
                    onError("Sunucu bağlantı hatası: ${error.message ?: "Bilinmeyen hata"}")
                }
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["action"] = "reset_password"
                params["email"] = email
                params["key"] = sqlKey
                return params
            }

            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                headers["Accept"] = "text/plain"
                return headers
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(context).add(stringRequest)
    }

    fun testConnection(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val testQuery = "SELECT 1 as test"
        executeSQLQuery(context, testQuery, { onSuccess() }, onError)
    }

    fun resetToDefaults() {
        setBaseUrl(DEFAULT_BASE_URL)
        setApiPath(DEFAULT_API_PATH)
        setSqlKey(DEFAULT_SQL_KEY)
        setBackupUrl(DEFAULT_BACKUP_URL)
        setUseBackup(false)
    }

    fun getAllSettings(): Map<String, String> {
        return mapOf(
            "base_url" to getBaseUrl(),
            "api_path" to getApiPath(),
            "sql_key" to getSqlKey(),
            "backup_url" to getBackupUrl(),
            "use_backup" to isUsingBackup().toString(),
            "current_url" to getFullApiUrl()
        )
    }

    fun executeSQLQuerySync(
        context: Context,
        query: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        executeSQLQuery(context, query, onSuccess, onError)
    }
}

// Data class'ları aynı pakete ekleyin


