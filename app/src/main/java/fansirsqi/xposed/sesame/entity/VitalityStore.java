package fansirsqi.xposed.sesame.entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.VitalityRewardsMap;
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
    public enum ExchangeStatus {
        InsufficientVitalityValue("活力值不足"),
        InsufficientInventory("库存量不足"),
        ExceedLimit("兑换达上限"),
        SeckillNotBegin("秒杀未开始"),
        SeckillHasEnd("秒杀已结束"),
        NeverExpireDress("不限时皮肤");
        ExchangeStatus(String nickNames) {
        }
    }
}
