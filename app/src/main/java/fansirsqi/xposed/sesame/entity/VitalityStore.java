package fansirsqi.xposed.sesame.entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.VitalityRewardsMap;
import lombok.Getter;

/**
 * @author Byseven
 * @date 2025/1/20
 * @apiNote
 */
public class VitalityStore extends MapperEntity {
    public VitalityStore(String i, String n) {
        this.id = i;
        this.name = n;
    }
    public static List<VitalityStore> getList() {
        List<VitalityStore> list = new ArrayList<>();
        Map<String, String> idSet = IdMapManager.getInstance(VitalityRewardsMap.class).getMap();
        for (Map.Entry<String, String> entry : idSet.entrySet()) {
            list.add(new VitalityStore(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    @Getter
    public enum ExchangeStatus {
        NO_ENOUGH_POINT("活力值不足"),
        NO_ENOUGH_STOCK("库存量不足"),
        REACH_LIMIT("兑换达上限"),
        SECKILL_NOT_BEGIN("秒杀未开始"),
        SECKILL_HAS_END("秒杀已结束"),
        HAS_NEVER_EXPIRE_DRESS("不限时皮肤");

        private final String nickName;

        ExchangeStatus(String nickName) {
            this.nickName = nickName;
        }

    }
}
