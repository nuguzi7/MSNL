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
	String Path = Environment.getExternalStorageDirectory().getPath() + "/MSNL/";
	String dataPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/Data/";
	String tmpPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/tmp/";
	static double latPoint = 0, lngPoint = 0, accPoint = 0;
	static boolean isRun = false;
	Handler handler = new Handler();
    public int num = 0;
    
	/* GPS */
	int count;
	boolean catched;
	double acc_before;
	String provider;
 
    LocationManager LM;
	
	/* File save */
	String text; // 파일 저장용 메모리
	String name; // 어플 목록
	String fileName; // 저장파일 이름(yyyymmdd.txt)
 
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "서비스가 시작되었습니다", Toast.LENGTH_SHORT).show();

    	LM = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        /* Set provider */
        LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, lL);
        LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, lL);
    }
    
    LocationListener lL = new LocationListener() {
    	@Override
    	public void onLocationChanged(Location loc) {
    		provider = loc.getProvider();
    		latPoint = loc.getLatitude();
    		lngPoint = loc.getLongitude();
    		accPoint = loc.getAccuracy();
    	}
    	
    	@Override
    	public void onProviderDisabled(String arg0) {
    	}
    	@Override
    	public void onProviderEnabled(String arg0) {
    	}
    	@Override
    	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    	}
    };
 
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
			while ( latPoint == 0 )
			{
				try{
					Thread.sleep(5000);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			handler.post(new Runnable(){public void run(){
					Toast.makeText(MyService.this, "위치정보가 잡혔습니다\n데이터 수집을 시작하겠습니다", Toast.LENGTH_SHORT).show();
				}
			});
			
			while(isRun)
			{
				
				/* 어플 목록 초기화 */
				name = "";
				
				/* 화면이 꺼져있는 경우 */
				PowerManager pn = (PowerManager)getSystemService(Context.POWER_SERVICE);
				if ( pn.isScreenOn() == false ) 
					name = "screen_off ";
				
				/* 어플 목록 받아오기 */
				ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
				List<RunningTaskInfo> Info = am.getRunningTasks(20);
				for( Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext(); ) {
					RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
					name += runningTaskInfo.topActivity.getPackageName();
					if(iterator.hasNext()){
						name += " ";
					}
				}
				
				/* 
				 * Initialize ( count == 0 )
				 * GPS check ( count < 30 )
				 * File save ( count == 180 )
				 */
				handler.post(new Runnable(){
					public void run(){
						if ( count == 0 ) {
							fileInit();
					        LM.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, lL);
					        LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, lL);
						}
						
						if ( count == 30 ) {
							LM.removeUpdates(lL);
						}

						count++;
						
						/* 시간 체크 */
						String strNow = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date(System.currentTimeMillis()));

						/* 날짜가 바뀔 시 */
						if ( !fileName.equals(strNow.substring(0,8) + ".txt" ) ) {
							fileSave();
						}
						else {
							/* fileSave용 변수 만들기 */
							text += Double.toString(latPoint) + " "
								+ Double.toString(lngPoint) + " "
								+ Double.toString(acc_before) + " "
								+ strNow + " "
								+ name.toString() + "\n";
						}
						
						if ( count >= 180 ) {
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
	
	public void fileInit()
	{
		fileName = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(System.currentTimeMillis())) + ".txt";
		text = "";

		try {
	    	/* 파일 체크 및 생성 */
			File file = new File(dataPath+fileName);
			if ( !file.exists() ) {
				file.createNewFile();
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

    public void fileSave()
    {
    	/* 파일에 저장 */
		File file = new File(dataPath+fileName);
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
    	LM.removeUpdates(lL);
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