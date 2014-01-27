package com.prelaunch.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

public class MyService extends Service {
	String path = Environment.getExternalStorageDirectory().getPath() + "/Test/";
	String path2 = Environment.getExternalStorageDirectory().getPath();
	static double latPoint = 0, lngPoint = 0, accPoint = 0;
	static boolean isRun = false;
	Handler handler = new Handler();
    public int num = 0;
    
	/* GPS */
	int count;
	boolean catched;
	double acc_before;
	String provider;
 
    LocationManager locManager; // 위치 정보 프로바이더
    LocationListener locationListener; // 위치 정보가 업데이트시 동작
	
	/* File save */
	String text;
	String name;
	String fileName;
 
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "서비스가 시작되었습니다", Toast.LENGTH_SHORT).show();

    	locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	provider = locManager.getBestProvider(getCriteria(), true);
 
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
				accPoint = location.getAccuracy();
				latPoint = location.getLatitude();
				lngPoint = location.getLongitude();
            }

			@Override
			public void onProviderDisabled(String provider){}

			@Override
			public void onProviderEnabled(String provider){}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras){}
             
        };
 
        // GPS 프로바이더를 통해 위치를 받도록 설정
        // 60초 간격으로 위치 업데이트
        // 위치 정보를 업데이트할 최소 거리 0.001f 
    	if (provider != null) {
    		locManager.requestLocationUpdates(provider, 60000, 0.001f, locationListener);
    	}
    }
	
	public static Criteria getCriteria() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE); //위도,경도 정확도
		criteria.setAltitudeRequired(true); //고도정보
		criteria.setBearingRequired(true); //방향정보
		criteria.setSpeedRequired(true); //속도정보
		criteria.setCostAllowed(true); //금전적 비용 부과 여부
		criteria.setPowerRequirement(Criteria.POWER_LOW); //최대 전력 수준
		
	    //API level 9 and up
	    criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
	    criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
	    //criteria.setBearingAccuracy(Criteria.ACCURACY_LOW);
	    //criteria.setSpeedAccuracy(Criteria.ACCURACY_MEDIUM);
		return criteria;
	}
 
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (isRun == false) {
    		isRun = true;
    		Runnable r = new mRun();
    		Thread rThread = new Thread(r);
    		rThread.start();
    	}
        
        Notification notification = new Notification(R.drawable.msn_logo, "서비스 시작하였습니다", System.currentTimeMillis());
        notification.setLatestEventInfo(getApplicationContext(), "App prelaunch team", "데이터 수집중입니다", null);
        startForeground(1, notification);
        
        return START_STICKY;
    }
	int checkTime = 0;
    class mRun implements Runnable {
		public void run()
		{
			count = 0;
			while(isRun)
			{
				while ( latPoint == 0 )
				{
					try{
						Thread.sleep(5000);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				
				/* 어플 목록 초기화 */
				name = "";
				
				/* 화면이 꺼져있는 경우 */
				PowerManager pn = (PowerManager)getSystemService(Context.POWER_SERVICE);
				if ( pn.isScreenOn() == false ) 
					name = "screen_off ";
				
				/* 어플 목록 받아오기 */
				ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
				List<RunningTaskInfo> Info = am.getRunningTasks(20);
				for(Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext();){
					RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
					name += runningTaskInfo.topActivity.getPackageName();
					if(iterator.hasNext()){
						name += " ";
					}
				}
				
				/* 
				 * Initialize ( count == 0 )
				 * GPS check ( count < 60 )
				 * File save ( count == 180 )
				 */
				handler.post(new Runnable(){
					public void run(){
						if ( count == 0 )
						{
							if ( !new File(path).exists() ) (new File(path)).mkdir();
							fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date(System.currentTimeMillis())).substring(0,8);
							text = "";
							locManager.requestLocationUpdates(provider, 60000, 0.001f, locationListener);
						}
						if ( count == 30 )
					    	locManager.removeUpdates(locationListener);
						
						String strNow = "";
						if ( latPoint != 0 && lngPoint != 0 )
						{
							count++;
							
							/* 시간 체크 */
							long now = System.currentTimeMillis();
							SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
							Date date = new Date(now);
							strNow = sdf.format(date);
							
							/* 2013.11.25 날짜가 바뀔 시 */
							if ( !fileName.equals(strNow.substring(0,8)) )
							{
								fileSave();
							}
							else
							{
								/* fileSave 용 변수 만들기 */
								text += Double.toString(latPoint) + " "
									+ Double.toString(lngPoint) + " "
									+ Double.toString(acc_before) + " "
									+ strNow + " "
									+ name.toString() + "\n";
							}
						}
						if ( count >= 180 )
						{
							/* 저장할 파일명 : yyyyMMdd */
							fileName = path + strNow.substring(0,8) + ".txt";
							
							/* 저장 */
							fileSave();
						}
					}
				});
				
				/* 1초 sleep */
				try{
					Thread.sleep(1000);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	};

    public void fileSave()
    {
    	/* 파일 체크 및 폴더 생성 */
		File file = new File(fileName);
		if( !(file.exists()) ) (new File(path)).mkdir();
		
		/* 파일에 저장 */
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(raf.length());
			raf.write((text).getBytes());
			raf.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		/* 초기화 */
		count = 0;
	}
    
    
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "onUnbind()", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }

    public IBinder onBind(Intent arg0) {
        Toast.makeText(MyService.this, "onBind()", Toast.LENGTH_SHORT).show();
        return binder;
    }
    
    public void onDestroy() {
        super.onDestroy();
    	locManager.removeUpdates(locationListener);
        Toast.makeText(this, "서비스가 종료되었습니다", Toast.LENGTH_SHORT).show();
    }
   
    private final IBinder binder = new MyBinder();
   
    public class MyBinder extends Binder{
        int getService(){
            Toast.makeText(MyService.this, "binder num : " + num, Toast.LENGTH_SHORT).show();
            return num;
        }
    }
}