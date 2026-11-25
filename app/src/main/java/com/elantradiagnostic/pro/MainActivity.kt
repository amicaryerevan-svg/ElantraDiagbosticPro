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
        private const val REQUEST_OBD2_PERMISSION = 4106
        private const val REQUEST_OBD2_EXPORT = 4107
        private const val PENDING_OBD2_EXPORT_STATE = "pending_obd2_export_text"
        private const val OBD2_HISTORY_KEY = "obd2_history_v1"
    }

    private var bg = Color.rgb(10, 16, 28)
    private val blue = Color.rgb(35, 135, 245)
    private val white = Color.WHITE
    private var obd2Client: Obd2BluetoothClient? = null
    private var pendingObd2ExportText: String? = null

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
        bg = getSharedPreferences("elantra_data", MODE_PRIVATE).getInt("theme_bg", Color.rgb(10, 16, 28))
        pendingObd2ExportText = savedInstanceState?.getString(PENDING_OBD2_EXPORT_STATE)
        showHome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PENDING_OBD2_EXPORT_STATE, pendingObd2ExportText)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        obd2Client?.close()
        obd2Client = null
        super.onDestroy()
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

        addButton(layout, "▶  Открыть YouTube") {
            openYouTube()
        }

        addButton(layout, "🔌  Подключить ELM327 Bluetooth") {
            startObd2Bluetooth()
        }

        addButton(layout, "📶  Подключение Wi-Fi") {
            showInfo("OBD2 Wi-Fi", "Для этого режима нужен совместимый Wi-Fi OBD2 адаптер. Подключение к Wi-Fi само по себе не считывает данные автомобиля.")
        }

        addButton(layout, "⚙️  Ошибки двигателя") {
            showInfo("Ошибки двигателя", "При подключении ELM327 приложение выполняет чтение стандартных сохранённых кодов DTC через OBD-II Mode 03. Для этого выбери «Подключить ELM327 Bluetooth». Mode 03 не читает все блоки ABS, SRS, кузова и трансмиссии; коды не стираются.")
        }

        addButton(layout, "📊  Датчики двигателя") {
            showInfo("Датчики двигателя", "При диагностическом снимке приложение расшифровывает карту поддерживаемых PID 01–20 и один раз запрашивает RPM (010C) и температуру охлаждающей жидкости (0105), если ЭБУ объявил их поддержку. Это снимок при подключении, а не непрерывная телеметрия; скорость пока не запрашивается.")
        }

        addButton(layout, "📋  История диагностики") {
            showObd2History()
        }

        addButton(layout, "🚘  GPS-панель и скорость") { showGpsPanel() }
        addButton(layout, "🅿️  Сохранить место парковки") { saveParkingLocation() }
        addButton(layout, "🧭  Найти автомобиль") { findParkedCar() }
        addButton(layout, "🛠️  Обслуживание и ТО") { showMaintenance() }
        addButton(layout, "⛽  Журнал заправок и расход") { showFuelLog() }
        addButton(layout, "🎬  Открыть видео на телефоне") { openLocalVideo() }
        addButton(layout, "📡  Bluetooth и Wi-Fi") { showConnectionStatus() }
        addButton(layout, "🎙️  Голосовой помощник") { startVoiceAssistant() }
        addButton(layout, "📝  Журнал заметок и поездок") { showTripNotes() }
        addButton(layout, "🎨  Оформление Sport / Neon") { showThemePicker() }

        addButton(layout, "ℹ️  О приложении") {
            showInfo("Elantra Diagnostic Pro", "Версия 1.14\nHyundai Elantra 2021 SEL 2.0L\n\nРежим без OBD2 помогает оценить возможные причины симптомов, но не считывает данные ЭБУ.")
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

    private fun openYouTube() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Обычный YouTube")
            .setMessage(
                "Откроется официальное приложение YouTube на телефоне. Используй видео только на парковке. " +
                    "Обычный Android Auto не передаёт произвольное видео YouTube на штатный экран, " +
                    "а это приложение не снимает блокировки видеопоказа во время движения."
            )
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Открыть YouTube") { _, _ ->
                val launchIntent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (launchIntent != null) {
                    try {
                        startActivity(launchIntent)
                    } catch (_: Exception) {
                        showInfo("YouTube", "Не удалось открыть приложение YouTube.")
                    }
                } else {
                    showInfo(
                        "YouTube не установлен",
                        "Установи официальное приложение YouTube из Google Play."
                    )
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.youtube")
                            )
                        )
                    } catch (_: Exception) {
                        // No app store or browser available.
                    }
                }
            }
            .show()
    }


    private fun prefs() = getSharedPreferences("elantra_data", MODE_PRIVATE)

    private fun saveObd2History(entry: Obd2DiagnosticHistoryEntry) {
        val previous = Obd2DiagnosticHistoryCodec.decode(
            prefs().getString(OBD2_HISTORY_KEY, "").orEmpty()
        )
        val updated = (listOf(entry) + previous).take(Obd2DiagnosticHistoryCodec.MAX_ENTRIES)
        prefs().edit()
            .putString(OBD2_HISTORY_KEY, Obd2DiagnosticHistoryCodec.encode(updated))
            .apply()
    }

    private fun showObd2History() {
        val entries = Obd2DiagnosticHistoryCodec.decode(
            prefs().getString(OBD2_HISTORY_KEY, "").orEmpty()
        )
        if (entries.isEmpty()) {
            showInfo("История диагностики", "Сохранённых сеансов пока нет. Выполни проверку через сопряжённый ELM327 Bluetooth.")
            return
        }

        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        val labels = entries.map { entry ->
            "${entry.status.displayLabel} · ${formatter.format(java.util.Date(entry.timestampMillis))} — ${entry.adapterName}"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Последние ${entries.size} диагностических сеансов")
            .setItems(labels) { _, which ->
                val entry = entries[which]
                val date = formatter.format(java.util.Date(entry.timestampMillis))
                android.app.AlertDialog.Builder(this)
                    .setTitle("${entry.status.displayLabel} · $date")
                    .setMessage("Статус: ${entry.status.displayLabel}\nАдаптер: ${entry.adapterName}\n\n${entry.summary}")
                    .setPositiveButton("Сохранить TXT") { _, _ ->
                        exportObd2Report(entry)
                    }
                    .setNeutralButton("Поделиться") { _, _ ->
                        shareObd2Report(entry)
                    }
                    .setNegativeButton("Закрыть", null)
                    .show()
            }
            .setNeutralButton("Очистить") { _, _ ->
                android.app.AlertDialog.Builder(this)
                    .setTitle("Удалить историю?")
                    .setMessage("Будут удалены сохранённые диагностические отчёты только на этом телефоне.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Удалить") { _, _ ->
                        prefs().edit().remove(OBD2_HISTORY_KEY).apply()
                        showInfo("История очищена", "Локальные диагностические отчёты удалены с телефона.")
                    }
                    .show()
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun exportObd2Report(entry: Obd2DiagnosticHistoryEntry) {
        val report = Obd2DiagnosticReportFormatter.format(entry)
        pendingObd2ExportText = report
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, Obd2DiagnosticReportFormatter.suggestedFileName(entry))
        }
        try {
            startActivityForResult(intent, REQUEST_OBD2_EXPORT)
        } catch (error: Exception) {
            pendingObd2ExportText = null
            showInfo(
                "Экспорт отчёта",
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Не удалось открыть системный выбор места сохранения."
            )
        }
    }

    private fun shareObd2Report(entry: Obd2DiagnosticHistoryEntry) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Отчёт OBD2 — Hyundai Elantra 2021")
            putExtra(Intent.EXTRA_TEXT, Obd2DiagnosticReportFormatter.format(entry))
        }
        try {
            startActivity(Intent.createChooser(sendIntent, "Поделиться отчётом OBD2"))
        } catch (_: Exception) {
            showInfo("Поделиться отчётом", "На телефоне нет приложения для отправки текстовых отчётов.")
        }
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationFeature() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            4103
        )
    }

    private fun latestPhoneLocation(): android.location.Location? {
        if (!hasLocationPermission()) return null
        val manager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val gps = try { manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val network = try { manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
        return listOfNotNull(gps, network).maxByOrNull { it.time }
    }

    private fun saveParkingLocation() {
        if (!hasLocationPermission()) { requestLocationFeature(); return }
        val loc = latestPhoneLocation()
        if (loc == null) {
            showInfo("Парковка", "Не удалось получить координаты. Включи геолокацию и попробуй на открытом месте.")
            return
        }
        prefs().edit().putString("parking_lat", loc.latitude.toString())
            .putString("parking_lon", loc.longitude.toString())
            .putLong("parking_time", System.currentTimeMillis()).apply()
        showInfo("Место сохранено", "Координаты парковки сохранены на телефоне. Точность зависит от GPS: ±${loc.accuracy.toInt()} м.")
    }

    private fun findParkedCar() {
        val lat = prefs().getString("parking_lat", null)
        val lon = prefs().getString("parking_lon", null)
        if (lat == null || lon == null) { showInfo("Парковка", "Сначала сохрани место парковки."); return }
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Моя Elantra)")
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: Exception) { showInfo("Карты", "На телефоне нет приложения для открытия карты.") }
    }

    private fun showGpsPanel() {
        if (!hasLocationPermission()) { requestLocationFeature(); return }
        val loc = latestPhoneLocation()
        if (loc == null) { showInfo("GPS-панель", "Нет координат. Включи геолокацию и попробуй на открытом месте."); return }
        val speed = if (loc.hasSpeed()) String.format("%.0f км/ч", loc.speed * 3.6f) else "Нет данных о скорости"
        showInfo("GPS-панель телефона",
            "Скорость по GPS: $speed\nТочность: ±${loc.accuracy.toInt()} м\nВысота: ${"%.0f".format(loc.altitude)} м\nКоординаты: ${loc.latitude}, ${loc.longitude}\n\nЭто показания телефона, а не ЭБУ и не штатный спидометр. Не смотри на экран во время движения.")
    }

    private fun showMaintenance() {
        val current = EditText(this).apply { hint = "Текущий пробег, км"; inputType = 2; setText(prefs().getString("odo", "")) }
        val oilDue = EditText(this).apply { hint = "Пробег следующей замены масла, км"; inputType = 2; setText(prefs().getString("oil_due", "")) }
        val other = EditText(this).apply { hint = "Следующее ТО / заметка"; setText(prefs().getString("service_note", "")) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 8, 36, 0); addView(current); addView(oilDue); addView(other) }
        android.app.AlertDialog.Builder(this).setTitle("Обслуживание Hyundai").setMessage("Данные вводятся вручную и хранятся на телефоне.")
            .setView(box).setNegativeButton("Отмена", null).setPositiveButton("Сохранить") { _, _ ->
                prefs().edit().putString("odo", current.text.toString()).putString("oil_due", oilDue.text.toString()).putString("service_note", other.text.toString()).apply()
                val now = current.text.toString().toLongOrNull(); val due = oilDue.text.toString().toLongOrNull()
                val result = if (now != null && due != null) { val left = due - now; if (left >= 0) "До замены масла: $left км" else "Замена просрочена на ${-left} км" } else "Данные сохранены."
                showInfo("Напоминание", result + if (other.text.isNotBlank()) "\n${other.text}" else "")
            }.show()
    }

    private fun showFuelLog() {
        val liters = EditText(this).apply { hint = "Заправлено литров"; inputType = 8194 }
        val km = EditText(this).apply { hint = "Пробег с прошлой заправки, км"; inputType = 8194 }
        val cost = EditText(this).apply { hint = "Стоимость заправки (необязательно)"; inputType = 8194 }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 8, 36, 0); addView(liters); addView(km); addView(cost) }
        android.app.AlertDialog.Builder(this).setTitle("Расход топлива").setMessage("Для точного расчёта заправляйся до полного бака и вводи пройденный пробег.")
            .setView(box).setNegativeButton("Отмена", null).setPositiveButton("Рассчитать") { _, _ ->
                val l = liters.text.toString().replace(",", ".").toDoubleOrNull()
                val d = km.text.toString().replace(",", ".").toDoubleOrNull()
                val p = cost.text.toString().replace(",", ".").toDoubleOrNull()
                if (l != null && d != null && l > 0 && d > 0) {
                    val avg = l * 100 / d
                    prefs().edit().putString("fuel_avg", "%.2f".format(avg)).putString("fuel_cost", p?.toString() ?: "").apply()
                    showInfo("Результат", "Средний расход: %.2f л/100 км".format(avg) + if (p != null) "\nСтоимость заправки: %.2f".format(p) else "")
                } else showInfo("Проверь данные", "Укажи положительное количество литров и километров.")
            }.show()
    }

    private fun openLocalVideo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "video/*" }
        try { startActivityForResult(intent, 4105) } catch (_: Exception) { showInfo("Видео", "Не удалось открыть выбор видео.") }
    }

    private fun startObd2Bluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_OBD2_PERMISSION
            )
            return
        }
        showPairedObd2Devices()
    }

    private fun showPairedObd2Devices() {
        val adapter = try {
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        } catch (_: SecurityException) {
            null
        }

        if (adapter == null) {
            showInfo("Bluetooth недоступен", "Телефон не поддерживает Bluetooth.")
            return
        }

        if (!adapter.isEnabled) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Включи Bluetooth")
                .setMessage("Для подключения ELM327 включи Bluetooth в настройках телефона.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Настройки Bluetooth") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (_: Exception) {
                        showInfo("Bluetooth", "Не удалось открыть настройки Bluetooth.")
                    }
                }
                .show()
            return
        }

        val devices = try {
            adapter.bondedDevices.toList().sortedBy { it.name ?: it.address }
        } catch (_: SecurityException) {
            showInfo("Нет разрешения", "Разреши приложению доступ к Bluetooth-устройствам поблизости.")
            return
        }

        if (devices.isEmpty()) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Нет сопряжённых устройств")
                .setMessage(
                    "Сначала подключи ELM327 в настройках Android: Bluetooth → Добавить устройство. " +
                        "Для многих адаптеров PIN — 1234 или 0000. Затем вернись сюда."
                )
                .setNegativeButton("Назад", null)
                .setPositiveButton("Настройки Bluetooth") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    } catch (_: Exception) {
                        showInfo("Bluetooth", "Не удалось открыть настройки Bluetooth.")
                    }
                }
                .show()
            return
        }

        val labels = devices.map { device ->
            "${device.name ?: "Bluetooth устройство"}\n${device.address}"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Выбери сопряжённый OBD2 адаптер")
            .setItems(labels) { _, which -> connectObd2Device(devices[which]) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun connectObd2Device(device: android.bluetooth.BluetoothDevice) {
        obd2Client?.close()
        val client = Obd2BluetoothClient()
        obd2Client = client
        val deviceName = try {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        } catch (_: SecurityException) {
            "OBD2 адаптер"
        }

        val progress = android.app.AlertDialog.Builder(this)
            .setTitle("Подключение к OBD2")
            .setMessage("Подключаюсь к $deviceName и проверяю связь с ЭБУ. Это может занять несколько секунд.")
            .setNegativeButton("Отмена", null)
            .create()

        progress.setOnCancelListener {
            if (obd2Client === client) {
                client.close()
                obd2Client = null
            }
        }
        progress.setOnShowListener {
            progress.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                progress.dismiss()
                client.close()
                if (obd2Client === client) obd2Client = null
            }
        }
        progress.show()

        client.connect(device, object : Obd2BluetoothClient.Listener {
            override fun onConnected(deviceName: String, adapterInfo: String, ecuResponse: String, livePidResponses: Map<Int, String>, dtcResponse: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || obd2Client !== client) return@runOnUiThread
                    if (progress.isShowing) progress.dismiss()

                    val supportedPids = Obd2ResponseParser.supportedPids(ecuResponse)
                    val ecuResponded = supportedPids != null
                    val liveDataSummary = if (supportedPids != null) {
                        buildList {
                            if (12 in supportedPids) {
                                val response = livePidResponses[0x0C].orEmpty()
                                val rpm = Obd2LiveDataParser.engineRpm(response)
                                val value = rpm?.let {
                                    String.format(java.util.Locale.US, "%.2f", it)
                                } ?: "нет корректного ответа"
                                add("Обороты двигателя (010C): $value об/мин")
                            } else {
                                add("Обороты двигателя (010C): ЭБУ не объявил поддержку PID")
                            }

                            if (5 in supportedPids) {
                                val response = livePidResponses[0x05].orEmpty()
                                val temperature = Obd2LiveDataParser.coolantTemperatureC(response)
                                val value = temperature?.let { "$it °C" } ?: "нет корректного ответа"
                                add("Температура охлаждающей жидкости (0105): $value")
                            } else {
                                add("Температура охлаждающей жидкости (0105): ЭБУ не объявил поддержку PID")
                            }
                        }.joinToString("\n")
                    } else {
                        "Показания не запрашивались: корректная карта поддерживаемых PID не получена."
                    }

                    val dtcSummary = if (supportedPids != null) {
                        when (val codes = Obd2DtcParser.troubleCodes(dtcResponse)) {
                            null -> "Нет корректного ответа на Mode 03. Ответ адаптера: $dtcResponse"
                            else -> if (codes.isEmpty()) {
                                "Сохранённые стандартные коды Mode 03 не обнаружены."
                            } else {
                                "Найдено кодов: ${codes.size}\n" + codes.joinToString("\n") { code ->
                                    val description = Obd2DtcDescriptions.descriptionFor(code)
                                    if (description != null) {
                                        "$code — $description"
                                    } else {
                                        "$code — нет проверенного описания в локальном справочнике."
                                    }
                                } +
                                    "\n\nСправочник содержит только выбранные стандартные P0-коды. Неизвестные и производитель-специфичные коды не интерпретируются. Код указывает на обнаруженную область неисправности, а не подтверждает поломку конкретной детали. Для точной проверки используй сервисную документацию Hyundai по VIN."
                            }
                        }
                    } else {
                        "Не запрашивались: корректный ответ ЭБУ на PID 0100 не получен."
                    }

                    val message = if (supportedPids != null) {
                        "Диагностический сеанс завершён; Bluetooth отключён. Адаптер: $deviceName\n\n" +
                            "Ответ ELM327: $adapterInfo\n\n" +
                            "ЭБУ ответил на запрос поддерживаемых параметров (0100):\n$ecuResponse\n\n" +
                            "Расшифровка карты PID:\n${Obd2ResponseParser.describeSupportedPids(supportedPids)}\n\n" +
                            "Однократное чтение поддерживаемых параметров:\n$liveDataSummary\n\n" +
                            "Сохранённые коды неисправностей (Mode 03):\n$dtcSummary\n\n" +
                            "Mode 03 охватывает стандартные emission-related коды силового агрегата, а не все блоки ABS, SRS, кузова или трансмиссии.\n\n" +
                            "Это моментальный снимок, а не потоковая телеметрия. Используй диагностику на стоянке; не смотри на экран во время движения."
                    } else {
                        "Bluetooth-подключение к $deviceName выполнено, но корректный ответ ЭБУ на PID 0100 не подтверждён. Сеанс завершён, Bluetooth отключён.\n\n" +
                            "Ответ адаптера: $ecuResponse\n\n" +
                            "Коды Mode 03: $dtcSummary\n\n" +
                            "Проверь совместимость ELM327, положение ключа ON и подключение адаптера. " +
                            "Не запускай двигатель только ради этой проверки."
                    }

                    saveObd2History(
                        Obd2DiagnosticHistoryEntry(
                            timestampMillis = System.currentTimeMillis(),
                            adapterName = deviceName,
                            summary = message,
                            status = if (ecuResponded) {
                                Obd2DiagnosticStatus.COMPLETED
                            } else {
                                Obd2DiagnosticStatus.ECU_RESPONSE_UNCONFIRMED
                            }
                        )
                    )
                    if (obd2Client === client) obd2Client = null

                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle(if (ecuResponded) "Диагностика завершена" else "Ответ ЭБУ не подтверждён")
                        .setMessage(message)
                        .setPositiveButton("Готово", null)
                        .show()
                }
            }

            override fun onFailure(status: Obd2DiagnosticStatus, message: String) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || obd2Client !== client) return@runOnUiThread
                    if (progress.isShowing) progress.dismiss()
                    obd2Client = null
                    val detail = "$message\n\nУбедись, что адаптер сопряжён с телефоном, не занят другим приложением и поддерживает Bluetooth Classic SPP."
                    saveObd2History(
                        Obd2DiagnosticHistoryEntry(
                            timestampMillis = System.currentTimeMillis(),
                            adapterName = deviceName,
                            summary = detail,
                            status = status
                        )
                    )
                    showInfo(status.displayLabel, detail)
                }
            }
        })
    }

    private fun showConnectionStatus() {
        val wifi = try {
            val manager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            if (manager.isWifiEnabled) "включён" else "выключен"
        } catch (_: Exception) { "статус недоступен" }
        val bt = try {
            val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) "не поддерживается" else if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) "неизвестен (нет разрешения)" else if (adapter.isEnabled) "включён" else "выключен"
        } catch (_: Exception) { "статус недоступен" }
        showInfo("Связь телефона", "Bluetooth: $bt\nWi-Fi: $wifi\n\nПоказывается состояние модулей телефона. Это не означает, что телефон подключён к ЭБУ автомобиля. Android Auto использует поддерживаемое автомобилем соединение.")
    }

    private fun startVoiceAssistant() {
        try {
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Произнеси команду")
            }
            startActivityForResult(intent, 4104)
        } catch (_: Exception) { showInfo("Голосовой помощник", "На телефоне нет доступного сервиса распознавания речи.") }
    }

    private fun showTripNotes() {
        val edit = EditText(this).apply { hint = "Например: поездка, пробег, ремонт, заметка"; minLines = 3; setText(prefs().getString("trip_notes", "")) }
        android.app.AlertDialog.Builder(this).setTitle("Заметки и история").setView(edit)
            .setNegativeButton("Отмена", null).setPositiveButton("Сохранить") { _, _ ->
                prefs().edit().putString("trip_notes", edit.text.toString()).apply()
                showInfo("Сохранено", "Заметка сохранена локально на телефоне.")
            }.show()
    }

    private fun showThemePicker() {
        val themes = arrayOf("Sport Blue", "Dark", "Neon")
        android.app.AlertDialog.Builder(this).setTitle("Стиль интерфейса").setItems(themes) { _, which ->
            val color = when (which) { 0 -> Color.rgb(10, 16, 28); 1 -> Color.rgb(5, 5, 8); else -> Color.rgb(18, 8, 30) }
            prefs().edit().putInt("theme_bg", color).apply()
            recreate()
        }.show()
    }

    @Deprecated("Activity result used for document picker and speech recognition")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OBD2_EXPORT) {
            val report = pendingObd2ExportText
            pendingObd2ExportText = null
            if (resultCode != RESULT_OK) return

            val uri = data?.data
            if (uri == null || report == null) {
                showInfo("Экспорт отчёта", "Не удалось получить файл или подготовленный отчёт.")
                return
            }

            try {
                val output = contentResolver.openOutputStream(uri, "w")
                    ?: throw java.io.IOException("Не удалось открыть выбранный файл для записи.")
                output.use { it.write(report.toByteArray(Charsets.UTF_8)) }
                showInfo("Отчёт сохранён", "Текстовый отчёт OBD2 сохранён в выбранное место.")
            } catch (error: Exception) {
                showInfo(
                    "Ошибка экспорта",
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Не удалось записать диагностический отчёт."
                )
            }
            return
        }

        if (resultCode != RESULT_OK || data == null) return
        if (requestCode == 4105) {
            val uri = data.data ?: return
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "video/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
            } catch (_: Exception) {
                showInfo("Видео", "На телефоне нет приложения для воспроизведения этого видео.")
            }
        } else if (requestCode == 4104) {
            val words = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) showInfo("Распознано", words)
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
        } else if (requestCode == REQUEST_OBD2_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPairedObd2Devices()
            } else {
                showInfo(
                    "Bluetooth-разрешение не выдано",
                    "Для связи с уже сопряжённым ELM327 нужно разрешение «Устройства поблизости» (Bluetooth Connect). Без него приложение не может подключиться к адаптеру."
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