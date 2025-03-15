package fansirsqi.xposed.sesame.task.antForest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import fansirsqi.xposed.sesame.data.Status;
import fansirsqi.xposed.sesame.util.Log;

public class Privilege {
    public static final String TAG = Privilege.class.getSimpleName();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final List<List<String>> YOUTH_PRIVILEGE_TASKS = Arrays.asList(
            Arrays.asList("DNHZ_SL_college", "DAXUESHENG_SJK", "双击卡"),
            Arrays.asList("DXS_BHZ", "NENGLIANGZHAO_20230807", "保护罩"),
            Arrays.asList("DXS_JSQ", "JIASUQI_20230808", "加速器")
    );

    static boolean youthPrivilege() {
        try {
            if (!Status.canYouthPrivilegeToday()) return false;

            boolean allSuccessful = true;
            for (List<String> task : YOUTH_PRIVILEGE_TASKS) {
                String queryParam = task.get(0);
                String receiveParam = task.get(1);
                String taskName = task.get(2);

                JsonNode queryResult = JSON_MAPPER.readTree(AntForestRpcCall.queryTaskListV2(queryParam));

                JsonNode taskInfoListNode = getNestedNode(queryResult, "forestTasksNew", "0", "taskInfoList");
                if (taskInfoListNode == null) {
                    Log.forest("青春特权🌸[" + taskName + "]任务列表未找到");
                    allSuccessful = false;
                    continue;
                }

                ArrayNode taskInfoList = (ArrayNode) taskInfoListNode;
                for (JsonNode taskInfo : taskInfoList) {
                    if (!handlePrivilegeTask(taskInfo, receiveParam, taskName)) {
                        allSuccessful = false;
                    }
                }
            }

            if (allSuccessful) {
                Status.setYouthPrivilegeToday();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.runtime(AntForest.TAG, "youthPrivilege err:");
            Log.printStackTrace(AntForest.TAG, e);
            return false;
        }
    }

    private static boolean handlePrivilegeTask(JsonNode taskInfo, String receiveParam, String taskName) {
        JsonNode taskBaseInfo = taskInfo.get("taskBaseInfo");
        if (taskBaseInfo == null) return false;

        String taskType = taskBaseInfo.get("taskType").asText();
        if (!taskType.equals(receiveParam)) return true;

        String taskStatus = taskBaseInfo.get("taskStatus").asText();
        if ("RECEIVED".equals(taskStatus)) {
            Log.forest("青春特权🌸[" + taskName + "]已领取");
        } else if ("FINISHED".equals(taskStatus)) {
            try {
                JsonNode receiveResult = JSON_MAPPER.readTree(AntForestRpcCall.receiveTaskAwardV2(receiveParam));
                String resultDesc = receiveResult.get("desc").asText();
                Log.forest("青春特权🌸[" + taskName + "]领取结果：" + resultDesc);
                return "处理成功".equals(resultDesc);
            } catch (Exception e) {
                Log.error("青春特权🌸[" + taskName + "]领取异常：" + e.getMessage());
                return false;
            }
        }
        return false;
    }

    static void studentSignInRedEnvelope() {
        try {
            Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            final int START_HOUR = 5;
            final int END_HOUR = 10;

            if (currentHour < START_HOUR) {
                Log.forest("青春特权🧧5点前不执行签到");
                return;
            }

            String tag = currentHour < END_HOUR ? "double" : "single";
            studentTaskHandle(tag);
        } catch (Exception e) {
            Log.runtime(TAG, "student SignInRedEnvelope错误:");
            Log.printStackTrace(TAG, e);
        }
    }

    static void studentTask(String tag) {
        try {
            String result = AntForestRpcCall.studentCheckin();
            if (result == null || result.isEmpty()) {
                Log.record("青春特权🧧签到失败：返回数据为空");
                return;
            }

            JsonNode resultJson = JSON_MAPPER.readTree(result);
            String resultCode = resultJson.get("resultCode").asText();
            if (!"SUCCESS".equals(resultCode)) {
                String resultDesc = resultJson.get("resultDesc").asText("未知错误");
                Log.forest("青春特权🧧签到失败: " + resultDesc);
                return;
            }

            String resultDesc = resultJson.get("resultDesc").asText("签到成功");
            Log.forest("青春特权🧧" + tag + "：" + resultDesc);
            Status.setStudentTaskToday();
        } catch (Exception e) {
            Log.runtime(TAG, "studentTask 异常: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    private static void studentTaskHandle(String tag) {
        try {
            if (!Status.canStudentTask()) {
                Log.record("青春特权🧧今日已达上限");
                return;
            }

            String response = AntForestRpcCall.studentQqueryCheckInModel();
            if (response == null || response.isEmpty()) {
                Log.record("青春特权🧧查询失败：返回数据为空");
                return;
            }

            JsonNode responseJson = JSON_MAPPER.readTree(response);
            String resultCode = responseJson.get("resultCode").asText("");
            if (!"SUCCESS".equals(resultCode)) {
                Log.record("青春特权🧧查询失败: " + responseJson.get("resultDesc").asText(""));
                return;
            }

            JsonNode studentCheckInInfo = responseJson.get("studentCheckInInfo");
            if (studentCheckInInfo == null) {
                Log.record("青春特权🧧查询失败：无签到信息");
                return;
            }

            String action = studentCheckInInfo.get("action").asText("");
            if (action.isEmpty()) {
                Log.record("青春特权🧧查询失败：无操作信息");
                return;
            }

            if ("DO_TASK".equals(action)) {
                Log.record("青春特权🧧今日已签到");
                Status.setStudentTaskToday();
            } else {
                studentTask(tag);
            }
        } catch (Exception e) {
            Log.runtime(TAG, "student TaskHandle 异常: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    private static JsonNode getNestedNode(JsonNode root, String... paths) {
        JsonNode node = root;
        for (String path : paths) {
            node = node.get(path);
            if (node == null) break;
        }
        return node;
    }
}