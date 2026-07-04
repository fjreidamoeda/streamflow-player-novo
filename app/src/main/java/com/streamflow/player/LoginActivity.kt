package com.streamflow.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var api: ApiClient
    private lateinit var etServer: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("streamflow", Context.MODE_PRIVATE)
        api = ApiClient()

        etServer = findViewById(R.id.etServer)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        etServer.setText(prefs.getString("server", ""))
        etUser.setText(prefs.getString("username", ""))
        etPass.setText(prefs.getString("password", ""))

        if (prefs.getBoolean("logged", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        btnLogin.setOnClickListener {
            val server = etServer.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (server.isBlank() || user.isBlank() || pass.isBlank()) {
                tvError.text = "Preencha todos os campos"
                tvError.visibility = TextView.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = TextView.GONE
            progressBar.visibility = ProgressBar.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.text = "ENTRANDO..."

            CoroutineScope(Dispatchers.Main).launch {
                val result = api.authenticate(server, user, pass)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "ENTRAR"
                    result.onSuccess {
                        prefs.edit().putString("server", server)
                            .putString("username", user)
                            .putString("password", pass)
                            .putBoolean("logged", true)
                            .apply()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }.onFailure { e ->
                        tvError.text = e.message ?: "Erro de conexao"
                        tvError.visibility = TextView.VISIBLE
                    }
                }
            }
        }
    }
}
