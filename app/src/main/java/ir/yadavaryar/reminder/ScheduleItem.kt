package ir.yadavaryar.reminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

const val KIND_ALARM = "alarm"
const val KIND_REMINDER = "reminder"

 data class ScheduleItem(
    val id: Int,
    val kind: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val daily: Boolean,
    val enabled: Boolean,
    val message: String
)

object ScheduleRepository {
    private const val PREFS = "yad_avar_yar_data"
    private const val KEY_ITEMS = "items"

    fun getAll(context: Context): List<ScheduleItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    add(
                        ScheduleItem(
                            id = o.optInt("id"),
                            kind = o.optString("kind", KIND_REMINDER),
                            title = o.optString("title", "یادآوری"),
                            hour = o.optInt("hour", 8).coerceIn(0, 23),
                            minute = o.optInt("minute", 0).coerceIn(0, 59),
                            daily = o.optBoolean("daily", true),
                            enabled = o.optBoolean("enabled", true),
                            message = o.optString("message", "یا صاحب‌الزمان، یاریم کنید در مسیر رضایت شما قدم بردارم.")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAll(context: Context, items: List<ScheduleItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("kind", item.kind)
                    put("title", item.title)
                    put("hour", item.hour)
                    put("minute", item.minute)
                    put("daily", item.daily)
                    put("enabled", item.enabled)
                    put("message", item.message)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    fun upsert(context: Context, item: ScheduleItem) {
        val items = getAll(context).toMutableList()
        val index = items.indexOfFirst { it.id == item.id }
        if (index >= 0) items[index] = item else items.add(item)
        saveAll(context, items)
    }

    fun remove(context: Context, id: Int) {
        saveAll(context, getAll(context).filterNot { it.id == id })
    }

    fun nextId(context: Context): Int = (getAll(context).maxOfOrNull { it.id } ?: 0) + 1
}
