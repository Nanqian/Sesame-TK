package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.StringUtil
import java.io.File

/**
 * @author Byseven
 * @date 2025/2/21
 * @apiNote
 */
object DataCache {
    private const val TAG: String = "DataCache"
    private const val FILENAME = "dataCache.json"

    @get:JsonIgnore
    private var init = false

    // 通用数据缓存
    val dataMap: MutableMap<String, Any> = mutableMapOf()

    init {
        // 在单例初始化时加载数据
        load()
    }

    fun <T> saveData(key: String, value: T): Boolean {
        if (value == null) {
            Log.error(TAG, "Value for key '$key' cannot be null.")
            return false
        }
        dataMap[key] = value
        return save()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getData(key: String, defaultValue: T? = null): T? {
        return dataMap[key] as? T ?: defaultValue
    }

    /**
     * 安全获取 Set<String> 类型的缓存值，自动处理 List
     * @/Set 类型兼容问题
     */
    fun getSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        val value = dataMap[key]
        return when (value) {
            is Set<*> -> value.mapNotNull { it as? String }.toSet()
            is List<*> -> value.mapNotNull { it as? String }.toSet()
            else -> defaultValue.toMutableSet()
        }
    }

    fun getList(key: String, defaultValue: List<String> = emptyList()): List<String> {
        val value = dataMap[key]
        return when (value) {
            is List<*> -> value.mapNotNull { it as? String }
            is Set<*> -> value.mapNotNull { it as? String }
            else -> defaultValue
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        val value = dataMap[key]
        return when (value) {
            is String -> value
            else -> defaultValue
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = dataMap[key]
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = dataMap[key]
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val value = dataMap[key]
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        val value = dataMap[key]
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: defaultValue
            else -> defaultValue
        }
    }


    fun saveList(key: String, value: List<String>): Boolean {
        return saveData(key, value)
    }

    fun saveString(key: String, value: String): Boolean {
        return saveData(key, value)
    }

    fun saveBoolean(key: String, value: Boolean): Boolean {
        return saveData(key, value)
    }

    fun saveInt(key: String, value: Int): Boolean {
        return saveData(key, value)
    }

    fun saveLong(key: String, value: Long): Boolean {
        return saveData(key, value)
    }

    fun saveDouble(key: String, value: Double): Boolean {
        return saveData(key, value)
    }

    fun saveSet(key: String, value: Set<String>): Boolean {
        return saveData(key, value)
    }


    fun removeData(key: String): Boolean {
        if (dataMap.containsKey(key)) {
            dataMap.remove(key)
            return save()
        }
        return false
    }

    fun clearAllData(): Boolean {
        dataMap.clear()
        return save()
    }

    private fun save(): Boolean {
        Log.record(TAG, "save DataCache")
        return Files.write2File(
            JsonUtil.formatJson(this),
            Files.getTargetFileofDir(Files.MAIN_DIR, FILENAME)
        )
    }

    @Synchronized
    fun load() {
        if (init) return // 避免重复加载
        val targetFile = Files.getTargetFileofDir(Files.MAIN_DIR, FILENAME)
        try {
            if (targetFile.exists()) {
                val json = Files.readFromFile(targetFile)
                JsonUtil.copyMapper().readerForUpdating(this).readValue<Any>(json)
                val formatted = JsonUtil.formatJson(this)
                if (formatted != null && formatted != json) {
                    Log.runtime(TAG, "format $TAG config")
                    Files.write2File(formatted, targetFile)
                }
            } else {
                Log.runtime(TAG, "init $TAG config")
                JsonUtil.copyMapper().updateValue(this, DataCache)
                Files.write2File(JsonUtil.formatJson(this), targetFile)
            }
        } catch (e: Exception) {
            Log.error(TAG, "加载缓存数据失败：${e.message}")
        }
        init = true
    }

    @Synchronized
    fun reset(targetFile: File?) {
        try {
            JsonUtil.copyMapper().updateValue(this, DataCache)
            Files.write2File(JsonUtil.formatJson(this), targetFile)
            Log.runtime(TAG, "reset $TAG config success")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
            Log.error(TAG, "reset $TAG config failed")
        }
    }
}