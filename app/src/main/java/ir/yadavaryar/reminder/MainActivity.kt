package ir.yadavaryar.reminder

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var page: LinearLayout
    private lateinit var scheduleList: LinearLayout
    private lateinit var permissionText: TextView
    private lateinit var permissionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            setBackgroundColor(color("#F4F7F2"))
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            textDirection = View.TEXT_DIRECTION_RTL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }
        scroll.addView(page)
        setContentView(scroll)

        buildHeader()
        buildPermissionPanel()
        addForm(KIND_ALARM)
        addForm(KIND_REMINDER)
        addSectionTitle("برنامه‌های تنظیم‌شده")
        scheduleList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(scheduleList, matchWrap())
        renderSchedules()

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionText.isInitialized) updatePermissionStatus()
    }

    private fun buildHeader() {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = rounded("#174D3D", 22)
        }
        header.addView(label("یادآور یار", 27, "#FFFFFF", true).apply { gravity = Gravity.CENTER })
        header.addView(label("زنگ هشدار و یادآور اعمال با پیام امام‌زمانی", 14, "#E0F0E7", false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        page.addView(header, matchWrap())
        page.addView(label("هر زمان برای یک کار مهم یادآور بساز؛ زنگ می‌زند یا در نوار اعلان‌ها ظاهر می‌شود.", 13, "#53665B", false).apply {
            setPadding(dp(4), dp(12), dp(4), dp(16))
        })
    }

    private fun buildPermissionPanel() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded("#E8F1E9", 16)
        }
        permissionText = label("در حال بررسی مجوزها…", 13, "#345448", false)
        permissionButton = Button(this).apply {
            text = "بررسی و فعال‌سازی مجوزها"
            isAllCaps = false
            setOnClickListener { openPermissionSettings() }
        }
        panel.addView(permissionText, matchWrap())
        panel.addView(permissionButton, matchWrap().apply { topMargin = dp(6) })
        page.addView(panel, matchWrap().apply { bottomMargin = dp(16) })
        updatePermissionStatus()
    }

    private fun updatePermissionStatus() {
        val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val exactAllowed = Build.VERSION.SDK_INT < 31 ||
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)

        val lines = mutableListOf<String>()
        lines.add(if (notificationsAllowed) "✓ اجازه نمایش اعلان‌ها فعال است." else "! اجازه اعلان‌ها هنوز فعال نشده است.")
        lines.add(if (exactAllowed) "✓ زمان‌بندی دقیق زنگ‌ها فعال است." else "! برای زنگ دقیق، مجوز «زنگ هشدار و یادآورها» لازم است.")
        permissionText.text = lines.joinToString("\n")
        permissionButton.visibility = if (notificationsAllowed && exactAllowed) View.GONE else View.VISIBLE
    }

    private fun openPermissionSettings() {
        val notificationsAllowed = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                }
            }
        }
        updatePermissionStatus()
    }

    private fun addForm(kind: String) {
        val isAlarm = kind == KIND_ALARM
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(15), dp(16), dp(16))
            background = rounded("#FFFFFF", 18)
            elevation = dp(2).toFloat()
        }
        card.addView(label(if (isAlarm) "تنظیم زنگ هشدار" else "تنظیم یادآور یک عمل", 18, "#174D3D", true))
        card.addView(label(
            if (isAlarm) "در ساعت تعیین‌شده، صدای زنگ گوشی پخش می‌شود."
            else "در زمان انتخابی، اعلان همراه با پیام در نوار اعلان‌های گوشی می‌آید.",
            12, "#65746B", false
        ).apply { setPadding(0, dp(5), 0, dp(12)) })

        val titleInput = EditText(this).apply {
            hint = if (isAlarm) "عنوان زنگ؛ مثلاً بیدار شدن" else "نام عمل؛ مثلاً خواندن دعای عهد"
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            imeOptions = EditorInfo.IME_ACTION_NEXT
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded("#F1F5F1", 12)
        }
        card.addView(titleInput, matchWrap().apply { bottomMargin = dp(9) })

        val now = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        var selectedHour = now.get(Calendar.HOUR_OF_DAY)
        var selectedMinute = now.get(Calendar.MINUTE)
        val timeButton = Button(this).apply {
            text = "انتخاب ساعت: ${formatTime(selectedHour, selectedMinute)}"
            isAllCaps = false
            setOnClickListener {
                TimePickerDialog(
                    this@MainActivity,
                    { _, hour, minute ->
                        selectedHour = hour
                        selectedMinute = minute
                        text = "انتخاب ساعت: ${formatTime(hour, minute)}"
                    },
                    selectedHour,
                    selectedMinute,
                    true
                ).show()
            }
        }
        card.addView(timeButton, matchWrap().apply { bottomMargin = dp(4) })

        val dailyCheck = CheckBox(this).apply {
            text = "هر روز در همین ساعت تکرار شود"
            isChecked = true
            textSize = 13f
            buttonTintList = android.content.res.ColorStateList.valueOf(color("#23745A"))
        }
        card.addView(dailyCheck, matchWrap())

        val messageInput = EditText(this).apply {
            hint = "پیام همراه اعلان"
            setText("یا صاحب‌الزمان، یاریم کنید امروز را در مسیر رضایت شما قدم بردارم.")
            minLines = 2
            maxLines = 4
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            gravity = Gravity.TOP or Gravity.RIGHT
            textSize = 13f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded("#F1F5F1", 12)
        }
        card.addView(messageInput, matchWrap().apply { topMargin = dp(7); bottomMargin = dp(9) })

        val saveButton = Button(this).apply {
            text = if (isAlarm) "ذخیره زنگ" else "ذخیره یادآور"
            isAllCaps = false
            setTextColor(color("#FFFFFF"))
            background = rounded("#23745A", 12)
            setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isBlank()) {
                    titleInput.error = "عنوان را وارد کن"
                    titleInput.requestFocus()
                    return@setOnClickListener
                }
                val item = ScheduleItem(
                    id = ScheduleRepository.nextId(this@MainActivity),
                    kind = kind,
                    title = title,
                    hour = selectedHour,
                    minute = selectedMinute,
                    daily = dailyCheck.isChecked,
                    enabled = true,
                    message = messageInput.text.toString().trim().ifBlank {
                        "یا صاحب‌الزمان، یاریم کنید در مسیر رضایت شما قدم بردارم."
                    }
                )
                ScheduleRepository.upsert(this@MainActivity, item)
                AlarmScheduler.schedule(this@MainActivity, item)
                titleInput.text.clear()
                Toast.makeText(
                    this@MainActivity,
                    if (isAlarm) "زنگ ذخیره شد." else "یادآور ذخیره شد.",
                    Toast.LENGTH_SHORT
                ).show()
                renderSchedules()
            }
        }
        card.addView(saveButton, matchWrap().apply { topMargin = dp(4) })
        page.addView(card, matchWrap().apply { bottomMargin = dp(14) })
    }

    private fun renderSchedules() {
        if (!::scheduleList.isInitialized) return
        scheduleList.removeAllViews()
        val items = ScheduleRepository.getAll(this).sortedWith(compareBy<ScheduleItem>({ it.hour }, { it.minute }))
        if (items.isEmpty()) {
            scheduleList.addView(label("هنوز زنگ یا یادآوری‌ای تنظیم نکرده‌ای.", 13, "#758179", false).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, dp(24))
            })
            return
        }
        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded("#FFFFFF", 15)
            }
            val firstRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
            info.addView(label(if (item.kind == KIND_ALARM) "⏰ زنگ هشدار" else "🔔 یادآور عمل", 12, "#23745A", true))
            info.addView(label(item.title, 16, "#24382F", true).apply { setPadding(0, dp(3), 0, 0) })
            info.addView(label(formatTime(item.hour, item.minute) + if (item.daily) "  •  هر روز" else "  •  یک‌بار", 12, "#69766E", false).apply {
                setPadding(0, dp(4), 0, 0)
            })
            firstRow.addView(info)
            val enabledSwitch = Switch(this).apply {
                isChecked = item.enabled
                text = "فعال"
                textSize = 11f
                setOnCheckedChangeListener { _, checked ->
                    val changed = item.copy(enabled = checked)
                    ScheduleRepository.upsert(this@MainActivity, changed)
                    if (checked) AlarmScheduler.schedule(this@MainActivity, changed)
                    else AlarmScheduler.cancel(this@MainActivity, item.id)
                    renderSchedules()
                }
            }
            firstRow.addView(enabledSwitch)
            card.addView(firstRow)
            card.addView(label(item.message, 12, "#66736B", false).apply {
                maxLines = 2
                setPadding(0, dp(8), 0, dp(5))
            })
            val deleteButton = Button(this).apply {
                text = "حذف این مورد"
                isAllCaps = false
                textSize = 12f
                setOnClickListener {
                    AlarmScheduler.cancel(this@MainActivity, item.id)
                    ScheduleRepository.remove(this@MainActivity, item.id)
                    renderSchedules()
                }
            }
            card.addView(deleteButton, matchWrap())
            scheduleList.addView(card, matchWrap().apply { bottomMargin = dp(9) })
        }
    }

    private fun addSectionTitle(text: String) {
        page.addView(label(text, 18, "#174D3D", true).apply {
            setPadding(dp(2), dp(6), dp(2), dp(10))
        })
    }

    private fun label(text: String, size: Int, hex: String, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color(hex))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        textDirection = View.TEXT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }

    private fun rounded(hex: String, radiusDp: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color(hex))
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun color(hex: String) = android.graphics.Color.parseColor(hex)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun formatTime(hour: Int, minute: Int) = String.format(Locale("fa"), "%02d:%02d", hour, minute)

    companion object {
        private const val REQUEST_NOTIFICATIONS = 201
    }
}
