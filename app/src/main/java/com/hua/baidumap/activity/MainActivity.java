package com.hua.baidumap.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.utils.DistanceUtil;
import com.hua.baidumap.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Spring on 2015/9/12.
 */
public class MainActivity extends Activity {
    // UI 相关
    private Button requestLocButton;
    MapView mMapView;
    BaiduMap mBaiduMap;
    TextView tvDistance;
    // 距离单位 m 米
    double currentMether = 0;
    double totalMeter = 0;
    boolean isFirstLoc = true;// 是否首次定位
    private LatLng pt1;
    private LatLng pt2;
    private LatLng tempPt;
    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    public class SDKReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)){
                Toast.makeText(MainActivity.this, "网络错误，请检查", Toast.LENGTH_SHORT).show();
            }else if(action.equals(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR)){
                Toast.makeText(MainActivity.this,"权限有错，请检查",Toast.LENGTH_SHORT).show();
            }
        }
    }


    BitmapDescriptor custom2 = BitmapDescriptorFactory
            .fromResource(R.drawable.icon_geo);

    private int cnt = 0;

    private SDKReceiver mReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 计算距离的
        tvDistance = (TextView) findViewById(R.id.tv_distance);

        requestLocButton = (Button) findViewById(R.id.btn_requestLoc);
        // 定位的模式，跟随，普通，罗盘
        mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;// 默认给个普通
        requestLocButton.setOnClickListener(new MyBtnOnClickListener());
        // 地图初始化

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        //设置缩放级别，
        MapStatusUpdate zoom = MapStatusUpdateFactory.zoomTo(17);
        mBaiduMap.animateMapStatus(zoom);

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);


        // 定位初始化,,要不要放到服务里面？？？？？？？？？

        mLocClient = new LocationClient(this);
       //mLocClient = ((DemoApplication)getApplication()).mLocationClient;
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);// 由高精度转为省电模式

        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型,默认gcj02-- 国家测绘局标准
        option.setScanSpan(2000);


        option.setLocationNotify(true);
        mLocClient.setLocOption(option);
        mLocClient.start();  // 启动定位服务



        // 注册广播，监听ApplicationKey是否有效
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
        iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
        mReceiver = new SDKReceiver();
        registerReceiver(mReceiver,iFilter);
    }





    /**
     * 定位SDK监听函数，，，，，操，这个最重要了，能从 location 里面获取好多信息
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            cnt ++;
            if(location == null || mMapView == null)
                return;

            if(isFirstLoc){
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
                mBaiduMap.animateMapStatus(u);
            }else{
                MyLocationData locData = new MyLocationData.Builder().accuracy(location.getRadius())
                        .direction(270)// 此处 设置的是方向信息，顺时针0-360；
                        .longitude(location.getLongitude())
                        .latitude(location.getLatitude())
                        .build();
                mBaiduMap.setMyLocationData(locData);
                System.out.println(new Date(System.currentTimeMillis()) + "坐标x:  " + location.getLatitude() + "  " + location.getLongitude());
                // TODO: 2015/9/13 添加地图覆盖物
                // 定义点,, pt1在首次定位中得到，pt2为每次的最新值，

                if(cnt <= 2){
                    pt2 = new LatLng(location.getLatitude(),location.getLongitude());

                    List<LatLng> points = new ArrayList<>();
                    points.add(pt2);
                    points.add(pt2);
                    OverlayOptions ooPolyline = new PolylineOptions().width(10)
                            .color(0Xaa1bff2c)
                            .points(points)
                            ;
                    //mBaiduMap.addOverlay(ooPolyline);
                    //mBaiduMap.clear();  // 要不要清，，等待测试
                    pt1 = pt2;
                }else {

                    pt2 = new LatLng(location.getLatitude(), location.getLongitude());

                    List<LatLng> points = new ArrayList<>();
                    points.add(pt1);
                    points.add(pt2);
                    OverlayOptions ooPolyline = new PolylineOptions().width(10)
                            .color(0Xaa1bff2c)
                            .points(points);
                    mBaiduMap.addOverlay(ooPolyline);
                    // 计算距离
                    currentMether = DistanceUtil.getDistance(pt1, pt2);
                    totalMeter+= currentMether;
                    tvDistance.setText("今天已跑距离" + new DecimalFormat(".00").format((float)totalMeter/1000) + " km");
                    // 画完线之后，让 pt2 = pt1;  pt2 = null;等待下次赋值
                    pt1 = pt2;


                }
            }
        }
    }

    class MyBtnOnClickListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (mCurrentMode) {
                case NORMAL:
                    requestLocButton.setText("跟随");
                    mCurrentMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                    mBaiduMap
                            .setMyLocationConfigeration(new MyLocationConfiguration(
                                    mCurrentMode, true, null));
                    break;
                case COMPASS:
                    requestLocButton.setText("普通");
                    mCurrentMode = MyLocationConfiguration.LocationMode.NORMAL;
                    mBaiduMap
                            .setMyLocationConfigeration(new MyLocationConfiguration(
                                    mCurrentMode , true, null));
                    break;
                case FOLLOWING:
                    requestLocButton.setText("罗盘");
                    mCurrentMode = MyLocationConfiguration.LocationMode.COMPASS;
                    mBaiduMap
                            .setMyLocationConfigeration(new MyLocationConfiguration(
                                    mCurrentMode , true, null));
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 注销广播
        unregisterReceiver(mReceiver);
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
    }
}
