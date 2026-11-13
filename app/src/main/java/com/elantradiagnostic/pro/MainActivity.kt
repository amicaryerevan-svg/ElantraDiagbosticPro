package com.elantradiagnostic.pro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val bg = Color.rgb(12, 18, 29)
    private val blue = Color.rgb(35, 135, 245)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 40, 24, 24)
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        scroll.addView(layout)

        val title = TextView(this).apply {
            text = "ELANTRA DIAGNOSTIC PRO"
            textSize = 25f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 12)
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "HYUNDAI ELANTRA • OBD2"
            textSize = 14f
            setTextColor(blue)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        layout.addView(subtitle)

        addButton(layout, "🔍  Диагностика автомобиля") {
            showInfo("Диагностика OBD2",
                "Подключите совместимый OBD2 Bluetooth-адаптер для чтения ошибок.")
        }

        addButton(layout, "⚙️  Ошибки двигателя") {
            showInfo("Ошибки двигателя",
                "Здесь будут отображаться коды неисправностей DTC после подключения.")
        }

        addButton(layout, "📊  Датчики двигателя") {
            showInfo("Датчики двигателя",
                "Здесь будут отображаться обороты, температура, скорость и другие параметры.")
        }

        addButton(layout, "🔌  Подключение OBD2") {
            showInfo("Подключение",
                "Подключение к OBD2-адаптеру будет реализовано следующим этапом.")
        }

        addButton(layout, "ℹ️  О приложении") {
            showInfo("Elantra Diagnostic Pro",
                "Версия 1.0\nДиагностическое приложение Hyundai Elantra.")
        }

        setContentView(scroll)
    }

    private fun addButton(
        layout: LinearLayout,
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(white)
            setBackgroundColor(Color.rgb(29, 48, 72))
            isAllCaps = false
            setPadding(12, 18, 12, 18)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)
        layout.addView(button, params)
    }

    private fun showInfo(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Назад") { _, _ -> }
            .show()
    }
}
