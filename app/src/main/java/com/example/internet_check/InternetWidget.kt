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

    private val sitesToCheck = listOf(
        "https://photomath.com/",
        "https://deepseek.com/en/",
        "https://www.mathway.com/ru/"
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

        // Устанавливаем серый фон
        views.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_gray)
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Проверка интернета
        thread {
            val hasInternet = checkInternet()

            Handler(Looper.getMainLooper()).post {
                val updatedViews = RemoteViews(context.packageName, R.layout.internet_widget)
                updatedViews.setOnClickPendingIntent(R.id.widget_layout, pendingUpdate)

                if (hasInternet) {
                    updatedViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_green)
                } else {
                    updatedViews.setInt(R.id.widget_layout, "setBackgroundResource", R.drawable.widget_red)
                }

                appWidgetManager.updateAppWidget(appWidgetId, updatedViews)
            }
        }
    }

    private fun checkInternet(): Boolean {
        for (site in sitesToCheck) {
            try {
                val url = URL(site)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.connect()
                val code = connection.responseCode
                connection.disconnect()

                if (code in 200..399) {
                    return true
                }
            } catch (e: Exception) {
                // Переходим к следующему сайту
            }
        }
        return false
    }
}