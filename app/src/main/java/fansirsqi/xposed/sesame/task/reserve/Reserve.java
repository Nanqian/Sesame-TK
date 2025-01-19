package fansirsqi.xposed.sesame.task.reserve;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import fansirsqi.xposed.sesame.entity.ReserveEntity;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.RandomUtil;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StatusUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;

public class Reserve extends ModelTask {
    private static final String TAG = Reserve.class.getSimpleName();

    @Override
    public String getName() {
        return "保护地🌏";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    @Override
    public String getIcon() {
        return "Reserve.png";
    }

    private SelectAndCountModelField reserveList;

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(reserveList = new SelectAndCountModelField("reserveList", "保护地列表", new LinkedHashMap<>(), ReserveEntity::getList));
        return modelFields;
    }

    public Boolean check() {
        return !TaskCommon.IS_ENERGY_TIME;
    }

    public void run() {
        try {
            Log.record("开始检测保护地");
            animalReserve();
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void animalReserve() {
        try {
            Log.record("开始执行-"+getName());
            String s = ReserveRpcCall.queryTreeItemsForExchange();
            if (s == null) {
                ThreadUtil.sleep(RandomUtil.delay());
                s = ReserveRpcCall.queryTreeItemsForExchange();
            }
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResCode(jo)) {
                JSONArray ja = jo.getJSONArray("treeItems");
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if (!jo.has("projectType")) {
                        continue;
                    }
                    if (!"RESERVE".equals(jo.getString("projectType"))) {
                        continue;
                    }
                    if (!"AVAILABLE".equals(jo.getString("applyAction"))) {
                        continue;
                    }
                    String projectId = jo.getString("itemId");
                    String itemName = jo.getString("itemName");
                    Map<String, Integer> map = reserveList.getValue();
                    for (Map.Entry<String, Integer> entry : map.entrySet()) {
                        if (Objects.equals(entry.getKey(), projectId)) {
                            Integer count = entry.getValue();
                            if (count != null && count > 0 && StatusUtil.canReserveToday(projectId, count)) {
                                exchangeTree(projectId, itemName, count);
                            }
                            break;
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "animalReserve err:");
            Log.printStackTrace(TAG, t);
        }finally {
            Log.record("结束执行-"+getName());
        }
    }

    private boolean queryTreeForExchange(String projectId) {
        try {
            String s = ReserveRpcCall.queryTreeForExchange(projectId);
            JSONObject jo = new JSONObject(s);
            if (ResUtil.checkResCode(jo)) {
                String applyAction = jo.getString("applyAction");
                int currentEnergy = jo.getInt("currentEnergy");
                jo = jo.getJSONObject("exchangeableTree");
                if ("AVAILABLE".equals(applyAction)) {
                    if (currentEnergy >= jo.getInt("energy")) {
                        return true;
                    } else {
                        Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#能量不足停止申请");
                        return false;
                    }
                } else {
                    Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#似乎没有了");
                    return false;
                }
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTreeForExchange err:");
            Log.printStackTrace(TAG, t);
        }
        return false;
    }

    private void exchangeTree(String projectId, String itemName, int count) {
        int appliedTimes = 0;
        try {
            String s;
            JSONObject jo;
            boolean canApply = queryTreeForExchange(projectId);
            if (!canApply)
                return;
            for (int applyCount = 1; applyCount <= count; applyCount++) {
                s = ReserveRpcCall.exchangeTree(projectId);
                jo = new JSONObject(s);
                if (ResUtil.checkResCode(jo)) {
                    int vitalityAmount = jo.optInt("vitalityAmount", 0);
                    appliedTimes = StatusUtil.getReserveTimes(projectId) + 1;
                    String str = "领保护地🏕️[" + itemName + "]#第" + appliedTimes + "次"
                            + (vitalityAmount > 0 ? "-活力值+" + vitalityAmount : "");
                    Log.forest(str);
                    StatusUtil.reserveToday(projectId, 1);
                } else {
                    Log.record(jo.getString("resultDesc"));
                    Log.runtime(jo.toString());
                    Log.forest("领保护地🏕️[" + itemName + "]#发生未知错误，停止申请");
                    // StatisticsUtil.reserveToday(projectId, count);
                    break;
                }
                ThreadUtil.sleep(300);
                canApply = queryTreeForExchange(projectId);
                if (!canApply) {
                    // StatisticsUtil.reserveToday(projectId, count);
                    break;
                } else {
                    ThreadUtil.sleep(300);
                }
                if (!StatusUtil.canReserveToday(projectId, count))
                    break;
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "exchangeTree err:");
            Log.printStackTrace(TAG, t);
        }
    }

}
