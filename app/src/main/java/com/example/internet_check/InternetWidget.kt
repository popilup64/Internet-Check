package com.example.internet_check

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class InternetWidget : AppWidgetProvider() {

    // Сайты для проверки (могут быть заблокированы)
    private val sitesToCheck = listOf(
        "https://photomath.com/",
        "https://deepseek.com/en/",
        "https://www.mathway.com/ru/"
    )

    // Сайты из белого списка (работают в РФ)
    private val whiteListSites = listOf(
        "https://yandex.ru/",
        "https://vk.com/"
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == "UPDATE_WIDGET") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, InternetWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.internet_widget)

        // Intent для обновления по нажатию
        val updateIntent = Intent(context, InternetWidget::class.java)
        updateIntent.action = "UPDATE_WIDGET"
        val pendingUpdate = PendingIntent.getBroadcast(
            context, 0, updateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_layout, pendingUpdate)

        // Круглый серый фон
        views.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_gray)

        // Показываем три точки загрузки
        views.setViewVisibility(R.id.loading_dots, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.wifi_icon, android.view.View.GONE)
        views.setTextViewText(R.id.loading_dots, "● ● ●")

        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Запускаем анимацию точек
        startDotsAnimation(context, appWidgetManager, appWidgetId, pendingUpdate)

        // Проверка интернета и белых списков
        thread {
            val result = checkInternetAndWhiteList()

            Handler(Looper.getMainLooper()).post {
                val updatedViews = RemoteViews(context.packageName, R.layout.internet_widget)
                updatedViews.setOnClickPendingIntent(R.id.widget_layout, pendingUpdate)

                // Скрываем точки загрузки
                updatedViews.setViewVisibility(R.id.loading_dots, android.view.View.GONE)
                updatedViews.setViewVisibility(R.id.wifi_icon, android.view.View.VISIBLE)

                when (result) {
                    "WHITELIST_ON" -> {
                        // Белый список включен - оранжевый фон
                        updatedViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_orange)
                        updatedViews.setImageViewResource(R.id.wifi_icon, R.drawable.ic_wifi_wl)
                    }
                    "INTERNET_ON" -> {
                        // Интернет есть - зеленый фон
                        updatedViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_green)
                        updatedViews.setImageViewResource(R.id.wifi_icon, R.drawable.ic_wifi_on)
                    }
                    else -> {
                        // Нет интернета - красный фон
                        updatedViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_red)
                        updatedViews.setImageViewResource(R.id.wifi_icon, R.drawable.ic_wifi_off)
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
            }
        }
    }

    private fun startDotsAnimation(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        pendingUpdate: PendingIntent
    ) {
        val dots = listOf("● ○ ○", "○ ● ○", "○ ○ ●", "● ● ●")
        var index = 0

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val views = RemoteViews(context.packageName, R.layout.internet_widget)
                views.setOnClickPendingIntent(R.id.widget_layout, pendingUpdate)
                views.setTextViewText(R.id.loading_dots, dots[index % dots.size])
                appWidgetManager.updateAppWidget(appWidgetId, views)

                index++
                handler.postDelayed(this, 400)
            }
        }

        handler.post(runnable)

        handler.postDelayed({
            handler.removeCallbacks(runnable)
        }, 10000)
    }

    private fun checkInternetAndWhiteList(): String {
        // Проверяем белый список
        var whiteListAvailable = 0
        for (site in whiteListSites) {
            if (checkSite(site)) {
                whiteListAvailable++
            }
        }
        val whitePercent = (whiteListAvailable * 100 / whiteListSites.size)

        // Проверяем остальные сайты
        var otherSitesAvailable = 0
        for (site in sitesToCheck) {
            if (checkSite(site)) {
                otherSitesAvailable++
            }
        }
        val otherPercent = (otherSitesAvailable * 100 / sitesToCheck.size)

        // Анализ
        return when {
            whitePercent >= 80 && otherPercent <= 30 -> "WHITELIST_ON"
            whitePercent >= 80 && otherPercent >= 70 -> "INTERNET_ON"
            else -> "NO_INTERNET"
        }
    }

    private fun checkSite(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.connect()
            val code = connection.responseCode
            connection.disconnect()

            code in 200..399
        } catch (e: Exception) {
            false
        }
    }
}