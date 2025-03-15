package fansirsqi.xposed.sesame.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import fansirsqi.xposed.sesame.entity.UserEntity;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.StringUtil;
import lombok.Data;

@Data
public class Config {
    private static boolean isLoaded = false;
    private static final String TAG = Config.class.getSimpleName();
    public static final Config INSTANCE = new Config();
    @JsonIgnore private volatile boolean init;
    private final Map<String, ModelFields> modelFieldsMap = new ConcurrentHashMap<>();

    public void setModelFieldsMap(Map<String, ModelFields> newModels) {
        modelFieldsMap.clear();
        Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
        if (newModels == null) {
            newModels = new HashMap<>();
        }
        for (ModelConfig modelConfig : modelConfigMap.values()) {
            String modelCode = modelConfig.getCode();
            ModelFields newModelFields = new ModelFields();
            ModelFields configModelFields = modelConfig.getFields();
            ModelFields modelFields = newModels.get(modelCode);
            for (ModelField<?> configModelField : configModelFields.values()) {
                ModelField<?> modelField = (modelFields != null) ? modelFields.get(configModelField.getCode()) : null;
                try {
                    Object value = modelField != null ? modelField.getValue() : null;
                    if (value != null) {
                        configModelField.setObjectValue(value);
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
                newModelFields.addField(configModelField);
            }
            modelFieldsMap.put(modelCode, newModelFields);
        }
    }

    public Boolean hasModelFields(String modelCode) {
        return modelFieldsMap.containsKey(modelCode);
    }

    public Boolean hasModelField(String modelCode, String fieldCode) {
        ModelFields modelFields = modelFieldsMap.get(modelCode);
        return modelFields != null && modelFields.containsKey(fieldCode);
    }

    public static Boolean isModify(String userId) {
        File configV2File = Files.getConfigV2File(userId);
        if (!configV2File.exists()) return true;
        String currentJson = Files.readFromFile(configV2File);
        return !Objects.equals(JsonUtil.formatJson(INSTANCE), currentJson);
    }

    public static synchronized Boolean save(String userId, Boolean force) {
        if (!force && !isModify(userId)) return true;
        try {
            String json = JsonUtil.formatJson(INSTANCE);
            if (StringUtil.isEmpty(userId)) {
                return Files.setDefaultConfigV2File(json);
            } else {
                return Files.setConfigV2File(userId, json);
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            Log.record("保存用户配置失败");
            return false;
        }
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    public static synchronized boolean load(String userId) {
        Log.record(TAG, "开始加载配置...");
        try {
            File configV2File = Files.getConfigV2File(userId);
            UserEntity userEntity = UserMap.get(userId);
            String userName = StringUtil.isEmpty(userId) ? "默认" : (userEntity != null ? userEntity.getShowName() : userId);
            Log.record("载入用户[" + userName + "]配置");
            if (configV2File.exists()) {
                updateConfigFromJson(configV2File);
            } else {
                createDefaultConfig(userId);
            }
            INSTANCE.setInit(true);
            isLoaded = true;
            Log.record(TAG, "加载配置结束！");
            return true;
        } catch (Throwable t) {
            handleLoadFailure(userId);
            return false;
        }
    }

    public static synchronized void unload() {
        for (ModelFields modelFields : INSTANCE.modelFieldsMap.values()) {
            for (ModelField<?> modelField : modelFields.values()) {
                if (modelField != null) {
                    modelField.reset();
                }
            }
        }
    }

    private static void updateConfigFromJson(File configV2File) {
        String json = Files.readFromFile(configV2File);
        if (StringUtil.isEmpty(json)) {
            Log.record(TAG, "配置文件内容为空，初始化新配置");
            writeFileWithFormattedJson(configV2File);
        } else {
            try {
                JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json);
            } catch (IOException e) {
                Log.printStackTrace(TAG, e);
                writeFileWithFormattedJson(configV2File);
            }
        }
    }

    private static void createDefaultConfig(String userId) {
        File defaultConfigV2File = Files.getDefaultConfigV2File();
        if (defaultConfigV2File.exists()) {
            String defaultJson = Files.readFromFile(defaultConfigV2File);
            if (StringUtil.isEmpty(defaultJson)) {
                writeFileWithFormattedJson(Files.getConfigV2File(userId));
            } else {
                Files.write2File(defaultJson, Files.getConfigV2File(userId));
            }
        } else {
            writeFileWithFormattedJson(Files.getConfigV2File(userId));
        }
    }

    private static void handleLoadFailure(String userId) {
        if (StringUtil.isEmpty(userId)) {
            unload();
            try {
                Files.write2File(JsonUtil.formatJson(INSTANCE), Files.getDefaultConfigV2File());
            } catch (Exception e) {
                Log.printStackTrace(TAG, e);
            }
        }
        isLoaded = false;
    }

    private static void writeFileWithFormattedJson(File configV2File) {
        try {
            String formatted = JsonUtil.formatJson(INSTANCE);
            Files.write2File(formatted, configV2File);
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    // Helper methods to avoid duplicate code
    public static File getConfigV2FileOrDefault(String userId) {
        return StringUtil.isEmpty(userId) ? Files.getDefaultConfigV2File() : Files.getConfigV2File(userId);
    }

    public static String getFormattedJson() {
        try {
            return JsonUtil.formatJson(INSTANCE);
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
            return null;
        }
    }
}