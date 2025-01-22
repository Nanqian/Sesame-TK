package fansirsqi.xposed.sesame.hook.rpc.bridge;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.General;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.RandomUtil;

/**
 * 新版 RPC 接口，支持最低支付宝版本 v10.3.96.8100 记录 RPC 抓包，支持最低支付宝版本 v10.3.96.8100
 */
public class NewRpcBridge implements RpcBridge {

    private static final String TAG = NewRpcBridge.class.getSimpleName();

    private ClassLoader loader;
    private Object newRpcInstance;
    private Method parseObjectMethod;
    private Class<?>[] bridgeCallbackClazzArray;
    private Method newRpcCallMethod;

    @Override
    public RpcVersion getVersion() {
        return RpcVersion.NEW;
    }

    @Override
    public void load() throws Exception {
        loader = ApplicationHook.getClassLoader();
        try {
            // 获取 RpcBridgeExtension 实例
            newRpcInstance = getRpcBridgeExtensionInstance();
            if (newRpcInstance == null) {
                throw new RuntimeException("Failed to initialize RpcBridgeExtension instance");
            }

            // 加载必要的方法和类
            parseObjectMethod = loader.loadClass("com.alibaba.fastjson.JSON").getMethod("parseObject", String.class);
            Class<?> bridgeCallbackClazz = loader.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback");
            bridgeCallbackClazzArray = new Class[]{bridgeCallbackClazz};
            newRpcCallMethod = newRpcInstance.getClass().getMethod("rpc"
                    , String.class
                    , boolean.class
                    , boolean.class
                    , String.class
                    , loader.loadClass(General.JSON_OBJECT_NAME)
                    , String.class
                    , loader.loadClass(General.JSON_OBJECT_NAME)
                    , boolean.class
                    , boolean.class
                    , int.class
                    , boolean.class
                    , String.class
                    , loader.loadClass("com.alibaba.ariver.app.api.App")
                    , loader.loadClass("com.alibaba.ariver.app.api.Page")
                    , loader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext")
                    , bridgeCallbackClazz
            );
            Log.runtime(TAG, "RpcBridge initialized successfully");
        } catch (Exception e) {
            Log.error(TAG, "Failed to initialize RpcBridge: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void unload() {
        newRpcCallMethod = null;
        bridgeCallbackClazzArray = null;
        parseObjectMethod = null;
        newRpcInstance = null;
        loader = null;
    }

    @Override
    public String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        RpcEntity resRpcEntity = requestObject(rpcEntity, tryCount, retryInterval);
        if (resRpcEntity != null) {
            return resRpcEntity.getResponseString();
        }
        return null;
    }

    @Override
    public RpcEntity requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        if (ApplicationHook.isOffline()) {
            Log.error(TAG, "Application is offline, skipping RPC request");
            return null;
        }

        try {
            int count = 0;
            while (count < tryCount) {
                count++;
                try {
                    RpcIntervalLimit.enterIntervalLimit(rpcEntity.getRequestMethod());
                    Object requestData = rpcEntity.getRpcFullRequestData();
                    if (requestData == null) {
                        Log.error(TAG, "RPC request data is null");
                        return null;
                    }

                    Object jsonObject = parseObjectMethod.invoke(null, requestData);
                    if (jsonObject == null) {
                        Log.error(TAG, "Failed to parse RPC request data");
                        return null;
                    }

                    // 发起 RPC 请求
                    newRpcCallMethod.invoke(newRpcInstance, rpcEntity.getRequestMethod(), false, false, "json", jsonObject, "", null, true, false, 0, false, "", null, null, null,
                            Proxy.newProxyInstance(loader, bridgeCallbackClazzArray, (proxy, innerMethod, args) ->
                                    handleProxyMethod(rpcEntity, innerMethod, args, proxy)) // 传递 proxy 参数
                    );

                    // 检查响应结果
                    if (!rpcEntity.getHasResult()) {
                        Log.debug(TAG, "RPC request has no result");
                        return null;
                    }
                    if (!rpcEntity.getHasError()) {
                        return rpcEntity;
                    }

                    // 处理错误响应
                    String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
                    if ("2000".equals(errorCode)) {
                        handleLoginTimeout();
                        return null;
                    }
                    return rpcEntity;
                } catch (Throwable t) {
                    Log.error(TAG, "RPC request failed (attempt " + count + "): " + t.getMessage());
                    if (retryInterval > 0) {
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            Thread.sleep(600 + RandomUtil.delay());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return null;
        } finally {
            Log.system(TAG, "New RPC\n方法: " + rpcEntity.getRequestMethod() + "\n参数: " + rpcEntity.getRequestData() + "\n数据: " + rpcEntity.getResponseString() + "\n");
        }
    }

    /**
     * 获取 RpcBridgeExtension 实例
     */
    private Object getRpcBridgeExtensionInstance() throws Exception {
        Object service = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", loader), "getService");
        Object extensionManager = XposedHelpers.callMethod(service, "getExtensionManager");
        Method getExtensionByName = extensionManager.getClass().getDeclaredMethod("createExtensionInstance", Class.class);
        getExtensionByName.setAccessible(true);
        Object instance = getExtensionByName.invoke(null, loader.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"));

        if (instance == null) {
            Object nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap");
            if (nodeExtensionMap != null) {
                @SuppressWarnings("unchecked")
                Map<Object, Map<String, Object>> map = (Map<Object, Map<String, Object>>) nodeExtensionMap;
                for (Map.Entry<Object, Map<String, Object>> entry : map.entrySet()) {
                    Map<String, Object> map1 = entry.getValue();
                    for (Map.Entry<String, Object> entry1 : map1.entrySet()) {
                        if ("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension".equals(entry1.getKey())) {
                            instance = entry1.getValue();
                            break;
                        }
                    }
                }
            }
        }

        if (instance == null) {
            Log.error(TAG, "Failed to get RpcBridgeExtension instance");
        }
        return instance;
    }

    /**
     * 处理动态代理方法调用
     */
    private Object handleProxyMethod(RpcEntity rpcEntity, Method method, Object[] args, Object proxy) {
        return switch (method.getName()) {
            case "equals" -> args != null && args.length > 0 && proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Proxy for " + bridgeCallbackClazzArray[0].getName();
            case "sendJSONResponse" -> handleSendJSONResponse(rpcEntity, args);
            default -> null;
        };
    }

    /**
     * 处理 sendJSONResponse 方法
     */
    private boolean handleSendJSONResponse(RpcEntity rpcEntity, Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            Log.error(TAG, "sendJSONResponse: args is null or empty");
            return false;
        }

        try {
            Object obj = args[0];
            String jsonString = (String) XposedHelpers.callMethod(obj, "toJSONString");
            rpcEntity.setResponseObject(obj, jsonString);

            boolean isSuccess = (Boolean) XposedHelpers.callMethod(obj, "containsKey", "success")
                    || (Boolean) XposedHelpers.callMethod(obj, "containsKey", "isSuccess");

            if (!isSuccess) {
                rpcEntity.setError();
                Log.error(TAG, "RPC response error: " + jsonString);
                return false;
            }
            return true;
        } catch (Exception e) {
            rpcEntity.setError();
            Log.error(TAG, "Failed to handle RPC response: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理登录超时
     */
    private void handleLoginTimeout() {
        if (!ApplicationHook.isOffline()) {
            ApplicationHook.setOffline(true);
            Notify.updateStatusText("登录超时");
            if (BaseModel.getTimeoutRestart().getValue()) {
                Log.record("尝试重新登录");
                ApplicationHook.reLoginByBroadcast();
            }
        }
    }
}