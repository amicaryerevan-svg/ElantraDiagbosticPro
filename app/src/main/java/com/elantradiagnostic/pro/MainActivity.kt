package com.elantradiagnostic.pro

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 4102
    }

    private val bg = Color.rgb(10, 16, 28)
    private val blue = Color.rgb(35, 135, 245)
    private val white = Color.WHITE

    private data class Symptom(val title: String, val causes: String, val checks: String, val urgency: String)

    private val symptoms = listOf(
        Symptom("Двигатель не запускается",
            "Возможные причины: разряженный аккумулятор, плохие клеммы, неисправность стартера, топливного насоса или системы зажигания.",
            "Проверьте, включаются ли приборы; нет ли коррозии на клеммах; слышен ли щелчок при запуске. Не пытайтесь долго крутить стартер.",
            "Если стартер не крутит или есть запах топлива/дыма — прекратите попытки запуска и обратитесь в сервис."),
        Symptom("Двигатель трясётся или работает неровно",
            "Возможные причины: свечи зажигания, катушки, подсос воздуха, загрязнение дросселя или неисправность топливной системы.",
            "Отметьте, появляется ли вибрация на холодном двигателе, на холостом ходу или при ускорении. Проверьте, не мигает ли Check Engine.",
            "Если Check Engine мигает или двигатель сильно трясётся — остановитесь в безопасном месте. Продолжение движения может повредить катализатор."),
        Symptom("Потеря мощности / плохой разгон",
            "Возможные причины: ограничение подачи топлива или воздуха, неисправность зажигания, датчиков двигателя либо трансмиссии.",
            "Отметьте, возникает ли проблема постоянно или только при разгоне, есть ли предупреждения на панели и изменились ли обороты двигателя.",
            "Если автомобиль не может безопасно поддерживать скорость, избегайте движения в потоке и организуйте диагностику."),
        Symptom("Двигатель перегревается",
            "Возможные причины: низкий уровень охлаждающей жидкости, утечка, неисправность вентилятора, термостата или водяного насоса.",
            "Остановитесь безопасно и выключите двигатель. Дайте ему остыть. Не открывайте крышку радиатора или бачка на горячем двигателе.",
            "Высокая температура — срочная ситуация. Не продолжайте движение при перегреве; вызовите помощь."),
        Symptom("Повышенный расход топлива",
            "Возможные причины: низкое давление в шинах, короткие поездки, неисправность датчиков, свечей или топливной системы.",
            "Проверьте давление в шинах на холодных колёсах, нагрузку автомобиля и фактический расход за несколько заправок.",
            "Если одновременно появились запах топлива, утечка или сильная потеря мощности — остановитесь и обратитесь в сервис."),
        Symptom("Рывки или толчки при переключении / разгоне",
            "Возможные причины: особенности работы IVT/CVT, состояние трансмиссионной жидкости, программное управление или проблемы двигателя.",
            "Запишите скорость, температуру автомобиля и условия появления рывков. Не делайте резких ускорений и не пытайтесь самостоятельно менять жидкость без правильной спецификации.",
            "Сильные удары, пробуксовка, предупреждение трансмиссии или потеря тяги требуют скорой профессиональной диагностики. Для проверки IVT может понадобиться Hyundai-совместимый сканер."),
        Symptom("Горит Check Engine",
            "Причина может быть от неплотно закрытой крышки топливного бака до неисправности системы зажигания, датчиков или контроля выбросов.",
            "Проверьте, плотно ли закрыта крышка топливного бака. Запишите, горит индикатор постоянно или мигает. Точный код ошибки без сканера определить нельзя.",
            "Если индикатор мигает, двигатель работает неровно или потерял мощность — остановитесь безопасно и не продолжайте поездку."),
        Symptom("Посторонний шум или стук",
            "Возможные причины: ремень или ролики, выхлопная система, подвеска, тормоза либо внутренние детали двигателя.",
            "Определите, откуда слышен звук и зависит ли он от оборотов, скорости, торможения или поворота. Не залезайте под автомобиль без надёжных опор.",
            "Громкий металлический стук двигателя, скрежет тормозов или резкое усиление шума — повод прекратить движение и вызвать помощь.")
    )

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

        addButton(layout, "🩺  Диагностика без OBD2") { showSymptomMenu() }

        addButton(layout, "🎵  Музыка телефона — Android Auto") {
            requestLocalAudioAccess()
        }

        addButton(layout, "▶  Открыть YouTube Music") {
            openYouTubeMusic()
        }

        addButton(layout, "🔵  Подключение Bluetooth (OBD2)") {
            showInfo("Подключение OBD2", "Для чтения реальных данных нужен совместимый адаптер ELM327 Bluetooth и поддержка нужных протоколов. Сейчас адаптер не подключён.")
        }

        addButton(layout, "📶  Подключение Wi-Fi") {
            showInfo("OBD2 Wi-Fi", "Для этого режима нужен совместимый Wi-Fi OBD2 адаптер. Подключение к Wi-Fi само по себе не считывает данные автомобиля.")
        }

        addButton(layout, "⚙️  Ошибки двигателя") {
            showInfo("Ошибки двигателя", "Реальные коды DTC можно прочитать только после подключения совместимого OBD2-сканера. Диагностика по симптомам не заменяет чтение кодов.")
        }

        addButton(layout, "📊  Датчики двигателя") {
            showInfo("Датчики двигателя", "Обороты, температура охлаждающей жидкости, скорость и другие параметры появятся после подключения совместимого OBD2-адаптера.")
        }

        addButton(layout, "📋  История диагностики") {
            showInfo("История", "История пока не сохраняется. Результаты этой версии диагностики можно записать вручную.")
        }

        addButton(layout, "ℹ️  О приложении") {
            showInfo("Elantra Diagnostic Pro", "Версия 1.2\nHyundai Elantra 2021 SEL 2.0L\n\nРежим без OBD2 помогает оценить возможные причины симптомов, но не считывает данные ЭБУ.")
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

    private fun openYouTubeMusic() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            showInfo(
                "YouTube Music",
                "Официальное приложение YouTube Music не найдено на телефоне. Установи его из Google Play, войди в аккаунт и подключи телефон к Android Auto. В движении используй только аудиорежим и голосовое управление."
            )
            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.youtube.music"))
            try {
                startActivity(storeIntent)
            } catch (_: Exception) {
                // No app store or browser available.
            }
        }
    }

    private fun requestLocalAudioAccess() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            showInfo(
                "Android Auto — музыка",
                "Доступ к аудиофайлам уже разрешён. Подключи телефон к Android Auto и открой Elantra Diagnostic Pro в списке медиаприложений. В библиотеке появятся поддерживаемые музыкальные файлы, сохранённые на телефоне."
            )
        } else {
            requestPermissions(arrayOf(permission), REQUEST_AUDIO_PERMISSION)
        }
    }

    @Deprecated("Permission callback retained for compatibility with the current Activity implementation")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showInfo(
                    "Доступ разрешён",
                    "Теперь подключи телефон к Android Auto. Открой список медиаприложений и выбери Elantra Diagnostic Pro. Если музыка не появилась, проверь, что на телефоне есть локальные аудиофайлы."
                )
            } else {
                showInfo(
                    "Нужен доступ к музыке",
                    "Без разрешения Android не позволит приложению показать локальные аудиофайлы в Android Auto. Разрешение можно включить в настройках приложения."
                )
            }
        }
    }

    private fun showSymptomMenu() {
        val names = symptoms.map { it.title }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle("Что происходит с автомобилем?")
            .setItems(names) { _, which -> showSymptomDetails(symptoms[which]) }
            .setNegativeButton("Назад", null)
            .show()
    }

    private fun showSymptomDetails(symptom: Symptom) {
        val message = "ВОЗМОЖНЫЕ ПРИЧИНЫ\n" + symptom.causes +
            "\n\nЧТО МОЖНО ПРОВЕРИТЬ\n" + symptom.checks +
            "\n\nВАЖНОСТЬ\n" + symptom.urgency +
            "\n\nЭто предварительная справка, а не точный диагноз. Для подтверждения причины нужна проверка автомобиля."
        android.app.AlertDialog.Builder(this)
            .setTitle(symptom.title)
            .setMessage(message)
            .setPositiveButton("Понятно", null)
            .setNeutralButton("Другой симптом") { _, _ -> showSymptomMenu() }
            .show()
    }

    private fun addButton(layout: LinearLayout, text: String, action: () -> Unit) {
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

    private fun showInfo(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Назад", null)
            .show()
    }
}