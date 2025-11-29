package com.baser.apartman

import android.content.Context
import android.content.SharedPreferences
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

object ApiManager {

    private const val PREFS_NAME = "api_settings"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_API_PATH = "api_path"
    private const val KEY_SQL_KEY = "sql_key"
    private const val KEY_BACKUP_URL = "backup_url"
    private const val KEY_USE_BACKUP = "use_backup"

    // Varsayılan değerler
    private const val DEFAULT_BASE_URL = "http://baser.org"
    private const val DEFAULT_API_PATH = "/apartman/api/api_hepsi.php"
    private const val DEFAULT_SQL_KEY = "randomkey"
    private const val DEFAULT_BACKUP_URL = "http://backup.baser.org"

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // İlk kurulumda varsayılan değerleri kaydet
        if (!prefs.contains(KEY_BASE_URL)) {
            setBaseUrl(DEFAULT_BASE_URL)
            setApiPath(DEFAULT_API_PATH)
            setSqlKey(DEFAULT_SQL_KEY)
            setBackupUrl(DEFAULT_BACKUP_URL)
            setUseBackup(false)
        }
    }

    // GET metodları
    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL)!!
    fun getApiPath(): String = prefs.getString(KEY_API_PATH, DEFAULT_API_PATH)!!
    fun getSqlKey(): String = prefs.getString(KEY_SQL_KEY, DEFAULT_SQL_KEY)!!
    fun getBackupUrl(): String = prefs.getString(KEY_BACKUP_URL, DEFAULT_BACKUP_URL)!!
    fun isUsingBackup(): Boolean = prefs.getBoolean(KEY_USE_BACKUP, false)

    // SET metodları
    fun setBaseUrl(url: String) = prefs.edit().putString(KEY_BASE_URL, url).apply()
    fun setApiPath(path: String) = prefs.edit().putString(KEY_API_PATH, path).apply()
    fun setSqlKey(key: String) = prefs.edit().putString(KEY_SQL_KEY, key).apply()
    fun setBackupUrl(url: String) = prefs.edit().putString(KEY_BACKUP_URL, url).apply()
    fun setUseBackup(useBackup: Boolean) = prefs.edit().putBoolean(KEY_USE_BACKUP, useBackup).apply()

    // Tam API URL'sini alma
    fun getFullApiUrl(): String {
        val baseUrl = if (isUsingBackup()) getBackupUrl() else getBaseUrl()
        return baseUrl + getApiPath()
    }

    // API isteği gönderme
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
            { response ->
                onSuccess(response)
            },
            { error ->
                // Ana sunucu hata verirse yedek sunucuya geç
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

    // API ayarlarını test etme
    fun testConnection(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val testQuery = "SELECT 1 as test"

        executeSQLQuery(
            context = context,
            query = testQuery,
            onSuccess = {
                onSuccess()
            },
            onError = { error ->
                onError(error)
            }
        )
    }

    // Ayarları sıfırlama
    fun resetToDefaults() {
        setBaseUrl(DEFAULT_BASE_URL)
        setApiPath(DEFAULT_API_PATH)
        setSqlKey(DEFAULT_SQL_KEY)
        setBackupUrl(DEFAULT_BACKUP_URL)
        setUseBackup(false)
    }

    // Tüm ayarları alma (Ayarlar sayfası için)
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
}