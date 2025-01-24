package fansirsqi.xposed.sesame.entity;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
/**
 * 表示一个能量收集实体，包含用户信息及操作相关的状态。
 */
@Getter
public class CollectEnergyEntity {
    // 用户 ID
    private final String userId;
    // 用户主页 JSON 对象
    @Setter
    private JSONObject userHome;
    // RPC 请求实体
    @Setter
    private RpcEntity rpcEntity;
    // 收集次数
    private Integer collectCount = 0;
    // 尝试次数
    private Integer tryCount = 0;
    // 是否需要翻倍
    @Setter
    private Boolean needDouble = false;
    // 是否需要重试
    @Setter
    private Boolean needRetry = false;
    /**
     * 构造方法，仅指定用户 ID。
     * @param userId 用户 ID
     */
    public CollectEnergyEntity(String userId) {
        this.userId = userId;
    }
    /**
     * 构造方法，指定用户 ID 和用户主页信息。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     */
    public CollectEnergyEntity(String userId, JSONObject userHome) {
        this.userId = userId;
        this.userHome = userHome;
    }
    /**
     * 构造方法，指定用户 ID、用户主页信息及 RPC 请求实体。
     * @param userId 用户 ID
     * @param userHome 用户主页 JSON 对象
     * @param rpcEntity RPC 请求实体
     */
    public CollectEnergyEntity(String userId, JSONObject userHome, RpcEntity rpcEntity) {
        this.userId = userId;
        this.userHome = userHome;
        this.rpcEntity = rpcEntity;
    }
    /**
     * 增加尝试次数。
     * @return 更新后的尝试次数
     */
    public Integer addTryCount() {
        this.tryCount += 1;
        return tryCount;
    }
    /**
     * 重置尝试次数为 0。
     */
    public void resetTryCount() {
        this.tryCount = 0;
    }
    /**
     * 设置需要翻倍，并增加收集次数。
     */
    public void setNeedDouble() {
        this.collectCount += 1;
        this.needDouble = true;
    }
    /**
     * 取消需要翻倍状态。
     */
    public void unsetNeedDouble() {
        this.needDouble = false;
    }
    /**
     * 设置需要重试状态。
     */
    public void setNeedRetry() {
        this.needRetry = true;
    }
    /**
     * 取消需要重试状态。
     */
    public void unsetNeedRetry() {
        this.needRetry = false;
    }
}
