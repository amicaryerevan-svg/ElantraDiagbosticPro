
package com.elantradiagnostic.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(10, 16, 28)
    private val blue = Color.rgb(35, 135, 245)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 35, 24, 30)
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        scroll.addView(layout)

        val title = TextView(this).apply {
            text = "ELANTRA"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(white)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "DIAGNOSTIC PRO"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(blue)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(subtitle)

        val vehicle = TextView(this).apply {
            text = "HYUNDAI ELANTRA 2021 SEL\n2.0L • OBD2"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(white)
            setPadding(15, 25, 15, 25)
            setBackgroundColor(Color.rgb(25, 38, 57))
        }
        layout.addView(vehicle)

        addButton(layout, "🔵  Подключение Bluetooth") {
            openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        }

        addButton(layout, "📶  Подключение Wi-Fi") {
            openSettings(Settings.ACTION_WIFI_SETTINGS)
        }

        addButton(layout, "🔍  Диагностика автомобиля") {
            showInfo(
                "Диагностика",
                "Для чтения реальных данных двигателя необходимо совместимое диагностическое соединение OBD2."
            )
        }

        addButton(layout, "⚙️  Ошибки двигателя") {
            showInfo(
                "Ошибки двигателя",
                "Коды неисправностей DTC появятся после подключения к диагностическому интерфейсу автомобиля."
            )
        }

        addButton(layout, "📊  Датчики двигателя") {
            showInfo(
                "Датчики двигателя",
                "Здесь будут отображаться обороты, температура охлаждающей жидкости, скорость и другие параметры после подключения."
            )
        }

        addButton(layout, "📋  История диагностики") {
            showInfo(
                "История",
                "Сохранённые результаты диагностики будут доступны здесь."
            )
        }

        addButton(layout, "ℹ️  О приложении") {
            showInfo(
                "Elantra Diagnostic Pro",
                "Версия 2.0\nHyundai Elantra 2021 SEL"
            )
        }

        val footer = TextView(this).apply {
            text = "PROFESSIONAL VEHICLE DIAGNOSTICS"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, 25, 0, 10)
        }
        layout.addView(footer)

        setContentView(scroll)
    }

    private fun addButton(
        layout: LinearLayout,
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(white)
            setBackgroundColor(Color.rgb(29, 48, 72))
            isAllCaps = false
            setPadding(12, 17, 12, 17)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 9, 0, 9)
        layout.addView(button, params)
    }

    private fun openSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (e: Exception) {
            showInfo("Ошибка", "Не удалось открыть настройки подключения.")
        }
    }

    private fun showInfo(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Назад", null)
            .show()
    }
}
