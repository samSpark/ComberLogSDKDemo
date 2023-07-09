package com.u2020.reporter;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.bytedance.hume.readapk.HumeSDK;
import com.u2020.sdk.logging.ComberAgent;
import com.u2020.sdk.logging.InitConfig;
import com.u2020.sdk.logging.Logger;
import com.u2020.sdk.logging.OAIDObserver;

import java.lang.ref.WeakReference;
import java.util.List;


public class DemoApplication extends Application {
    @Deprecated
    private WeakReference<MainActivity> main;//仅测试用途
    @Deprecated
    public void setMain(WeakReference<MainActivity> main) {//仅测试用途
        this.main = main;
    }

    @Override
    public void onCreate() {
        super.onCreate(); 
        //请在Application.onCreate中初始化
        ComberAgent.init(InitConfig.Builder.create(this)
                .setAppId(1)//产品提供，应用的唯一标识，appid/game_id，必填项
                .setAppKey("demo_test")//产品提供，公匙，必填项
                .setAppSecret("5JHYvJ7BEyPF61zc")//产品提供，私匙，必填项
                .setDomain("testtoken-datahub.zeda1.com")//产品提供，域名，必填项
                .setPlatform("demo_test")//产品提供，平台，必填项
                .setChannel(1)//产品提供，联运渠道id，可选，缺省1
                .setAgent(100_000)//产品提供，渠道位，可选，缺省100000
                .setSite(100_000)//产品提供，广告位，可选，缺省100000
                .skipSession(false)//在线时长统计配置，是否关闭心跳&会话，true:关闭，false:开启，可选，缺省关闭
                .setSessionTime(60)//心跳间隔时间，单位秒，开启心跳&会话后生效，范围1-5分钟，60倍数，可选，缺省60秒
                .setLogger(new Logger() {//日记接口
                    @Override
                    public void log(String message, Throwable throwable) {
                        Log.d("comber", message, throwable);//建议打开日记方便联调
                        if(main != null && main.get() != null) {//仅测试用途, 非用于正式环境
                            MainActivity activity = main.get();
                            activity.setLogCat(message, throwable);
                        }
                    }
                })
                .build());
        //以下可在Comber始化后任意地方使用
        //String siteId = String.valueOf(ComberAgent.getSiteId());//提供ComberLogSDK渠道包的id，每个渠道包id都不一样。
        //头条系渠道包渠道id获取：HumeSDK.getChannel(this).
        //获取OAID(匿名设备标识符)
        //valid可能运行在子线程环境，若在其内中使用相关UI API，请注意主线程切换runOnUiThread(Runnable action)避免异常
//        ComberAgent.registerOAIDObserver(new OAIDObserver() {
//            @Override
//            public void valid(final String oaid) {
//            }
//        });
    } 
}
