package fansirsqi.xposed.sesame.entity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 表示好友能量监视器的实体类，提供能量收集的统计和管理功能。
 * 该类是线程安全的，适用于多线程环境。
 *
 * @author Byseven
 * @since 2025/01/22
 */
@Setter
@Getter
public class FriendWatch extends MapperEntity {

    // 日志标签
    private static final String TAG = FriendWatch.class.getSimpleName();

    // 用于存储好友能量数据的 Map
    private static final Map<String, FriendWatch> friendWatchMap = new HashMap<>();

    // Getter 和 Setter 方法
    private String startTime; // 开始统计时间
    private int allGet; // 总收集能量
    private int weekGet; // 本周收集能量

    /**
     * 构造方法，初始化好友监视器对象。
     *
     * @param id   好友 ID
     * @param name 好友名称
     */
    public FriendWatch(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * 比较两个好友的周收能量，用于排序。
     *
     * @param o 要比较的另一个对象
     * @return 比较结果
     */
    @Override
    public int compareTo(MapperEntity o) {
        FriendWatch another = (FriendWatch) o;
        if (this.weekGet > another.weekGet) {
            return -1;
        } else if (this.weekGet < another.weekGet) {
            return 1;
        }
        return super.compareTo(o);
    }

    /**
     * 更新好友的能量收集数据。
     *
     * @param id              好友 ID
     * @param collectedEnergy 本次收集的能量
     */
    public static void friendWatch(String id, int collectedEnergy) {
        try {
            FriendWatch friendWatch = friendWatchMap.get(id);
            if (friendWatch == null) {
                friendWatch = new FriendWatch(id, UserMap.getMaskName(id));
                friendWatch.startTime = TimeUtil.getFormatDate();
                friendWatch.allGet = 0;
                friendWatch.weekGet = 0;
                friendWatchMap.put(id, friendWatch);
            }
            friendWatch.weekGet += collectedEnergy;
        } catch (Throwable th) {
            Log.runtime(TAG, "friendWatch err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 保存好友能量数据到文件。
     */
    public static synchronized void save(String userId) {
        try {
            if (userId == null) return;
            JSONObject joFriendWatch = new JSONObject(friendWatchMap);
            String formattedJson = JsonUtil.formatJson(joFriendWatch);
            Files.write2File(formattedJson, Files.getFriendWatchFile(userId));
        } catch (Exception e) {
            Log.runtime(TAG, "friendWatch save err:");
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 更新每日统计数据，如果需要更新周数据则进行重置。
     */
    public static void updateDay(String userId) {
        if (userId == null) return;
        if (!needUpdateAll(Files.getFriendWatchFile(userId).lastModified())) {
            return;
        }
        try {
            String dateStr = TimeUtil.getFormatDate();
            for (FriendWatch friendWatch : friendWatchMap.values()) {
                friendWatch.allGet += friendWatch.weekGet;
                friendWatch.weekGet = 0;
                if (friendWatch.startTime == null) {
                    friendWatch.startTime = dateStr;
                }
            }
            save(userId);
        } catch (Throwable th) {
            Log.runtime(TAG, "friendWatch updateDay err:");
            Log.printStackTrace(TAG, th);
        }
    }

    /**
     * 加载好友能量数据。
     *
     * @return 加载是否成功
     */
    public static synchronized Boolean load(String userId) {
        try {
            if (userId == null) return false;
            String strFriendWatch = Files.readFromFile(Files.getFriendWatchFile(userId));
            JSONObject joFriendWatch = strFriendWatch.isEmpty() ? new JSONObject() : new JSONObject(strFriendWatch);
            friendWatchMap.clear();
            Iterator<String> ids = joFriendWatch.keys();
            while (ids.hasNext()) {
                String id = ids.next();
                JSONObject friend = joFriendWatch.getJSONObject(id);
                FriendWatch friendWatch = new FriendWatch(id, friend.optString("name"));
                friendWatch.startTime = friend.optString("startTime", "无");
                friendWatch.weekGet = friend.optInt("weekGet", 0);
                friendWatch.allGet = friend.optInt("allGet", 0);
                friendWatchMap.put(id, friendWatch);
            }
            return true;
        } catch (JSONException e) {
            Log.printStackTrace(e);
            friendWatchMap.clear();
        }
        return false;
    }

    /**
     * 卸载好友能量数据，清空内存中的缓存。
     */
    public static synchronized void unload() {
        friendWatchMap.clear();
    }

    /**
     * 判断是否需要更新所有数据（周一重置）。
     *
     * @param last 上次更新时间的时间戳
     * @return 是否需要更新
     */
    public static boolean needUpdateAll(long last) {
        if (last == 0L) {
            return true;
        }
        Calendar cLast = Calendar.getInstance();
        cLast.setTimeInMillis(last);
        Calendar cNow = Calendar.getInstance();
        return cLast.get(Calendar.DAY_OF_YEAR) != cNow.get(Calendar.DAY_OF_YEAR) &&
                cNow.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
    }

    /**
     * 获取好友能量列表，用于展示。
     *
     * @return 包含所有好友能量数据的列表
     */
    public static List<FriendWatch> getList() {
        return new ArrayList<>(friendWatchMap.values());
    }

}
