package com.example.internet_check

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var text: TextView
    private lateinit var mainLayout: androidx.constraintlayout.widget.ConstraintLayout

    // Сайты, которые должны быть в белом списке (разрешены в РФ)
    private val whiteListSites = listOf(
        "https://yandex.ru/",
        "https://mail.ru/",
        "https://dzen.ru/",
        "https://gosuslugi.ru/"
    )

    // Сайты, которые НЕ в белом списке (потенциально заблокированы)
    private val nonWhiteListSites = listOf(
        "https://photomath.com/",
        "https://deepseek.com/en/",
        "https://www.mathway.com/ru/",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.button)
        text = findViewById(R.id.text)
        mainLayout = findViewById(R.id.main)

        button.setOnClickListener {
            text.text = "Анализ фильтрации трафика...\n"
            Thread {
                checkWhiteListFilter()
            }.start()
        }
    }

    private fun checkWhiteListFilter() {
        val results = StringBuilder()

        // 1. Проверяем доступность "белых" сайтов
        results.append("🔍 ПРОВЕРКА БЕЛОГО СПИСКА (ТСПУ)\n")
        results.append("=".repeat(40) + "\n\n")

        results.append("📋 Сайты из белого списка (должны быть доступны):\n")
        var whiteListAvailable = 0

        for (site in whiteListSites) {
            val available = checkSite(site)
            if (available) whiteListAvailable++
            val status = if (available) "✅ ДОСТУПЕН" else "❌ НЕДОСТУПЕН"
            results.append("   $status | $site\n")
        }

        results.append("\n📋 Сайты НЕ из белого списка (могут быть заблокированы):\n")
        var nonWhiteListAvailable = 0

        for (site in nonWhiteListSites) {
            val available = checkSite(site)
            if (available) nonWhiteListAvailable++
            val status = if (available) "✅ ДОСТУПЕН" else "❌ ЗАБЛОКИРОВАН"
            results.append("   $status | $site\n")
        }

        // 2. Анализ результатов
        results.append("\n" + "=".repeat(40) + "\n")
        results.append("📊 РЕЗУЛЬТАТЫ АНАЛИЗА:\n\n")

        val whitePercent = (whiteListAvailable * 100 / whiteListSites.size)
        val nonWhitePercent = (nonWhiteListAvailable * 100 / nonWhiteListSites.size)

        results.append("Белый список: $whiteListAvailable/${whiteListSites.size} доступно ($whitePercent%)\n")
        results.append("Остальные сайты: $nonWhiteListAvailable/${nonWhiteListSites.size} доступно ($nonWhitePercent%)\n\n")

        // 3. Определяем статус фильтрации
        val filterStatus = when {
            whitePercent >= 80 && nonWhitePercent <= 30 -> {
                "🔴 БЕЛЫЙ СПИСОК ВКЛЮЧЕН!\nФильтрация трафика активна.\nБольшинство сайтов не из белого списка заблокированы."
            }
            whitePercent >= 80 && nonWhitePercent >= 70 -> {
                "🟡 БЕЛЫЙ СПИСОК НЕ АКТИВЕН\nФильтрация не обнаружена.\nДоступны почти все сайты."
            }
            whitePercent < 50 -> {
                "⚠️ ПРОБЛЕМЫ С ИНТЕРНЕТОМ\nМногие сайты недоступны. Проверьте подключение."
            }
            else -> {
                "🟠 ЧАСТИЧНАЯ ФИЛЬТРАЦИЯ\nНекоторые сайты из белого списка недоступны, некоторые заблокированы."
            }
        }

        results.append("$filterStatus\n")

        runOnUiThread {
            text.text = results.toString()

            // Меняем цвет фона в зависимости от статуса
            when {
                filterStatus.contains("ВКЛЮЧЕН") -> {
                    mainLayout.setBackgroundColor(Color.parseColor("#FF9800"))  // Оранжевый - фильтрация есть
                    button.setBackgroundColor(Color.parseColor("#E65100"))
                }
                filterStatus.contains("НЕ АКТИВЕН") -> {
                    mainLayout.setBackgroundColor(Color.parseColor("#4CAF50"))  // Зеленый - фильтрации нет
                    button.setBackgroundColor(Color.parseColor("#2E7D32"))
                }
                else -> {
                    mainLayout.setBackgroundColor(Color.parseColor("#F44336"))  // Красный - проблемы
                    button.setBackgroundColor(Color.parseColor("#C62828"))
                }
            }

            text.setTextColor(Color.WHITE)
            button.setTextColor(Color.WHITE)

            Toast.makeText(this, filterStatus.take(50), Toast.LENGTH_LONG).show()
        }
    }

    private fun checkSite(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true

            connection.connect()
            val code = connection.responseCode
            connection.disconnect()

            code in 200..399
        } catch (e: Exception) {
            false
        }
    }
}