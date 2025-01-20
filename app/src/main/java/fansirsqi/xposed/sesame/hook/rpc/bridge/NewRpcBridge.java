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
 * 新版rpc接口，支持最低支付宝版本v10.3.96.8100 记录rpc抓包，支持最低支付宝版本v10.3.96.8100
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
            Object service = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", loader), "getService");
            Object extensionManager = XposedHelpers.callMethod(service, "getExtensionManager");
            Method getExtensionByName = extensionManager.getClass().getDeclaredMethod("createExtensionInstance", Class.class);
            getExtensionByName.setAccessible(true);
            newRpcInstance = getExtensionByName.invoke(null, loader.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"));
            if (newRpcInstance == null) {
                Object nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap");
                if (nodeExtensionMap != null) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Map<String, Object>> map = (Map<Object, Map<String, Object>>) nodeExtensionMap;
                    for (Map.Entry<Object, Map<String, Object>> entry : map.entrySet()) {
                        Map<String, Object> map1 = entry.getValue();
                        for (Map.Entry<String, Object> entry1 : map1.entrySet()) {
                            if ("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension".equals(entry1.getKey())) {
                                newRpcInstance = entry1.getValue();
                                break;
                            }
                        }
                    }
                }
                if (newRpcInstance == null) {
                    Log.runtime(TAG, "get newRpcInstance null");
                    throw new RuntimeException("get newRpcInstance is null");
                }
            }
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
            Log.runtime(TAG, "get newRpcCallMethod successfully");
        } catch (Exception e) {
            Log.runtime(TAG, "get newRpcCallMethod err:");
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
            return null;
        }
        try {
            int count = 0;
            do {
                count++;
                try {
                    RpcIntervalLimit.enterIntervalLimit(rpcEntity.getRequestMethod());
                    newRpcCallMethod.invoke(
                            newRpcInstance, rpcEntity.getRequestMethod(), false, false, "json", parseObjectMethod.invoke(null, rpcEntity.getRpcFullRequestData()), "", null, true, false, 0, false, "", null, null, null, Proxy.newProxyInstance(loader, bridgeCallbackClazzArray, (proxy, innerMethod, args) -> {
                                if (args.length == 1 && "sendJSONResponse".equals(innerMethod.getName())) {
                                    try {
                                        Object obj = args[0];
                                        rpcEntity.setResponseObject(obj, (String) XposedHelpers.callMethod(obj, "toJSONString"));
                                        if (!(Boolean) XposedHelpers.callMethod(obj, "containsKey", "success")
                                                && !(Boolean) XposedHelpers.callMethod(obj, "containsKey", "isSuccess")) {
                                            rpcEntity.setError();
                                            Log.error("new rpc response | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.getRequestMethod() + " args: " + rpcEntity.getRequestData() + " | data: " + rpcEntity.getResponseString());
                                        }
                                    } catch (Exception e) {
                                        rpcEntity.setError();
                                        Log.error("new rpc response | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.getRequestMethod() + " err:");
                                        Log.printStackTrace(e);
                                    }
                                }
                                return null;
                            })
                    );
                    if (!rpcEntity.getHasResult()) {
                        return null;
                    }
                    if (!rpcEntity.getHasError()) {
                        return rpcEntity;
                    }
                    try {
                        String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
                        if ("2000".equals(errorCode)) {
                            if (!ApplicationHook.isOffline()) {
                                ApplicationHook.setOffline(true);
                                Notify.updateStatusText("登录超时");
                                if (BaseModel.getTimeoutRestart().getValue()) {
                                    Log.record("尝试重新登录");
                                    ApplicationHook.reLoginByBroadcast();
                                }
                            }
                            return null;
                        }
                        return rpcEntity;
                    } catch (Exception e) {
                        Log.error("new rpc response | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.getRequestMethod() + " get err:");
                        Log.printStackTrace(e);
                    }
                    if (retryInterval < 0) {
                        try {
                            Thread.sleep(600 + RandomUtil.delay());
                        } catch (InterruptedException e) {
                            Log.printStackTrace(e);
                        }
                    } else if (retryInterval > 0) {
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            Log.printStackTrace(e);
                        }
                    }
                }
                catch (Throwable t) {
                    Log.error("new rpc request | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.getRequestMethod() + " err:");
                    Log.printStackTrace(t);
                    if (retryInterval < 0) {
                        try {
                            Thread.sleep(600 + RandomUtil.delay());
                        } catch (InterruptedException e) {
                            Log.printStackTrace(e);
                        }
                    } else if (retryInterval > 0) {
                        try {
                            Thread.sleep(retryInterval);
                        } catch (InterruptedException e) {
                            Log.printStackTrace(e);
                        }
                    }
                }
            } while (count < tryCount);
            return null;
        } finally {
            Log.system(TAG, "New RPC\n方法: " + rpcEntity.getRequestMethod() + "\n参数: " + rpcEntity.getRequestData() + "\n数据: " + rpcEntity.getResponseString() + "\n");
        }
    }
}
