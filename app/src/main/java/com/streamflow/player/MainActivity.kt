package com.streamflow.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var api: ApiClient
    private lateinit var rvCategories: RecyclerView
    private lateinit var rvContent: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var btnLive: Button
    private lateinit var btnMovies: Button
    private lateinit var btnSeries: Button
    private lateinit var btnLogout: Button
    private lateinit var etSearch: EditText

    private var server = ""
    private var username = ""
    private var password = ""
    private var currentType = "live"
    private var categories = listOf<Category>()
    private var contentItems = listOf<Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("streamflow", Context.MODE_PRIVATE)
        api = ApiClient()

        server = prefs.getString("server", "") ?: ""
        username = prefs.getString("username", "") ?: ""
        password = prefs.getString("password", "") ?: ""

        if (server.isBlank()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        rvCategories = findViewById(R.id.rvCategories)
        rvContent = findViewById(R.id.rvContent)
        progressBar = findViewById(R.id.progressBar)
        tvTitle = findViewById(R.id.tvTitle)
        btnLive = findViewById(R.id.btnLive)
        btnMovies = findViewById(R.id.btnMovies)
        btnSeries = findViewById(R.id.btnSeries)
        btnLogout = findViewById(R.id.btnLogout)
        etSearch = findViewById(R.id.etSearch)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvContent.layoutManager = LinearLayoutManager(this)

        btnLive.setOnClickListener { switchTab("live") }
        btnMovies.setOnClickListener { switchTab("vod") }
        btnSeries.setOnClickListener { switchTab("series") }

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterContent(s?.toString()?.lowercase() ?: "")
            }
        })

        switchTab("live")
    }

    private fun switchTab(type: String) {
        currentType = type
        val active = 0xffe63e2e.toInt()
        val inactive = 0xff333333.toInt()
        btnLive.setBackgroundColor(if (type == "live") active else inactive)
        btnMovies.setBackgroundColor(if (type == "vod") active else inactive)
        btnSeries.setBackgroundColor(if (type == "series") active else inactive)
        loadCategories()
    }

    private fun loadCategories() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = api.getCategories(server, username, password, currentType)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { cats ->
                    categories = cats
                    rvCategories.adapter = CategoryAdapter(cats) { cat ->
                        loadContent(cat.categoryId)
                    }
                }.onFailure { e ->
                    Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadContent(categoryId: String?) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = when (currentType) {
                "live" -> api.getLiveStreams(server, username, password, categoryId)
                "vod" -> api.getVodStreams(server, username, password, categoryId)
                "series" -> api.getSeries(server, username, password, categoryId)
                else -> Result.failure(Exception("Invalido"))
            }
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { items ->
                    contentItems = when (items) {
                        is List<*> -> items.filterNotNull()
                        else -> emptyList()
                    }
                    rvContent.adapter = ContentAdapter(contentItems) { item -> onItemClick(item) }
                }.onFailure { e ->
                    Toast.makeText(this@MainActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onItemClick(item: Any) {
        when (item) {
            is LiveStream -> {
                val url = "${server.trimEnd('/')}/live/$username/$password/${item.streamId}.ts"
                startPlayer(url, item.name, item.streamIcon)
            }
            is VodStream -> {
                val url = "${server.trimEnd('/')}/movie/$username/$password/${item.streamId}.${item.containerExtension}"
                startPlayer(url, item.name, item.streamIcon)
            }
            is SeriesItem -> {
                startActivity(Intent(this, PlayerActivity::class.java).apply {
                    putExtra("series_id", item.seriesId)
                    putExtra("series_name", item.name)
                })
            }
        }
    }

    private fun startPlayer(url: String, title: String, icon: String) {
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("url", url)
            putExtra("title", title)
            putExtra("icon", icon)
        })
    }

    private fun filterContent(query: String) {
        val adapter = rvContent.adapter as? ContentAdapter ?: return
        adapter.filter(query)
    }
}
