package fansirsqi.xposed.sesame.util;

import org.json.JSONException;
import org.json.JSONObject;

public class ResUtil {
    private static final String TAG = ResUtil.class.getSimpleName();

    /**
     * 检查JSON对象中的memo字段
     *
     * @param tag 标签
     * @param jo  JSON对象
     * @return 如果memo字段值为SUCCESS返回true，否则返回false
     */
    public static Boolean checkMemo(String tag, JSONObject jo) {
        try {
            if (!"SUCCESS".equals(jo.optString("memo"))) {
                Log.error(tag, "checkMemo err:  " + jo);
                return false;
            }
            return true;
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "checkMemo err:", t);
        }
        return false;
    }


    public static Boolean checkResultCode(JSONObject jo) {
        return checkResultCode(TAG, jo);
    }

    /**
     * 检查JSON对象中的响应码（resultCode），并根据其类型和值返回处理结果。
     *
     * <p>
     * 该方法首先尝试从JSON对象中获取resultCode字段，然后根据其类型（整数或字符串）进行检查。
     * 如果resultCode的值符合预期（整数类型为200，字符串类型为"SUCCESS"或"100"），则返回true。
     * 如果resultCode不存在、类型不匹配或值不符合预期，将记录错误信息并返回false。
     * 如果在处理过程中发生异常，将捕获异常并记录错误信息，然后返回false。
     *
     * @param TAG 用来标识日志的标签
     * @param jo  包含resultCode的JSON对象
     * @return 如果resultCode符合预期，则返回true；否则返回false
     */
    public static Boolean checkResultCode(String TAG, JSONObject jo) {
        try {
            Object resCode = jo.opt("resultCode");
            if (resCode == null) {
                Log.error(TAG + "checkResCode err: resultCode不存在: " + jo);
                return false;
            }
            if (resCode instanceof Integer) {
                if ((Integer) resCode != 200) {
                    Log.error(TAG + "checkResCode err: resultCode!= 200: " + jo);
                    return false;
                }
                return true;
            } else if (resCode instanceof String) {
                if (!((String) resCode).matches("(?i)SUCCESS|100")) {
                    Log.error(TAG + "checkResCode err: resultCode!= SUCCESS|100 :" + jo);
                    return false;
                }
                return true;
            }
            Log.record(TAG + "checkResCode Type fail: " + jo);
            return false;
        } catch (Throwable t) {
            Log.runtime(TAG, "checkResCode error: " + t.getMessage());
        }
        return false;
    }

    /**
     * 检查JSON对象中的success字段，并根据其值返回处理结果。
     *
     * @param jo 包含success/isSuccess字段的JSON对象
     * @return 如果success/isSuccess字段值为true，则返回true；否则返回false
     */
    public static Boolean checkSuccess(JSONObject jo) {

        if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
            return true; // 任意一个为 true 就算成功
        } else {
            Log.error("checkSuccess err: " + jo);
            return false; // 否则失败
        }
    }

    /**
     * 先将String转换为JSONObject，然后调用checkSuccess
     * 检查JSON对象中的success字段，并根据其值返回处理结果。
     *
     * @param s 包含success/isSuccess字段的JSON对象
     * @return 如果success/isSuccess字段值为true，则返回true；否则返回false
     */
    public static Boolean checkSuccess(String s) throws JSONException {
        JSONObject jo = new JSONObject(s);
        return checkSuccess(jo);
    }

}
