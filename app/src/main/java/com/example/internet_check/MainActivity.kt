package com.example.internet_check

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var text: TextView
    private lateinit var mainLayout: androidx.constraintlayout.widget.ConstraintLayout

    private val websites = listOf(
        "https://yandex.ru/",
        "https://google.com/",
        "https://vk.com/",
        "https://dzen.ru/",
        "https://mail.ru/"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById<Button>(R.id.button)
        text = findViewById<TextView>(R.id.text)
        mainLayout = findViewById(R.id.main)

        button.setBackgroundColor(Color.parseColor("#2196F3"))

        button.setOnClickListener {
            Thread {
                checkAllSites()
            }.start()
        }
    }
    private fun checkAllSites(){
        val results = StringBuilder()
        var hasInternet = false
        for (site in websites) {
            val result = checkSite(site)
            results.append(result).append("\n\n")

            if (result == "Internet connect") {
                hasInternet = true
            }

            runOnUiThread {
                text.text = results.toString()
                changeBackgroundColor(hasInternet)
            }
        }
    }

    private fun checkSite(urlString: String): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.requestMethod = "HEAD"

            val start = System.currentTimeMillis()
            connection.connect()
            val code = connection.responseCode
            val time = System.currentTimeMillis() - start

            connection.disconnect()

            if (code == 200) {
                runOnUiThread {
                    Toast.makeText(this, "✅ $urlString - ДОСТУПЕН", Toast.LENGTH_SHORT).show()
                }
                "Internet connect"
            } else {
                runOnUiThread {
                    Toast.makeText(this, "НЕДОСТУПЕН", Toast.LENGTH_SHORT).show()
                }
                "Non Internet"
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "НЕДОСТУПЕН", Toast.LENGTH_SHORT).show()
            }
            "Non Internet"
        }
    }
    private fun changeBackgroundColor(hasInternet: Boolean) {
        if (hasInternet) {
            // Интернет есть - зеленый фон
            mainLayout.setBackgroundColor(Color.parseColor("#4CAF50"))  // Зеленый
            text.setTextColor(Color.WHITE)
            button.setTextColor(Color.BLACK)
        } else {
            // Интернета нет - красный фон
            mainLayout.setBackgroundColor(Color.parseColor("#F44336"))  // Красный
            text.setTextColor(Color.WHITE)
            button.setTextColor(Color.BLACK)
        }
    }
}
