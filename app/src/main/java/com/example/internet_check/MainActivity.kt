package com.example.internet_check

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var text: TextView
    private lateinit var mainLayout: androidx.constraintlayout.widget.ConstraintLayout

    private var lastClickTime = 0L
    private val CLICK_DELAY = 3000L

    // Сайты для проверки
    private val sitesToCheck = listOf(
        "https://photomath.com/",
        "https://deepseek.com/en/",
        "https://www.mathway.com/ru/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        text = findViewById(R.id.text)
        mainLayout = findViewById(R.id.main)
        button.setBackgroundColor(Color.parseColor("#2196F3"))

        button.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()

            text.text = "TEST..."
            button.isEnabled = false
            Thread {
                checkInternet()
            }.start()
        }
    }

    private fun checkInternet() {
        // Проверяем первый сайт
        val firstSiteAvailable = checkSite(sitesToCheck[0])

        if (!firstSiteAvailable) {
            // Если первый сайт недоступен - сразу показываем результат
            runOnUiThread {
                button.isEnabled = true
                text.text = "❌"
                text.textSize = 50f
                mainLayout.setBackgroundColor(Color.parseColor("#F44336"))
                button.setBackgroundColor(Color.parseColor("#C62828"))
                text.setTextColor(Color.WHITE)
                button.setTextColor(Color.WHITE)
                Toast.makeText(this, "Интернета нет", Toast.LENGTH_SHORT).show()

                button.postDelayed({
                    resetToDefault()
                }, 3000)
            }
            return
        }

        // Если первый сайт доступен - проверяем остальные (опционально)
        var hasInternet = true

        for (site in sitesToCheck) {
            if (checkSite(site)) {
                hasInternet = true
                break
            }
        }

        runOnUiThread {
            button.isEnabled = true

            if (hasInternet) {
                text.text = "✅"
                text.textSize = 50f
                mainLayout.setBackgroundColor(Color.parseColor("#4CAF50"))
                button.setBackgroundColor(Color.parseColor("#2E7D32"))
                Toast.makeText(this, "Интернет есть", Toast.LENGTH_SHORT).show()
            }

            text.setTextColor(Color.WHITE)
            button.setTextColor(Color.WHITE)

            button.postDelayed({
                resetToDefault()
            }, 3000)
        }
    }

    private fun checkSite(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = true

            connection.connect()
            val code = connection.responseCode
            connection.disconnect()

            code in 200..399
        } catch (e: SocketTimeoutException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun resetToDefault() {
        mainLayout.setBackgroundColor(Color.parseColor("#E0E0E0"))
        button.setBackgroundColor(Color.parseColor("#2196F3"))
        text.textSize = 30f
        button.text = "CHECK"
        text.text = "Check Internet"
        text.setTextColor(Color.BLACK)
        button.setTextColor(Color.WHITE)
        button.isEnabled = true
    }
}