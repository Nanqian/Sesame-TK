package fansirsqi.xposed.sesame.data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonMappingException;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import lombok.Data;
/**
 * UI 配置类，用于管理应用的 UI 配置。
 * 包括加载、保存、卸载等操作。
 */
@Data
public class UIConfig {
    private static final String TAG = UIConfig.class.getSimpleName();
    // 单例模式：UIConfig 的唯一实例
    public static final UIConfig INSTANCE = new UIConfig();
    // 用于标记是否已初始化，防止重复初始化
    @JsonIgnore
    private boolean init;
    // 是否使用新 UI
    private Boolean newUI = true;
    /**
     * 保存当前 UI 配置到文件中
     *
     * @return 保存成功返回 true，失败返回 false
     */
    public static Boolean save() {
        Log.record("保存UI配置");
        return Files.setUIConfigFile(JsonUtil.formatJson(INSTANCE));
    }
    /**
 * 加载 UI 配置文件，如果文件不存在，则初始化为默认配置
 *
 * @return 加载后的 UIConfig 实例
 */
public static synchronized UIConfig load() {
    java.io.File uiConfigFile = Files.getUIConfigFile();
    try {
        if (uiConfigFile.exists()) {
            String json = Files.readFromFile(uiConfigFile);
            if (!json.trim().isEmpty()) {
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
                String formatted = JsonUtil.formatJson(INSTANCE);
                if (formatted != null && !formatted.equals(json)) {
                    Log.runtime(TAG, "格式化UI配置");
                    Files.write2File(formatted, uiConfigFile);
                }
            } else {
                Log.runtime(TAG, "配置文件为空，使用默认配置");
                resetToDefault();
            }
        } else {
            Log.runtime(TAG, "配置文件不存在，初始化默认配置");
            resetToDefault();
            Files.write2File(JsonUtil.formatJson(INSTANCE), uiConfigFile);
        }
    } catch (Exception e) {
        Log.printStackTrace(TAG, e);
        Log.runtime(TAG, "重置UI配置");
        resetToDefault();
        try {
            Files.write2File(JsonUtil.formatJson(INSTANCE), uiConfigFile);
        } catch (Exception e2) {
            Log.printStackTrace(TAG, e2);
        }
    }
    INSTANCE.setInit(true);
    return INSTANCE;
}
/**
 * 重置 UIConfig 实例为默认配置
 */
private static void resetToDefault() {
    unload();
    INSTANCE.setNewUI(true);
}
    public static synchronized void unload() {
        try {
            JsonUtil.copyMapper().updateValue(INSTANCE, new UIConfig());
        } catch (JsonMappingException e) {
            Log.printStackTrace(TAG, e);
        }
    }
}
