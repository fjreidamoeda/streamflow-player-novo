package com.streamflow.player

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun apiUrl(server: String, user: String, pass: String, action: String, extra: String = ""): String {
        val base = server.trimEnd('/')
        return "$base/player_api.php?username=$user&password=$pass&action=$action$extra"
    }

    suspend fun authenticate(server: String, username: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl(server, username, password, "")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Resposta vazia")
                val auth = gson.fromJson(body, AuthResponse::class.java)
                val info = auth.userInfo
                if (info != null && info.auth == 1)
                    Result.success(info)
                else
                    Result.failure(Exception(info?.message ?: "Credenciais invalidas"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getCategories(server: String, user: String, pass: String, type: String): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val action = when (type) {
                    "live" -> "get_live_categories"
                    "vod" -> "get_vod_categories"
                    "series" -> "get_series_categories"
                    else -> throw Exception("Tipo invalido")
                }
                val url = apiUrl(server, user, pass, action)
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Vazio")
                val listType = object : TypeToken<List<Category>>() {}.type
                Result.success(gson.fromJson(body, listType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getLiveStreams(server: String, user: String, pass: String, catId: String? = null): Result<List<LiveStream>> =
        withContext(Dispatchers.IO) {
            try {
                var url = apiUrl(server, user, pass, "get_live_streams")
                if (catId != null) url += "&category_id=$catId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Vazio")
                val listType = object : TypeToken<List<LiveStream>>() {}.type
                Result.success(gson.fromJson(body, listType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getVodStreams(server: String, user: String, pass: String, catId: String? = null): Result<List<VodStream>> =
        withContext(Dispatchers.IO) {
            try {
                var url = apiUrl(server, user, pass, "get_vod_streams")
                if (catId != null) url += "&category_id=$catId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Vazio")
                val listType = object : TypeToken<List<VodStream>>() {}.type
                Result.success(gson.fromJson(body, listType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSeries(server: String, user: String, pass: String, catId: String? = null): Result<List<SeriesItem>> =
        withContext(Dispatchers.IO) {
            try {
                var url = apiUrl(server, user, pass, "get_series")
                if (catId != null) url += "&category_id=$catId"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Vazio")
                val listType = object : TypeToken<List<SeriesItem>>() {}.type
                Result.success(gson.fromJson(body, listType))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSeriesEpisodes(server: String, user: String, pass: String, seriesId: String): Result<Map<String, List<EpisodeInfo>>> =
        withContext(Dispatchers.IO) {
            try {
                val url = apiUrl(server, user, pass, "get_series_info", "&series_id=$seriesId")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("Vazio")
                val detail = gson.fromJson(body, SeriesDetail::class.java)
                Result.success(detail.episodes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
