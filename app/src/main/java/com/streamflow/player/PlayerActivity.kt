package com.streamflow.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var api: ApiClient
    private var player: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isFullscreen = false

    private lateinit var playerView: PlayerView
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        prefs = getSharedPreferences("streamflow", Context.MODE_PRIVATE)
        api = ApiClient()

        playerView = findViewById(R.id.playerView)
        tvTitle = findViewById(R.id.tvPlayerTitle)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)

        val url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        val seriesId = intent.getStringExtra("series_id")
        val seriesName = intent.getStringExtra("series_name")

        tvTitle.text = title ?: seriesName ?: "Player"

        btnBack.setOnClickListener { finish() }
        playerView.setOnClickListener { toggleFullscreen() }

        if (seriesId != null) {
            loadSeriesEpisodes(seriesId, seriesName ?: "Serie")
        } else if (url != null) {
            playUrl(url)
        }
    }

    private fun loadSeriesEpisodes(seriesId: String, seriesName: String) {
        val server = prefs.getString("server", "") ?: ""
        val user = prefs.getString("username", "") ?: ""
        val pass = prefs.getString("password", "") ?: ""

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
            val result = api.getSeriesEpisodes(server, user, pass, seriesId)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                result.onSuccess { episodes ->
                    val allEps = mutableListOf<EpisodeInfo>()
                    for ((_, eps) in episodes) allEps.addAll(eps)
                    if (allEps.isNotEmpty()) {
                        val first = allEps[0]
                        val epUrl = if (first.streamUrl.isNotBlank()) first.streamUrl
                            else "${server.trimEnd('/')}/series/$user/$pass/${first.id}.${first.containerExtension}"
                        playUrl(epUrl)
                    }
                    Toast.makeText(this@PlayerActivity, "${allEps.size} episodios carregados", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(this@PlayerActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playUrl(url: String) {
        try {
            player?.release()
            playerView.player = null
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
            val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
            player = ExoPlayer.Builder(this).setMediaSourceFactory(mediaSourceFactory).build()
            playerView.player = player
            player?.setMediaItem(MediaItem.fromUri(url))
            player?.prepare()
            player?.play()
            player?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        runOnUiThread { progressBar.visibility = View.GONE }
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@PlayerActivity, "Erro: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Erro ao reproduzir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }
    override fun onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); player?.release() }
}
