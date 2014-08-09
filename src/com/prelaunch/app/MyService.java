package com.prelaunch.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class MyService extends Service {
	String Path = Environment.getExternalStorageDirectory().getPath()
			+ "/MSNL/";
	String dataPath = Path + "Data/";
	String tmpPath = Path + "tmp/";

	private static double latPoint = 0, lngPoint = 0, accPoint = 0;
	private double acc_X, acc_Y, acc_Z;
	static boolean isRun = false;
	Handler handler = new Handler();
	public int num = 0;

	// GPS
	int count;
	boolean catched;
	double acc_before;
	String provider;

	SensorManager SM;
	LocationManager LM;
	ActivityManager aM;
	long availableMegs;
	MemoryInfo mi;

	// File save
	String text; // �뙆�씪 ���옣�슜 硫붾え由�
	String name; // �뼱�뵆 紐⑸줉
	String fileName; // ���옣�뙆�씪 �씠由�(yyyymmdd.txt)

	public void onCreate() {
		super.onCreate();
		
		SM = (SensorManager)getSystemService(SENSOR_SERVICE);
		SM.registerListener(sL, SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

		LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, lL);
		
		aM = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		handler2.sendEmptyMessage(0);
	}
	
	@SuppressLint("HandlerLeak") private Handler handler2 = new Handler() {
    	public void handleMessage(Message msg) {
    		mi = new MemoryInfo();
    		aM.getMemoryInfo(mi);
            availableMegs = mi.availMem / 1024L;
    		handler2.sendEmptyMessageDelayed(0, 1000);
    	}
    };
	
	
	SensorEventListener sL = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
		}
		@Override
		public void onSensorChanged(SensorEvent se) {
			acc_X = se.values[0];
			acc_Y = se.values[1];
			acc_Z = se.values[2];
		}
	};

	LocationListener lL = new LocationListener() {
		@Override
		public void onLocationChanged(Location loc) {
			provider = loc.getProvider();
			latPoint = loc.getLatitude();
			lngPoint = loc.getLongitude();
			accPoint = loc.getAccuracy();
			String text = "Provider: " + provider + "\n" + "Latitude: "
					+ Double.toString(latPoint) + "\n" + "Longitude: "
					+ Double.toString(lngPoint) + "\n" + "Accuracy: "
					+ Double.toString(accPoint);
			updateForeground(text);
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
			new Thread(new mRun()).start();
			startForeground(1,
					getMyActivityNotification("�쐞移� �젙蹂대�� 李얜뒗 以묒엯�땲�떎.", true));
		}

		return START_STICKY;
	}

	int checkTime = 0;

	class mRun implements Runnable {
		public void run() {
			count = 0;
			while (latPoint == 0);
			makeNotification("�쐞移� �젙蹂대�� 李얠븯�뒿�땲�떎 \n�뜲�씠�꽣 �닔吏묒쓣 �떆�옉�븯寃좎뒿�땲�떎");

			while (isRun) {

				// �뼱�뵆 紐⑸줉 珥덇린�솕
				name = "";

				// �뼱�뵆 紐⑸줉 諛쏆븘�삤湲�
				ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
				List<RunningTaskInfo> Info = am.getRunningTasks(20);
				for (Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext();) {
					RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
					final String packageName = runningTaskInfo.topActivity.getPackageName();
					name += packageName;
					if (iterator.hasNext()) {
						name += " ";
					}
				}
				 

				/*
				 * Initialize ( count == 0 ) 
				 * GPS check ( count < 30 ) 
				 * File save ( count == 180 )
				 */
				handler.post(new Runnable() {
					public void run() {
						if (count == 0) {
							fileInit();
							// �뿉�꼫吏� �냼紐⑤�� 以꾩씠湲� �쐞�빐 GPS provider �궗�슜 以묒�
							LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0, lL);
						}

						count++;

						// �떆媛� 泥댄겕
						String strNow = getTime();

						// �궇吏쒓� 諛붾�� �떆
						if (!fileName.equals(strNow.substring(0, 8) + ".txt")) {
							fileSave();
						} else {
							// fileSave�슜 蹂��닔 留뚮뱾湲�
//							text += Double.toString(latPoint) + " "
//								+ Double.toString(lngPoint) + " "
//								+ Double.toString(acc_before) + " "
//								+ Integer.toString(getBatteryPercentage(getApplicationContext())) + " "
//								+ Boolean.toString(screenStatus()) + " "
//								+ strNow + " "
//								+ name.toString() + "\n";
							text += jsonCreate().toString() + "\n";
							Log.i("MyService",text);
						}

						if (count >= 180) {
							fileSave();
						}
					}
				});

				// data collection interval : 1000ms
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	private JSONObject jsonCreate()
	{
		JSONObject json_result = new JSONObject();
		
		
		// SENSOR
		JSONObject json_sensor, json_gps, json_acc;
		try {
			json_gps = new JSONObject()
				.put("lat", latPoint)
				.put("lng", lngPoint)
				.put("acc", accPoint);
			json_acc = new JSONObject()
				.put("X", acc_X)
				.put("Y", acc_Y)
				.put("Z", acc_Z);
			json_sensor = new JSONObject()
				.put("GPS",json_gps)
				.put("ACC",json_acc);
			json_result.put("sensor", json_sensor);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// STATUS
		JSONObject json_status, json_battery;
		try {
			json_battery = new JSONObject()
				.put("percentage", getBatteryPercentage(getApplicationContext()))
				.put("status", checkBatteryState()); // 0:Charging 1:Full Battery 2:Not Charging
			json_status = new JSONObject()
				.put("date", getTime())
				.put("screen_status", screenStatus())
				.put("brightness", getBrightness())
				.put("battery", json_battery)
				.put("memory",availableMegs);
			json_result.put("status", json_status);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// APP
		JSONArray json_apps = new JSONArray();
		ArrayList<String> apps = appList();
		try {
			for ( int i=0; i<apps.size(); i++ )
			{
				String appName = apps.get(i);
				JSONObject app = new JSONObject()
					.put("name", appName)
					//.put("memory_usage", 0)
					.put("importance", getAppImportance(appName))
					.put("mem", getAppMemory(appName));
				json_apps.put(app);
			}
			json_result.put("app", json_apps);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json_result;
	}
	
	public ArrayList<String> appList() 
	{
		ArrayList<String> appList = new ArrayList<String>();
		
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> Info = am.getRunningTasks(20);
		for (Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext();) {
			RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
			appList.add(runningTaskInfo.topActivity.getPackageName());
			if (iterator.hasNext()) {
				name += " ";
			}
		}
		return appList;
	}

	public void fileInit() {
		fileName = new SimpleDateFormat("yyyyMMdd", Locale.KOREA)
				.format(new Date(System.currentTimeMillis())) + ".txt";
		text = "";

		try {
			// File check & create
			File file = new File(dataPath + fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Save data memory to file
	public void fileSave() {
		File file = new File(dataPath + fileName);
		try {
			RandomAccessFile raf = new RandomAccessFile(file, "rw");
			raf.seek(raf.length());
			raf.write((text).getBytes());
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* 珥덇린�솕 */
		count = 0;
	}
	
	private String getTime()
	{
		return new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.KOREA).format(new Date(System.currentTimeMillis()));
	}

	// getBatteryPercentage(getApplicationContext())
	public static int getBatteryPercentage(Context context) {
		Intent batteryStatus = context.registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = level / (float) scale;
		return (int) (batteryPct * 100);
	}

	// Notification maker
	private Notification getMyActivityNotification(String text, boolean tick) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Main.class), 0);

		if (tick == true) {
			return new NotificationCompat.Builder(getApplicationContext())
					.setContentTitle(getString(R.string.team_name))
					.setContentText(text).setSmallIcon(R.drawable.ic_launcher)
					.setTicker(text).setAutoCancel(true)
					.setContentIntent(contentIntent).build();
		} else {
			return new NotificationCompat.Builder(getApplicationContext())
					.setContentTitle(getString(R.string.team_name))
					.setContentText(text).setSmallIcon(R.drawable.ic_launcher)
					.setAutoCancel(true).setContentIntent(contentIntent)
					.build();
		}
	}

	// Notification to foreground
	private void updateForeground(String text) {
		Notification notification = getMyActivityNotification(text, false);

//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//				new Intent(this, Main.class), 0);

		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, notification);
	}

	// Notification notifier
	private void makeNotification(String text) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(7777, getMyActivityNotification(text, true));
	}
	
	private boolean screenStatus() {
		PowerManager pn = (PowerManager) getSystemService(Context.POWER_SERVICE);
		return pn.isScreenOn();
	}

	private int getAppImportance(String packageName) {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return -1;
		}
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(packageName)) {
				
				return appProcess.importance;
			}
		}
		return -1;
	}
	
	private int getAppMemory(String packageName) {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return -1;
		}
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(packageName)) {
				int pids[] = new int[1];
			    pids[0] = appProcess.pid;
			    android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
			    android.os.Debug.MemoryInfo pidMemoryInfo = memoryInfoArray[0];
				return pidMemoryInfo.getTotalPss();
			}
		}
		return -1;
	}

	private float getBrightness() {
		float curBrightnessValue = -1;
		try {
			curBrightnessValue = android.provider.Settings.System.getInt(
					getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return curBrightnessValue;
	}

	/*private long getMemoryUsage() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}*/

	/*
	 * [Return Value] 0 : Charging 1 : Full Battery 2 : Not Charging
	 */
	private int checkBatteryState() {
		/*
		 * ++++ You could also detect current level of charge by getting more
		 * details from your sticky intent : BatteryManager.EXTRA_LEVEL to get
		 * current level and BatterManager.EXTRA_SCALE to get the maximum level.
		 */
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(null, filter);

		int chargeState = batteryStatus.getIntExtra(
				BatteryManager.EXTRA_STATUS, -1);

		switch (chargeState) {
		case BatteryManager.BATTERY_STATUS_CHARGING:
			return 0;
		case BatteryManager.BATTERY_STATUS_FULL:
			return 1;
		default:
			return 2;
		}
	}

	public void onDestroy() {
		super.onDestroy();
		LM.removeUpdates(lL);
		makeNotification("�꽌鍮꾩뒪媛� 醫낅즺�릺�뿀�뒿�땲�떎");
	}

	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	public IBinder onBind(Intent arg0) {
		return binder;
	}

	private final IBinder binder = new MyBinder();

	public class MyBinder extends Binder {
		int getService() {
			return num;
		}
	}
}