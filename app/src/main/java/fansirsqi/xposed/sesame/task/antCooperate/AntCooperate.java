package fansirsqi.xposed.sesame.task.antCooperate;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StatusUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;
public class AntCooperate extends ModelTask {
    private static final String TAG = AntCooperate.class.getSimpleName();
    private static final String UserId = UserMap.getCurrentUid();
    @Override
    public String getName() {
        return "合种";
    }
    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }
    @Override
    public String getIcon() {
        return "AntCooperate.png";
    }
    private final BooleanModelField cooperateWater = new BooleanModelField("cooperateWater", "合种浇水|开启", false);
    private final SelectAndCountModelField cooperateWaterList = new SelectAndCountModelField("cooperateWaterList", "合种浇水列表", new LinkedHashMap<>(), CooperateEntity::getList,"开启合种浇水后执行一次重载");
    private final SelectAndCountModelField cooperateWaterTotalLimitList = new SelectAndCountModelField("cooperateWaterTotalLimitList", "浇水总量限制列表", new LinkedHashMap<>(), CooperateEntity::getList);
    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(cooperateWater);
        modelFields.addField(cooperateWaterList);
        modelFields.addField(cooperateWaterTotalLimitList);
        return modelFields;
    }
    @Override
    public Boolean check() {
        return !TaskCommon.IS_ENERGY_TIME;
    }
    @Override
    public void run() {
        try {
            Log.other("执行开始-" + getName());
            if (cooperateWater.getValue()) {
                String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                if (s == null) {
                    ThreadUtil.sleep(RandomUtil.delay());
                    s = AntCooperateRpcCall.queryUserCooperatePlantList();
                }
                JSONObject jo = new JSONObject(s);
                if (ResUtil.checkResCode(jo)) {
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                    JSONArray ja = jo.getJSONArray("cooperatePlants");
                    for (int i = 0; i < ja.length(); i++) {
                        jo = ja.getJSONObject(i);
                        String cooperationId = jo.getString("cooperationId");
                        if (!jo.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            jo = new JSONObject(s).getJSONObject("cooperatePlant");
                        }
                        String name = jo.getString("name");
                        int waterDayLimit = jo.getInt("waterDayLimit");
                        Log.debug(TAG, "合种[" + name + "]:" + cooperationId + ", 限额:" + waterDayLimit);
                        CooperateMap.getInstance(CooperateMap.class).add(cooperationId, name);
                        if (!StatusUtil.canCooperateWaterToday(UserId, cooperationId)) {
                            continue;
                        }
                        Integer num = cooperateWaterList.getValue().get(cooperationId);
                        if (num != null) {
                            Integer limitNum = cooperateWaterTotalLimitList.getValue().get(cooperationId);
                            if (limitNum != null) {
                                num = calculatedWaterNum(cooperationId, num, limitNum);
                            }
                            if (num > waterDayLimit) {
                                num = waterDayLimit;
                            }
                            if (num > userCurrentEnergy) {
                                num = userCurrentEnergy;
                            }
                            if (num > 0) {
                                cooperateWater(cooperationId, num, name);
                            }
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            CooperateMap.getInstance(CooperateMap.class).save(UserId);
            Log.other("执行结束-" + getName());
        }
    }
    private static void cooperateWater(String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(AntCooperate.UserId, coopId, count);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResCode(jo)) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                StatusUtil.cooperateWaterToday(UserId, coopId);
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            ThreadUtil.sleep(1500);
        }
    }
    private static int calculatedWaterNum(String coopId, int num, int limitNum) {
        try {
            String s = AntCooperateRpcCall.queryCooperateRank("A", coopId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success", false)) {
                JSONArray jaList = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    String userId = joItem.getString("userId");
                    if (userId.equals(AntCooperate.UserId)) {
                        int energySummation = joItem.optInt("energySummation", 0);
                        if (num > limitNum - energySummation) {
                            num = limitNum - energySummation;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "calculatedWaterNum err:");
            Log.printStackTrace(TAG, t);
        }
        return num;
    }
}
