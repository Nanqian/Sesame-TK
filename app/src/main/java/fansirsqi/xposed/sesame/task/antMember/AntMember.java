package fansirsqi.xposed.sesame.task.antMember;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StatusUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class AntMember extends ModelTask {
  private static final String TAG = AntMember.class.getSimpleName();

  @Override
  public String getName() {
    return "会员🏆";
  }

  @Override
  public ModelGroup getGroup() {
    return ModelGroup.MEMBER;
  }

  @Override
  public String getIcon() {
    return "AntMember.png";
  }

  private BooleanModelField memberSign;
  private BooleanModelField memberTask;
  private BooleanModelField collectSesame;
  private BooleanModelField collectSesameWithOneClick;
  private BooleanModelField sesameTask;
  private BooleanModelField collectInsuredGold;
  private BooleanModelField enableGoldTicket;
  private BooleanModelField enableGameCenter;
  private BooleanModelField merchantSign;
  private BooleanModelField merchantKmdk;
  private BooleanModelField merchantMoreTask;
  private BooleanModelField beanSignIn;
  private BooleanModelField beanExchangeBubbleBoost;

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberTask = new BooleanModelField("memberTask", "会员任务", false));
    modelFields.addField(sesameTask = new BooleanModelField("sesameTask", "芝麻信用|芝麻粒信用任务", false));
    modelFields.addField(collectSesame = new BooleanModelField("collectSesame", "芝麻信用|芝麻粒领取", false));
    modelFields.addField(collectSesameWithOneClick = new BooleanModelField("collectSesameWithOneClick", "芝麻信用|芝麻粒领取使用一键收取", false));
    modelFields.addField(collectInsuredGold = new BooleanModelField("collectInsuredGold", "蚂蚁保|保障金领取", false));
    modelFields.addField(enableGoldTicket = new BooleanModelField("enableGoldTicket", "黄金票签到", false));
    modelFields.addField(enableGameCenter = new BooleanModelField("enableGameCenter", "游戏中心签到", false));
    modelFields.addField(merchantSign = new BooleanModelField("merchantSign", "商家服务|签到", false));
    modelFields.addField(merchantKmdk = new BooleanModelField("merchantKmdk", "商家服务|开门打卡", false));
    modelFields.addField(merchantMoreTask = new BooleanModelField("merchantMoreTask", "商家服务|积分任务", false));
    modelFields.addField(beanSignIn = new BooleanModelField("beanSignIn", "安心豆签到", false));
    modelFields.addField(beanExchangeBubbleBoost = new BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false));
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
      if (memberSign.getValue()) {
        doMemberSign();
      }
      if (memberTask.getValue()) {
        doAllMemberAvailableTask();
      }
      if ((sesameTask.getValue() || collectSesame.getValue()) && checkSesameCanRun()) {
        if (sesameTask.getValue()) {
          doAllAvailableSesameTask();
        }
        if (collectSesame.getValue()) {
          collectSesame(collectSesameWithOneClick.getValue());
        }
      }
      if (collectInsuredGold.getValue()) {
        collectInsuredGold();
      }
      if (enableGoldTicket.getValue()) {
        goldTicket();
      }
      if (enableGameCenter.getValue()) {
        enableGameCenter();
      }
      if (beanSignIn.getValue()) {
        beanSignIn();
      }
      if (beanExchangeBubbleBoost.getValue()) {
        beanExchangeBubbleBoost();
      }
      if (merchantSign.getValue() || merchantKmdk.getValue() || merchantMoreTask.getValue()) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.transcodeCheck());
        if (!jo.optBoolean("success")) {
          return;
        }
        JSONObject data = jo.getJSONObject("data");
        if (!data.optBoolean("isOpened")) {
          Log.record("商家服务👪未开通");
          return;
        }
        if (merchantKmdk.getValue()) {
          if (TimeUtil.isNowAfterTimeStr("0600") && TimeUtil.isNowBeforeTimeStr("1200")) {
            kmdkSignIn();
          }
          kmdkSignUp();
        }
        if (merchantSign.getValue()) {
          doMerchantSign();
        }
        if (merchantMoreTask.getValue()) {
          doMerchantMoreTask();
        }
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }finally {
      Log.other("执行结束-" + getName());
    }
  }

  /**
   * 会员签到
   */
  private void doMemberSign() {
    try {
      if (StatusUtil.canMemberSignInToday(UserMap.getCurrentUid())) {
        String s = AntMemberRpcCall.queryMemberSigninCalendar();
        ThreadUtil.sleep(500);
        JSONObject jo = new JSONObject(s);
        if (ResUtil.checkResCode(jo)) {
          Log.other("会员签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString("signinSumDay") + "天");
          StatusUtil.memberSignInToday(UserMap.getCurrentUid());
        } else {
          Log.record(jo.getString("resultDesc"));
          Log.runtime(s);
        }
      }
      queryPointCert(1, 8);
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员任务-逛一逛
   * 单次执行 1
   */
  private void doAllMemberAvailableTask() {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      ThreadUtil.sleep(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResUtil.checkResCode(jsonObject)) {
        Log.error(TAG + ".doAllMemberAvailableTask", "会员任务响应失败: " + jsonObject.getString("resultDesc"));
        return;
      }
      if (!jsonObject.has("availableTaskList")) {
        return;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      if (taskList.length() == 0) {
        return;
      }
      JSONObject task = taskList.getJSONObject(0);
      ThreadUtil.sleep(16000);
      processTask(task);
    } catch (Throwable t) {
      Log.runtime(TAG, "doAllMemberAvailableTask err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员积分收取
   * @param page 第几页
   * @param pageSize 每页数据条数
   */
  private static void queryPointCert(int page, int pageSize) {
    try {
      String s = AntMemberRpcCall.queryPointCert(page, pageSize);
      ThreadUtil.sleep(500);
      JSONObject jo = new JSONObject(s);
      if (ResUtil.checkResCode(jo)) {
        boolean hasNextPage = jo.getBoolean("hasNextPage");
        JSONArray jaCertList = jo.getJSONArray("certList");
        for (int i = 0; i < jaCertList.length(); i++) {
          jo = jaCertList.getJSONObject(i);
          String bizTitle = jo.getString("bizTitle");
          String id = jo.getString("id");
          int pointAmount = jo.getInt("pointAmount");
          s = AntMemberRpcCall.receivePointByUser(id);
          jo = new JSONObject(s);
          if (ResUtil.checkResCode(jo)) {
            Log.other("会员积分🎖️[领取" + bizTitle + "]#" + pointAmount + "积分");
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(s);
          }
        }
        if (hasNextPage) {
          queryPointCert(page + 1, pageSize);
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "queryPointCert err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 检查是否满足运行芝麻信用任务的条件
   * @return bool
   */
  private static Boolean checkSesameCanRun() {
    try {
      String s = AntMemberRpcCall.queryHome();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.other(TAG + ".checkSesameCanRun.queryHome", jo.optString("errorMsg"));
        return false;
      }
      JSONObject entrance = jo.getJSONObject("entrance");
      if (!entrance.optBoolean("openApp")) {
        Log.other("芝麻信用💳[未开通芝麻信用]");
        return false;
      }
      return true;
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".checkSesameCanRun", t);
      return false;
    }
  }

  /**
   * 芝麻信用任务
   */
  private static void doAllAvailableSesameTask() {
    try {
      String s = AntMemberRpcCall.queryAvailableSesameTask();
      ThreadUtil.sleep(500);
      JSONObject jo = new JSONObject(s);
      if (jo.has("resData")) {
        jo = jo.getJSONObject("resData");
      }
      if (!jo.optBoolean("success")) {
        Log.other(TAG + ".doAllAvailableSesameTask.queryAvailableSesameTask", jo.getString("resultCode"));
        return;
      }
      JSONObject taskObj = jo.getJSONObject("data");
      if (taskObj.has("dailyTaskListVO")) {
        joinAndFinishSesameTask(taskObj.getJSONObject("dailyTaskListVO").getJSONArray("waitCompleteTaskVOS"));
        joinAndFinishSesameTask(taskObj.getJSONObject("dailyTaskListVO").getJSONArray("waitJoinTaskVOS"));
      }
      if (taskObj.has("toCompleteVOS")) {
        joinAndFinishSesameTask(taskObj.getJSONArray("toCompleteVOS"));
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".doAllAvailableSesameTask", t);
    }
  }

  /**
   * 芝麻信用-领取并完成任务
   * @param taskList 任务列表
   * @throws JSONException JSON解析异常，上抛处理
   */
  private static void joinAndFinishSesameTask(JSONArray taskList) throws JSONException {
    for (int i = 0; i < taskList.length(); i++) {
      JSONObject task = taskList.getJSONObject(i);
      String taskTemplateId = task.getString("templateId");
      String taskTitle = task.getString("title");
      int needCompleteNum = task.getInt("needCompleteNum");
      int completedNum = task.has("completedNum") ? task.getInt("completedNum") : 0;
      String s;
      String recordId;
      JSONObject responseObj;
      if (task.getString("actionUrl").contains("jumpAction")) {
        // 跳转APP任务 依赖跳转的APP发送请求鉴别任务完成 仅靠hook支付宝无法完成
        continue;
      }
      if (!task.has("todayFinish")) {
        // 领取任务
        s = AntMemberRpcCall.joinSesameTask(taskTemplateId);
        ThreadUtil.sleep(500);
        responseObj = new JSONObject(s);
        if (!responseObj.optBoolean("success")) {
          Log.error("芝麻信用💳[领取任务" + taskTitle + "失败]#" + s);
          continue;
        }
        recordId = responseObj.getJSONObject("data").getString("recordId");
      } else {
        if (!task.has("recordId")) {
          Log.error("芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task);
          continue;
        }
        recordId = task.getString("recordId");
      }
      s = AntMemberRpcCall.feedBackSesameTask(taskTemplateId);
      ThreadUtil.sleep(16000);
      responseObj = new JSONObject(s);
      if (!responseObj.optBoolean("success")) {
        Log.error("芝麻信用💳[任务" + taskTitle + "回调失败]#" + s);
        continue;
      }
      // 无法自动完成的任务只领取，不操作完成，否则会报错
      switch (taskTemplateId) {
        case "save_ins_universal_new": // 坚持攒保证金
        case "xiaofeijin_visit_new": // 坚持攒消费金金币
        case "xianyonghoufu_new": // 体验先用后付
          continue;
      }
      s = AntMemberRpcCall.finishSesameTask(recordId);
      ThreadUtil.sleep(4000);
      responseObj = new JSONObject(s);
      if (!responseObj.optBoolean("success")) {
        Log.error("芝麻信用💳[任务" + taskTitle + "完成失败]#" + s);
        continue;
      }
      Log.other("芝麻信用💳[" + taskTitle + "]#(" + (completedNum + 1) + "/" + needCompleteNum + "天)");
    }
  }

  /**
   * 芝麻粒收取
   * @param withOneClick 启用一键收取
   */
  private void collectSesame(Boolean withOneClick) {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryCreditFeedback());
      ThreadUtil.sleep(500);
      if (!jo.optBoolean("success")) {
        Log.other(TAG + ".collectSesame.queryCreditFeedback", jo.getString("resultView"));
        return;
      }
      JSONArray availableCollectList = jo.getJSONArray("creditFeedbackVOS");
      if (withOneClick) {
        ThreadUtil.sleep(2000);
        jo = new JSONObject(AntMemberRpcCall.collectAllCreditFeedback());
        ThreadUtil.sleep(2000);
        if (!jo.optBoolean("success")) {
          Log.other(TAG + ".collectSesame.collectAllCreditFeedback", jo.optString("resultView"));
          return;
        }
      }
      for (int i = 0; i < availableCollectList.length(); i++) {
        jo = availableCollectList.getJSONObject(i);
        if (!"UNCLAIMED".equals(jo.getString("status"))) {
          continue;
        }
        String title = jo.getString("title");
        String creditFeedbackId = jo.getString("creditFeedbackId");
        String potentialSize = jo.getString("potentialSize");
        if (!withOneClick) {
          jo = new JSONObject(AntMemberRpcCall.collectCreditFeedback(creditFeedbackId));
          ThreadUtil.sleep(2000);
          if (!jo.optBoolean("success")) {
            Log.other(TAG + ".collectSesame.collectCreditFeedback", jo.optString("resultView"));
            continue;
          }
        }
        Log.other("芝麻信用💳[" + title + "]#" + potentialSize + "粒" + (withOneClick ? "(一键收取)" : ""));
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectSesame", t);
    }
  }

  /**
   * 商家开门打卡签到
   */
  private static void kmdkSignIn() {
    try {
      String s = AntMemberRpcCall.queryActivity();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        if ("SIGN_IN_ENABLE".equals(jo.getString("signInStatus"))) {
          String activityNo = jo.getString("activityNo");
          JSONObject joSignIn = new JSONObject(AntMemberRpcCall.signIn(activityNo));
          if (joSignIn.optBoolean("success")) {
            Log.other("商家服务🏬[开门打卡签到成功]");
          } else {
            Log.record(joSignIn.getString("errorMsg"));
            Log.runtime(joSignIn.toString());
          }
        }
      } else {
        Log.record("queryActivity" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家开门打卡报名
   */
  private static void kmdkSignUp() {
    try {
      for (int i = 0; i < 5; i++) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.queryActivity());
        if (jo.optBoolean("success")) {
          String activityNo = jo.getString("activityNo");
          if (!TimeUtil.getFormatDate().replace("-", "").equals(activityNo.split("_")[2])) {
            break;
          }
          if ("SIGN_UP".equals(jo.getString("signUpStatus"))) {
            break;
          }
          if ("UN_SIGN_UP".equals(jo.getString("signUpStatus"))) {
            String activityPeriodName = jo.getString("activityPeriodName");
            JSONObject joSignUp = new JSONObject(AntMemberRpcCall.signUp(activityNo));
            if (joSignUp.optBoolean("success")) {
              Log.other("商家服务🏬[" + activityPeriodName + "开门打卡报名]");
              return;
            } else {
              Log.record(joSignUp.getString("errorMsg"));
              Log.runtime(joSignUp.toString());
            }
          }
        } else {
          Log.record("queryActivity");
          Log.runtime(jo.toString());
        }
        ThreadUtil.sleep(500);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignUp err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家积分签到
   */
  private static void doMerchantSign() {
    try {
      String s = AntMemberRpcCall.merchantSign();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.runtime(TAG, "doMerchantSign err:" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      String signResult = jo.getString("signInResult");
      String reward = jo.getString("todayReward");
      if ("SUCCESS".equals(signResult)) {
        Log.other("商家服务🏬[每日签到]#获得积分" + reward);
      } else {
        Log.record(s);
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家积分任务
   */
  private static void doMerchantMoreTask() {
    String s = AntMemberRpcCall.taskListQuery();
    try {
      boolean doubleCheck = false;
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        JSONArray taskList = jo.getJSONObject("data").getJSONArray("taskList");
        for (int i = 0; i < taskList.length(); i++) {
          JSONObject task = taskList.getJSONObject(i);
          if (!task.has("status")) {
            continue;
          }
          String title = task.getString("title");
          String reward = task.getString("reward");
          String taskStatus = task.getString("status");
          if ("NEED_RECEIVE".equals(taskStatus)) {
            if (task.has("pointBallId")) {
              jo = new JSONObject(AntMemberRpcCall.ballReceive(task.getString("pointBallId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
            }
          } else if ("PROCESSING".equals(taskStatus) || "UNRECEIVED".equals(taskStatus)) {
            if (task.has("extendLog")) {
              JSONObject bizExtMap = task.getJSONObject("extendLog").getJSONObject("bizExtMap");
              jo = new JSONObject(AntMemberRpcCall.taskFinish(bizExtMap.getString("bizId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🏬[" + title + "]#领取积分" + reward);
              }
              doubleCheck = true;
            } else {
              String taskCode = task.getString("taskCode");
              switch (taskCode) {
                case "SYH_CPC_DYNAMIC":
                  // 逛一逛商品橱窗
                  taskReceive(taskCode, "SYH_CPC_DYNAMIC_VIEWED", title);
                  break;
                case "JFLLRW_TASK":
                  // 逛一逛得缴费红包
                  taskReceive(taskCode, "JFLL_VIEWED", title);
                  break;
                case "ZFBHYLLRW_TASK":
                  // 逛一逛支付宝会员
                  taskReceive(taskCode, "ZFBHYLL_VIEWED", title);
                  break;
                case "QQKLLRW_TASK":
                  // 逛一逛支付宝亲情卡
                  taskReceive(taskCode, "QQKLL_VIEWED", title);
                  break;
                case "SSLLRW_TASK":
                  // 逛逛领优惠得红包
                  taskReceive(taskCode, "SSLL_VIEWED", title);
                  break;
                case "ELMGYLLRW2_TASK":
                  // 去饿了么果园0元领水果
                  taskReceive(taskCode, "ELMGYLL_VIEWED", title);
                  break;
                case "ZMXYLLRW_TASK":
                  // 去逛逛芝麻攒粒攻略
                  taskReceive(taskCode, "ZMXYLL_VIEWED", title);
                  break;
                case "GXYKPDDYH_TASK":
                  // 逛信用卡频道得优惠
                  taskReceive(taskCode, "xykhkzd_VIEWED", title);
                  break;
                case "HHKLLRW_TASK":
                  // 49999元花呗红包集卡抽
                  taskReceive(taskCode, "HHKLLX_VIEWED", title);
                  break;
                case "TBNCLLRW_TASK":
                  // 去淘宝芭芭农场领水果百货
                  taskReceive(taskCode, "TBNCLLRW_TASK_VIEWED", title);
                  break;
              }
            }
          }
        }
        if (doubleCheck) {
          doMerchantMoreTask();
        }
      } else {
        Log.runtime("taskListQuery err:" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskListQuery err:");
      Log.printStackTrace(TAG, t);
    } finally {
      try {
        ThreadUtil.sleep(1000);
      } catch (Exception e) {
        Log.printStackTrace(e);
      }
    }
  }

  /**
   * 完成商家积分任务
   * @param taskCode 任务代码
   * @param actionCode 行为代码
   * @param title 标题
   */
  private static void taskReceive(String taskCode, String actionCode, String title) {
    try {
      String s = AntMemberRpcCall.taskReceive(taskCode);
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        ThreadUtil.sleep(500);
        jo = new JSONObject(AntMemberRpcCall.actioncode(actionCode));
        if (jo.optBoolean("success")) {
          ThreadUtil.sleep(16000);
          jo = new JSONObject(AntMemberRpcCall.produce(actionCode));
          if (jo.optBoolean("success")) {
            Log.other("商家服务🏬[完成任务" + title + "]");
          }
        }
      } else {
        Log.record("taskReceive" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskReceive err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 保障金领取
   */
  private void collectInsuredGold() {
    try {
      String s = AntMemberRpcCall.queryAvailableCollectInsuredGold();
      ThreadUtil.sleep(200);
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.other(TAG + ".collectInsuredGold.queryInsuredHome", "保障金🏥[响应失败]#" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      JSONObject signInBall = jo.getJSONObject("signInDTO");
      JSONArray otherBallList = jo.getJSONArray("eventToWaitDTOList");
      if (1 == signInBall.getInt("sendFlowStatus") && 1 == signInBall.getInt("sendType")) {
        s = AntMemberRpcCall.collectInsuredGold(signInBall);
        ThreadUtil.sleep(2000);
        jo = new JSONObject(s);
        if (!jo.optBoolean("success")) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]#+" + gainGold + "元");
      }
      for (int i = 0; i <otherBallList.length(); i++) {
        JSONObject anotherBall = otherBallList.getJSONObject(i);
        s = AntMemberRpcCall.collectInsuredGold(anotherBall);
        ThreadUtil.sleep(2000);
        jo = new JSONObject(s);
        if (!jo.optBoolean("success")) {
          Log.other(TAG + ".collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#" + s);
          return;
        }
        String gainGold = jo.getJSONObject("data").getJSONObject("gainSumInsuredDTO").getString("gainSumInsuredYuan");
        Log.other("保障金🏥[领取保证金]+" + gainGold + "元");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG + ".collectInsuredGold", t);
    }
  }

  /**
   * 执行会员任务 类型1
   * @param task 单个任务对象
   */
  private void processTask(JSONObject task) throws JSONException {
    JSONObject taskConfigInfo = task.getJSONObject("taskConfigInfo");
    String name = taskConfigInfo.getString("name");
    Long id = taskConfigInfo.getLong("id");
    String awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint");
    String targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0);
    String[] targetBusinessArray = targetBusiness.split("#");
    if (targetBusinessArray.length < 3) {
      Log.runtime(TAG, "processTask target param err:" + Arrays.toString(targetBusinessArray));
      return;
    }
    String bizType = targetBusinessArray[0];
    String bizSubType = targetBusinessArray[1];
    String bizParam = targetBusinessArray[2];
    String str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id);
    ThreadUtil.sleep(500);
    JSONObject jo = new JSONObject(str);
    if (!ResUtil.checkResCode(jo)) {
      Log.runtime(TAG, "执行任务失败:" + jo.optString("resultDesc"));
      return;
    }
    Log.other("会员任务🎖️[" + name + "]#获得积分" + awardParamPoint);
  }

  public void kbMember() {
    try {
      if (!StatusUtil.canKbSignInToday()) {
        return;
      }
      String s = AntMemberRpcCall.rpcCall_signIn();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success", false)) {
        jo = jo.getJSONObject("data");
        Log.other("口碑签到📅[第" + jo.getString("dayNo") + "天]#获得" + jo.getString("value") + "积分");
        StatusUtil.KbSignInToday();
      } else if (s.contains("\"HAS_SIGN_IN\"")) {
        StatusUtil.KbSignInToday();
      } else {
        Log.runtime(TAG, jo.getString("errorMessage"));
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void goldTicket() {
    try {
      // 签到
      goldBillCollect("\"campId\":\"CP1417744\",\"directModeDisableCollect\":true,\"from\":\"antfarm\",");
      // 收取其他
      goldBillCollect("");
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /** 收取黄金票 */
  private void goldBillCollect(String signInfo) {
    try {
      String str = AntMemberRpcCall.goldBillCollect(signInfo);
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".goldBillCollect.goldBillCollect", jsonObject.optString("resultDesc"));
        return;
      }
      JSONObject object = jsonObject.getJSONObject("result");
      JSONArray jsonArray = object.getJSONArray("collectedList");
      int length = jsonArray.length();
      if (length == 0) {
        return;
      }
      for (int i = 0; i < length; i++) {
        Log.other("黄金票🙈[" + jsonArray.getString(i) + "]");
      }
      Log.other("黄金票🏦本次总共获得[" + JsonUtil.getValueByPath(object, "collectedCamp.amount") + "]");
    } catch (Throwable th) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, th);
    }
  }

  private void enableGameCenter() {
    try {
      try {
        String str = AntMemberRpcCall.querySignInBall();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.querySignInBall", jsonObject.optString("resultDesc"));
          return;
        }
        str = JsonUtil.getValueByPath(jsonObject, "data.signInBallModule.signInStatus");
        if (String.valueOf(true).equals(str)) {
          return;
        }
        str = AntMemberRpcCall.continueSignIn();
        ThreadUtil.sleep(300);
        jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.continueSignIn", jsonObject.optString("resultDesc"));
          return;
        }
        Log.other("游戏中心🎮签到成功");
      } catch (Throwable th) {
        Log.runtime(TAG, "signIn err:");
        Log.printStackTrace(TAG, th);
      }
      try {
        String str = AntMemberRpcCall.queryPointBallList();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".batchReceive.queryPointBallList", jsonObject.optString("resultDesc"));
          return;
        }
        JSONArray jsonArray = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "data.pointBallList");
        if (jsonArray == null || jsonArray.length() == 0) {
          return;
        }
        str = AntMemberRpcCall.batchReceivePointBall();
        ThreadUtil.sleep(300);
        jsonObject = new JSONObject(str);
        if (jsonObject.optBoolean("success")) {
          Log.other("游戏中心🎮全部领取成功[" + JsonUtil.getValueByPath(jsonObject, "data.totalAmount") + "]乐豆");
        } else {
          Log.runtime(TAG + ".batchReceive.batchReceivePointBall", jsonObject.optString("resultDesc"));
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "batchReceive err:");
        Log.printStackTrace(TAG, th);
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanSignIn() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      if (jo.getJSONObject("result").getBoolean("canPush")) {
        jo = new JSONObject(AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN"));
        if (jo.optBoolean("success")) {
          String prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
          Log.record("安心豆🫘[" + prizeName + "]");
        } else {
          Log.runtime(jo.toString());
        }
      }

    } catch (Throwable t) {
      Log.runtime(TAG, "beanSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanExchangeBubbleBoost() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      int userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint");
      jo = new JSONObject(AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
      String itemId = jo.getString("itemId");
      String itemName = jo.getString("itemName");
      jo = jo.getJSONObject("itemExchangeConsultDTO");
      int realConsumePointAmount = jo.getInt("realConsumePointAmount");
      if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
        return;
      }
      jo = new JSONObject(AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount));
      if (jo.optBoolean("success")) {
        Log.record("安心豆🫘[兑换:" + itemName + "]");
      } else {
        Log.runtime(jo.toString());
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanExchangeBubbleBoost err:");
      Log.printStackTrace(TAG, t);
    }
  }
}
