package com.u2020.reporter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.u2020.sdk.logging.ComberAgent;
import com.u2020.sdk.logging.CottonHelper;
import com.u2020.sdk.logging.CottonParam;
import com.u2020.sdk.logging.OAIDObserver;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {
    private EditText edit_comber_log, edit_custom_cotton;
    private Spinner spinner_standard_cotton, spinner_define_cotton;
    private Button send_standard_cotton, send_define_cotton, send_custom_cotton_now, send_custom_cotton_later;
    private ArrayAdapter standerAdt, defineAdt;
    private int standerAdtIndex;
    private String uid;
    /**
     * 具体值请参考{@link CottonParam.Value}
     */
    private int defineActionId = CottonParam.Value.SDK_VIEW_OPEN_LOGIN;
    /**
     * ComberLog所需的动态权限，非强制！为了保证数据转化追踪请尽量引导用户同意授予READ_PHONE_STATE权限
     */
    private final String[] requestPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    /***-----------------------------ComberLog Lifecycle Begin--------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getIdentifier(this, "layout", "main"));
        setTestContentView();
        //权限申请示例
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requestPermissions, 2020);
        }
        ComberAgent.registerOAIDObserver(new OAIDObserver() {//可选项，valid可能运行在子线程环境！！
            @Override
            public void valid(final String oaid) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edit_comber_log.setText(edit_comber_log.getText() + "\noaid_" + oaid);
                    }
                });
            }
        });
    }

    /**
     * 必接项，启动行为，ComberLog会过滤频繁的启动
     */
    @Override
    protected void onResume() {
        super.onResume();
        CottonHelper.onStart();
    }

    /**
     * 必接项，权限申请最终结果处理，数据转化TraceId相关
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //若对接方强制用户必须允许权限或有引导授权等业务操作
		//则建议在最后的授权处理结果才调用以下方法
		//请保证将最后结果通知到ComberLog，否则将可能影响激活。
		ComberAgent.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 深度转化相关必接项：启动、注册、登录、支付行为；
     * 其他可选项：在线时长、绑定账号信息、下单、支付页面显示、添加&选择支付渠道、预设埋点行为、自定义行为；
     * 若为游戏产品，游戏行为上报包含：1(选择服务器)，2(创建角色，必接)，3(进入游戏)、4(等级提升，必接)、5(角色退出、切换角色)、6(游戏内自定义事件)；
     */
    @Override
    public void onClick(View view) {
        if (view == send_standard_cotton) {//标准行为(及时)
            checkAction((String) standerAdt.getItem(standerAdtIndex));
        } else if (view == send_define_cotton) {//预设埋点行为(定时)
            CottonHelper.onView(defineActionId);//
        } else if (view == send_custom_cotton_now) {//自定义行为(及时)
            if (!TextUtils.isEmpty(edit_custom_cotton.getText())) {
                //1.不带拓展参数
                //ComberAgent.onAction(edit_custom_cotton.getText().toString().trim());
                //2.携带拓展参数，示例：
                JSONObject ext = new JSONObject();
                try {
                    ext.put("time", System.currentTimeMillis());
                    ext.put("frequency", 18);
                } catch (JSONException ignored) {
                }
                ComberAgent.onAction(edit_custom_cotton.getText().toString().trim(), ext);//参数1为行为类型或名称，参数2位拓展参数
            }
        } else if (view == send_custom_cotton_later) {//自定义行为(定时)
            //1.不带拓展参数
            //ComberAgent.onIntervalAction(edit_custom_cotton.getText().toString().trim());
            //2.携带拓展参数，示例：
            JSONObject ext = new JSONObject();
            try {
                ext.put("time", System.currentTimeMillis());
                ext.put("frequency", 18);
            } catch (JSONException ignored) {
            }
            ComberAgent.onIntervalAction(edit_custom_cotton.getText().toString().trim(), ext);
        }
    }

    private void checkAction(String action) {
        switch (action) {
            case CottonParam.Action.START://启动，必接项
                CottonHelper.onStart();
                break;
            case CottonParam.Action.REGISTER://注册，必接项，用户id和用户账号相对单个用户唯一且不为空
                /**
                 * 注册方式(1普通注册，2随机账号，3手机注册，4设备注册，5短信注册(短信上行)，6微信注册，7QQ注册，8微博注册,9.硬核渠道,10.其他方式)
                 * 手动修改过随机生成的账户，账户类型使用【普通注册】而非【随机账号】
                 */
                CottonHelper.onRegister(uid = String.valueOf(new Random().nextInt(10_000)), "123456", CottonParam.RegType.PHONE);//uid、account、 reg type
                break;
            case CottonParam.Action.LOGIN://登录，必接项,用户id和用户账号相对单个用户唯一且不为空
                CottonHelper.onLogin(uid, "123456");//uid、account
                //调用时机一般在登录之后，开启心跳会话
                CottonHelper.onLine(true);//可选，当InitConfig.Builder.skipSession(false)时生效；CottonHelper.onLine(false)为下线
                break;
            case CottonParam.Action.BIND://账号绑定的相关信息，每次更新任何字段上报对应的一条，无更新的不传
                JSONObject ext = new JSONObject();
                try {
                    ext.put(CottonParam.Key.TRUE_NAME, "Sam");
                    ext.put(CottonParam.Key.QQ, "5656102");
                    ext.put(CottonParam.Key.ID_CARD, "99999999999");
                    ext.put(CottonParam.Key.PHONE, "15918543937");
                    ext.put(CottonParam.Key.EMAIL, "5656102@qq.com");
                } catch (JSONException ignored) {
                }
                CottonHelper.onBind(ext);
                break;
            case CottonParam.Action.PLAY://游戏行为
                playGame();
                break;
            case CottonParam.Action.CHECKOUT://下单
                checkout();
                break;
            case CottonParam.Action.VIEW_PAYMENT://支付页面，必接项
                viewPayment();
                break;
            case CottonParam.Action.ADD_PAYMENT_CHANNEL://支付渠道
                onAddPaymentChannel();
                break;
            case CottonParam.Action.PURCHASE://支付&购买，必接项
                purchase();
                break;
        }
    }

    /***-----------------------------ComberLog Lifecycle End--------------------------*/

    /***----------------------------------Test Example--------------------------------*/
                                         //仅测试用途//

    @SuppressLint("ClickableViewAccessibility")
    private void setTestContentView() {
        //日记
        ((DemoApplication) getApplicationContext()).setMain(new WeakReference<>(this));
        edit_comber_log = findViewById(getIdentifier(this, "id", "edit_comber_log"));
        edit_comber_log.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        //标准行为
        spinner_standard_cotton = findViewById(getIdentifier(this, "id", "spinner_standard_cotton"));
        spinner_standard_cotton.setOnItemSelectedListener(this);
        List<String> standardActionList = new ArrayList<>();
        standardActionList.add(CottonParam.Action.START);
        standardActionList.add(CottonParam.Action.REGISTER);
        standardActionList.add(CottonParam.Action.LOGIN);
        standardActionList.add(CottonParam.Action.BIND);
        standardActionList.add(CottonParam.Action.PLAY);
        standardActionList.add(CottonParam.Action.CHECKOUT);
        standardActionList.add(CottonParam.Action.VIEW_PAYMENT);
        standardActionList.add(CottonParam.Action.ADD_PAYMENT_CHANNEL);
        standardActionList.add(CottonParam.Action.PURCHASE);
        standerAdt = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, standardActionList);
        standerAdt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_standard_cotton.setAdapter(standerAdt);
        send_standard_cotton = findViewById(getIdentifier(this, "id", "send_standard_cotton"));
        send_standard_cotton.setOnClickListener(this);
        //预设埋点
        String[] view_actions = getResources().getStringArray(getIdentifier(this, "array", "view_action"));
        defineAdt = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Arrays.asList(view_actions));
        defineAdt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_define_cotton = findViewById(getIdentifier(this, "id", "spinner_define_cotton"));
        spinner_define_cotton.setOnItemSelectedListener(this);
        spinner_define_cotton.setAdapter(defineAdt);
        send_define_cotton = findViewById(getIdentifier(this, "id", "send_define_cotton"));
        send_define_cotton.setOnClickListener(this);
        //自定义行为
        edit_custom_cotton = findViewById(getIdentifier(this, "id", "edit_custom_cotton"));
        send_custom_cotton_now = findViewById(getIdentifier(this, "id", "send_custom_cotton_now"));
        send_custom_cotton_now.setOnClickListener(this);
        send_custom_cotton_later = findViewById(getIdentifier(this, "id", "send_custom_cotton_later"));
        send_custom_cotton_later.setOnClickListener(this);
    }

    @Deprecated
    public void setLogCat(final String message, Throwable throwable) {
        if (edit_comber_log != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    edit_comber_log.setText(edit_comber_log.getText() + "\n" + message);
                    edit_comber_log.setSelection(edit_comber_log.getText().length());
                }
            });
        }
    }

    private int getIdentifier(Context context, String type, String id) {
        return context.getResources().getIdentifier(id, type, context.getPackageName());
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == spinner_standard_cotton) {
            standerAdtIndex = i;
        } else if (adapterView == spinner_define_cotton) {
            defineActionId = (i + 1) * 10;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    /**
     *  若果对接为游戏产品，则以下对接行为{@link CottonParam.Key#ACTION_ID}类型中，创角和等级提升为必选项，其他可选
     *   {@link CottonParam.GameAction}
     *      int SELECT_SERVER = 1;           //选择服务器
     *      int CREATE_ROLE = 2;             //创建角色
     *      int ENTER_GAME = 3;              //进入游戏
     *      int LEVEL_UP = 4;                //等级提升
     *      int EXIT_GAME = 5;               //退出游戏
     *      int DEFINED_EVENT = 6;           //自定义事件
     *      自定义事件参数说明：
     *      1.自定义事件类型{@link CottonParam.Key#EVENT_TYPE，CottonParam.Key.ACTION_ID=6时必传}，
     *      2.额外参数{@link CottonParam.Key#ACTION_PARAM， json格式}根据自定义事件类型不同可能会有不同的必传及非必传需求，
     *      若有需求，运营、研发可根据游戏特点自行定义埋点事件EVENT_TYPE，定义事件对应的行为额外参数
     *
     */
    private void playGame() {
        JSONObject param = new JSONObject();
        try {
            //进入游戏示例
            param.put(CottonParam.Key.ACTION_ID, CottonParam.GameAction.ENTER_GAME);//3
            //游戏自定义事件示例
            //param.put(CottonParam.Key.ACTION_ID, CottonParam.GameAction.DEFINED_EVENT);//6
            //param.put(CottonParam.Key.EVENT_TYPE, "main_task");//游戏自定义事件类型示例，主线任务
            //param.put(CottonParam.Key.ACTION_PARAM,new JSONObject().put("main_task_id", "1001"));//游戏自定义事件额外参数示例，任务id等拓展参数
            //游戏行为携带的角色信息，必选项
            setGameParam(param);
        } catch (JSONException ignored) {
        }
        CottonHelper.onGameAction(param);
    }

    /**
     * 下单，可选项
     */
    private void checkout() {
        JSONObject param = new JSONObject();
        try {
            //游戏产品则携带角色信息
            setGameParam(param);
        } catch (JSONException ignored) {
        }
        //商品id, 商品名称, 商品描述, 充值金额, 订单id, 游戏参数
        CottonHelper.onCheckout("21", "元宝", "游戏货币道具",100F, "BE17682866" + System.currentTimeMillis() + System.currentTimeMillis(), param);
        //非游戏产品示例
        //CottonHelper.onCheckout("21", "阴阳快递员", "悬疑惊悚小说", 100F, "BE17682866" + System.currentTimeMillis());
    }

    /**
     * 弹出充值页面，必接项
     */
    private void viewPayment() {
        JSONObject param = new JSONObject();
        try {
            //订单信息
            param.put(CottonParam.Key.PRODUCT_ID, "21");
            param.put(CottonParam.Key.PRODUCT_NAME, "元宝");
            param.put(CottonParam.Key.PRODUCT_DESC, "游戏货币道具");
            param.put(CottonParam.Key.PAY_MONEY, 200F);
            //此时还未生成订单则不传订单相关ID信息
            param.put(CottonParam.Key.ORDER_ID, "BE17682866" + System.currentTimeMillis());//订单id示例
            param.put(CottonParam.Key.CP_ORDER_ID, "CP17682866" + System.currentTimeMillis());//研发订单ID，可选

            //游戏产品则携带角色信息
            setGameParam(param);
        } catch (JSONException ignored) {
        }
        CottonHelper.onViewPayment(param);
    }

    /**
     * 添加&选择支付渠道，可选项
     */
    private void onAddPaymentChannel() {
        JSONObject param = new JSONObject();
        try {
            //订单信息
            param.put(CottonParam.Key.PRODUCT_ID, "21");
            param.put(CottonParam.Key.PRODUCT_NAME, "元宝");
            param.put(CottonParam.Key.PRODUCT_DESC, "游戏货币道具");
            param.put(CottonParam.Key.PAY_MONEY, 200F);
            //此时还未生成订单则不传订单相关ID信息
            param.put(CottonParam.Key.ORDER_ID, "BE17682866" + System.currentTimeMillis());//订单id示例
            param.put(CottonParam.Key.CP_ORDER_ID, "CP17682866" + System.currentTimeMillis() + System.currentTimeMillis());//研发订单ID示例，可选

            //游戏产品则携带角色信息
            setGameParam(param);
        } catch (JSONException ignored) {
        }
        CottonHelper.onAddPaymentChannel(1, param);//参数1：payChannel, 付费渠道ID，对方平台自定义对应支付关系，如1微信2支付宝3银联等；参数2：拓展参数
    }

    /**
     * 支付&购买，必接项
     */
    private void purchase() {
        JSONObject param = new JSONObject();
        try {
            //游戏产品则携带角色信息
            setGameParam(param);
        } catch (JSONException ignored) {
        }
        //商品id, 商品名称, 商品描述, 充值金额, 订单id, 付费渠道ID, 是否支付&购买成功, 成功&失败备注\原因(可选，如正在处理中、用户中途取消、网络连接出错等)
        CottonHelper.onPurchase("21", "元宝", "游戏货币道具",100F, "BE17682866" + System.currentTimeMillis(), 1, true, null, param);
        //非游戏产品示例
        //CottonHelper.onPurchase("21", "阴阳快递员", "悬疑惊悚小说", 100F, "BE17682866" + System.currentTimeMillis(), 1, true, "success");
    }

    /**
     *  游戏产品则需携带角色信息。
     *  设置游戏角色的当前信息, 请注意类型安全，避免引起行为上报失败。
     */
    private void setGameParam(JSONObject param) throws JSONException {
        param.put(CottonParam.Key.SERVER_ID, "208");//玩家所在服务器的ID，String
        param.put(CottonParam.Key.SERVER_NAME, "风驰");//玩家所在服务器的名称，String
        param.put(CottonParam.Key.ROLE_NAME, "Shayla");//角色名称，String
        param.put(CottonParam.Key.ROLE_ID, "3689");//角色id，String
        param.put(CottonParam.Key.ROLE_LEVEL, 1);//角色等级，int
        param.put(CottonParam.Key.ROLE_CAREER, "弓箭手");//角色职业，String
        param.put(CottonParam.Key.ROLE_CREATE_TIME, System.currentTimeMillis() / 1000);//创角时间,单位秒，long
        param.put(CottonParam.Key.ROLE_SEX, 2);//角色性别，1男2女, int
        param.put(CottonParam.Key.VIP, 11);//VIP等级, int
        param.put(CottonParam.Key.REMAIN_CURRENCY, 500L);//剩余充值元宝/钻石, long
        param.put(CottonParam.Key.FIGHT, 8000000L);//战力, long
    }

}
