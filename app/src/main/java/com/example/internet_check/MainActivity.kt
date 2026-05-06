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

    // Сайты для проверки (могут быть заблокированы) - ТЕ ЖЕ, ЧТО В ВИДЖЕТЕ
    private val sitesToCheck = listOf(
        "https://photomath.com/",
        "https://deepseek.com/en/",
        "https://www.mathway.com/ru/"
    )

    // Сайты из белого списка (работают в РФ) - ТЕ ЖЕ, ЧТО В ВИДЖЕТЕ
    private val whiteListSites = listOf(
        "https://yandex.ru/",
        "https://vk.com/"
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

            text.text = "Проверка..."
            button.isEnabled = false
            Thread {
                checkInternetAndWhiteList()
            }.start()
        }
    }

    private fun checkInternetAndWhiteList() {
        // Проверяем сайты из белого списка
        var whiteListAvailable = 0
        for (site in whiteListSites) {
            val result = checkSite(site)
            android.util.Log.d("MainActivity", "Белый сайт $site - $result")
            if (result) {
                whiteListAvailable++
            }
        }
        val whitePercent = (whiteListAvailable * 100 / whiteListSites.size)

        // Проверяем остальные сайты
        var otherSitesAvailable = 0
        for (site in sitesToCheck) {
            val result = checkSite(site)
            android.util.Log.d("MainActivity", "Проверяемый сайт $site - $result")
            if (result) {
                otherSitesAvailable++
            }
        }
        val otherPercent = (otherSitesAvailable * 100 / sitesToCheck.size)

        // Логирование для отладки
        android.util.Log.d("MainActivity", "whitePercent = $whitePercent, otherPercent = $otherPercent")

        runOnUiThread {
            button.isEnabled = true

            // Анализ как в виджете
            when {
                whitePercent >= 80 && otherPercent <= 30 -> {
                    text.text = "❌"
                    text.textSize = 50f
                    mainLayout.setBackgroundColor(Color.parseColor("#FF9800")) // Оранжевый
                    button.setBackgroundColor(Color.parseColor("#E65100"))
                    Toast.makeText(this, "Белый список ВКЛЮЧЕН", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("MainActivity", "Результат: БЕЛЫЙ СПИСОК ВКЛЮЧЕН")
                }
                whitePercent >= 80 && otherPercent >= 70 -> {
                    text.text = "✅"
                    text.textSize = 50f
                    mainLayout.setBackgroundColor(Color.parseColor("#4CAF50")) // Зеленый
                    button.setBackgroundColor(Color.parseColor("#2E7D32"))
                    Toast.makeText(this, "Интернет есть", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("MainActivity", "Результат: ИНТЕРНЕТ ЕСТЬ")
                }
                else -> {
                    text.text = "❌"
                    text.textSize = 50f
                    mainLayout.setBackgroundColor(Color.parseColor("#F44336")) // Красный
                    button.setBackgroundColor(Color.parseColor("#C62828"))
                    Toast.makeText(this, "Нет интернета", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("MainActivity", "Результат: НЕТ ИНТЕРНЕТА (whitePercent=$whitePercent, otherPercent=$otherPercent)")
                }
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