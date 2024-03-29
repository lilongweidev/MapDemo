package com.llw.mapdemo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private LocationClient mLocClient;
    private BaiduMap mBaiduMap;

    private BitmapDescriptor bitmap;//标点的图标
    private double markerLatitude = 0;//标点纬度
    private double markerLongitude = 0;//标点经度
    private ImageButton ibLocation;//重置定位按钮
    private Marker marker;//标点也可以说是覆盖物

    //需要请求的动态权限
    private final String[] permissionArray = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //动态请求权限
    private ActivityResultLauncher<String[]> requestPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //请求定位，registerForActivityResult的注册方式需要在Activity初始化之前进行，否则会报错。
        requestPermissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean coarseLocation = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
            boolean fineLocation = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
            boolean readPhoneState = Boolean.TRUE.equals(result.get(Manifest.permission.READ_PHONE_STATE));
            boolean writeStorage = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE));
            if (coarseLocation && fineLocation && readPhoneState && writeStorage) {
                //所有权限都已经成功获取
                initLocation();// 定位初始化
            } else {
                Toast.makeText(MainActivity.this, "有权限未通过", Toast.LENGTH_SHORT).show();
            }
        });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();//视图初始化

        checkVersion();//检查版本

        mapOnClick();//地图点击
    }

    /**
     * 检查版本
     */
    private void checkVersion() {
        //Android6.0以下不需要动态请求权限，直接初始化
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initLocation();// 定位初始化
            return;
        }
        //Android6.0及以上需要动态请求权限，所以需要检查是否已经请求了权限。
        boolean result = false;
        //遍历权限数组，有一个没有通过则表示需要请求权限，这里还能再细分每一个请求权限。
        for (String permission : permissionArray) {
            result = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        if (result) initLocation();
        else requestPermissions.launch(permissionArray);
    }

    private void initView() {
        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        //回到当前定位
        ibLocation = (ImageButton) findViewById(R.id.ib_location);
        mMapView.showScaleControl(true);  // 设置比例尺是否可见（true 可见/false不可见）
        //mMapView.showZoomControls(false);  // 设置缩放控件是否可见（true 可见/false不可见）
        mMapView.removeViewAt(1);// 删除百度地图Logo

        mBaiduMap = mMapView.getMap();

        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                final String info = (String) marker.getExtraInfo().get("info");
                Toast.makeText(MainActivity.this, info, Toast.LENGTH_SHORT).show();
                return true;

            }
        });
    }

    /**
     * 地图点击
     */
    private void mapOnClick() {
        // 设置marker图标
        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }

            //此方法就是点击地图监听
            @Override
            public void onMapClick(LatLng latLng) {
                //获取经纬度
                markerLatitude = latLng.latitude;
                markerLongitude = latLng.longitude;
                //先清除图层
                mBaiduMap.clear();
                // 定义Maker坐标点
                LatLng point = new LatLng(markerLatitude, markerLongitude);
                // 构建MarkerOption，用于在地图上添加Marker
                MarkerOptions options = new MarkerOptions().position(point)
                        .icon(bitmap);
                // 在地图上添加Marker，并显示
                //mBaiduMap.addOverlay(options);
                marker = (Marker) mBaiduMap.addOverlay(options);
                Bundle bundle = new Bundle();
                bundle.putSerializable("info", "纬度：" + markerLatitude + "   经度：" + markerLongitude);
                marker.setExtraInfo(bundle);//将bundle值传入marker中，给baiduMap设置监听时可以得到它

                //点击地图之后重新定位
                initLocation();
            }
        });

    }


    /**
     * 定位初始化
     */
    public void initLocation() {
        //添加隐私合规政策
        LocationClient.setAgreePrivacy(true);
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        if (mLocClient == null) {
            try {
                mLocClient = new LocationClient(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mLocClient != null) {
            MyLocationListener myListener = new MyLocationListener();
            mLocClient.registerLocationListener(myListener);
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 设置高精度定位
            option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
            option.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
            option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            option.setIgnoreKillProcess(false);//可选，默认false，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认杀死
            option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
            mLocClient.setLocOption(option);
            mLocClient.start();//开始定位
        }
    }

    /**
     * 点切换到其他标点位置时，重置定位显示，点击之后回到自动定位
     *
     * @param view
     */
    public void resetLocation(View view) {
        markerLatitude = 0;
        initLocation();
        marker.remove();//清除标点
    }

    /**
     * 定位SDK监听函数 这里是实现BDAbstractLocationListener，之前的版本是BDLocationListener
     */
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            Toast.makeText(MainActivity.this, location.getAddrStr(), Toast.LENGTH_SHORT).show();
            // MapView 销毁后不在处理新接收的位置
            if (mMapView == null) {
                return;
            }
            double resultLatitude;
            double resultLongitude;

            if (markerLatitude == 0) {//自动定位
                resultLatitude = location.getLatitude();
                resultLongitude = location.getLongitude();
                ibLocation.setVisibility(View.GONE);
            } else {//标点定位
                resultLatitude = markerLatitude;
                resultLongitude = markerLongitude;
                ibLocation.setVisibility(View.VISIBLE);
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())// 设置定位数据的精度信息，单位：米
                    .direction(location.getDirection()) // 此处设置开发者获取到的方向信息，顺时针0-360
                    .latitude(resultLatitude)
                    .longitude(resultLongitude)
                    .build();

            mBaiduMap.setMyLocationData(locData);// 设置定位数据, 只有先允许定位图层后设置数据才会生效
            LatLng latLng = new LatLng(resultLatitude, resultLongitude);
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(latLng).zoom(20.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 在activity执行onResume时必须调用mMapView. onResume ()
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 在activity执行onPause时必须调用mMapView. onPause ()
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        // 在activity执行onDestroy时必须调用mMapView.onDestroy()
        mMapView.onDestroy();
    }
}
