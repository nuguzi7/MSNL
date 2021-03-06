package com.prelaunch.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity implements OnClickListener {

	static int BUFFER_SIZE = 1024;
	int count = 0;
	public static TextView printApp;
	String Path = Environment.getExternalStorageDirectory().getPath() + "/MSNL/";
	String dataPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/Data/";
	String tmpPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/tmp/";

	Button btn, btn2, btn3;
	String default_text = "<사용법>\n" +
			"1.어플을 켜고 'Start'버튼을 누릅니다\n" +
			"2.밑에 검정색으로 '서비스가 시작되었습니다'라고 뜨면 다른 일을 하시면 됩니다\n" +
			"3.자료를 모으고 toServer버튼을 눌러서 데이터를 보내줍니다(수초~수분 소요)\n" +
			"(주의 : toServer버튼을 누르면 데이터가 소진됩니다)\n\n" +
			"Start : data 수집 시작\n" +
			"toServer : 서버로 data 전송\n" +
			"GPS Check : GPS값 체크 + 서비스 체크";

	public static Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if ( msg.arg1 == 1 )
				printApp.setText(new String(msg.obj.toString()));
			else if ( msg.arg1 == 2 )
				printApp.append(new String(msg.obj.toString()));
			else
				System.out.println("arg1 error");
		}
	};   

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// StrictMode setting
		if(android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	        StrictMode.setThreadPolicy(policy);
		}
		
		// 폴더 체크 및 생성
		if( !(new File(Path).exists()) ) (new File(Path)).mkdir();
		if( !(new File(dataPath).exists()) ) (new File(dataPath)).mkdir();
		if( !(new File(tmpPath).exists()) ) (new File(tmpPath)).mkdir();

		btn = (Button)findViewById(R.id.start);
		if ( MyService.isRun ) btn.setText("Stop");
		btn2 = (Button)findViewById(R.id.toServer);
		btn3 = (Button)findViewById(R.id.check);
		
		btn.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btn3.setOnClickListener(this);

		printApp = (TextView)findViewById(R.id.printApp);
		printApp.setText(default_text);
	}
	
	public void onClick(View v)
	{
		switch( v.getId() )
		{
		case R.id.start:
			if ( btn.getText().toString().equals("Start") )
			{
				if ( checkGPS() )
				{
					handler.post(new Runnable(){
						public void run(){
							btn.setText("Stop");
							printApp.setText(default_text);
						}
					});
					startService(new Intent(Main.this, MyService.class));
				}
			}
			else
			{
				handler.post(new Runnable(){
					public void run(){
						btn.setText("Start");
						printApp.setText(default_text);
					}
				});
				stopService(new Intent(Main.this, MyService.class));
			}
			break;
			
		case R.id.toServer:
			MyService.isRun = false;
			handler.post(new Runnable(){
				public void run(){
					btn.setText("Start");
				}
			});
			stopService(new Intent(Main.this, MyService.class));
			new Thread(new ftpSend()).start();
			break;
			
		case R.id.check:
			//startActivity(new Intent(Main.this, Dispatcher.class));
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = registerReceiver(null, ifilter);
			int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

			float batteryPct = level / (float)scale;
		    
		    Toast.makeText(Main.this, Integer.toString(level) + "/" + Integer.toString(scale) + "=" + Float.toString(batteryPct), Toast.LENGTH_LONG).show();
			break;
		}
	}

	public boolean checkGPS() {
		LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			return true;
		Toast.makeText(Main.this, "GPS 기능을 켜 주십시오", Toast.LENGTH_LONG).show();
		startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),0);
		return false;
	}

	boolean isSend = false;
	boolean isBreak = false;
	public class ftpSend implements Runnable {
		public void run() {
			MyService.isRun = false;
			System.out.println("ftpSend Start!");
			msgSend( "Start FTP send\n\n파일 목록", 1 );

			// get phoneNumber
			TelephonyManager tMgr =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			String phoneNumber = "0" + tMgr.getLine1Number().substring(3);

			// get date
			String strNow = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(System.currentTimeMillis()));

			FTPUpLoader upLoader = new FTPUpLoader();
			ArrayList<String> list = new ArrayList<String>();
			
			// Add file : 폴더안의 파일 전부 추가
			File dirFile=new File(dataPath);
			if ( !dirFile.exists() ) dirFile.mkdir();
			File []fileList=dirFile.listFiles();
			String[] files = new String[fileList.length];
			int fileNum=0;
			for(File tempFile : fileList) {
				if(tempFile.isFile()) {
					String tempFileName = tempFile.getName();
					String tempFilePath = tempFile.getPath();
					files[fileNum++] = tempFilePath;
					list.add(tempFileName);
					msgSend( "\n\t"+tempFileName, 2 );
				}
			}
			if ( fileNum == 0 ) // 파일이 없을경우
			{
				msgSend( "\n\tNo File!", 2 );
				return;
			}

			// 압축
			String zipName = strNow+".zip";
			try {
				msgSend( "\n\t\t압축중 ", 2 );
				zip( files, tmpPath+zipName );
				msgSend( "=> 완료", 2 );
			} catch(Exception e){
				Log.e("TAG","ZIP Exception", e);
				msgSend( "=> 실패", 2 );
				return;
			}
			
			// 파일삭제
			msgSend( "\n\t\t삭제중", 2 );
			for(File tempFile : fileList) {
				if(tempFile.isFile()) {
					(new File(tempFile.getPath())).delete();
				}
			}
			msgSend( " => 완료", 2 );
			
			// Add file : 폴더안의 파일 전부 추가
			dirFile=new File(tmpPath);
			File []zipDir=dirFile.listFiles();
			ArrayList<String> zipList = new ArrayList<String>();
			for(File tempFile : zipDir) {
				if(tempFile.isFile()) {
					zipList.add(tempFile.getName());
					System.out.println(tempFile.getPath().toString());
				}
			}
			
			// Connect to server
			String ftpPath = "/Users/dcmichael/web/oldweb/preteam/lab_data/" + phoneNumber;
			boolean re = upLoader.sendFtpServer("msn.unist.ac.kr", 21, "dcmichael", "bestmsnl", ftpPath, tmpPath, zipList);

			msgSend( re?"\n\n업로드 완료":"\n\n업로드 실패", 2 );
		}
	}
	
	public void msgSend( String obj, int arg ) {
		Message msg = handler.obtainMessage();
		msg.obj = obj;
		msg.arg1 = arg;
		handler.sendMessage(msg);
	}

	public boolean isServiceRunningCheck() {
		ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.prelaunch.gps_app.MyService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	public static void zip(String[] files, String zipFile) throws IOException {
		BufferedInputStream origin = null;
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
		try { 
			byte data[] = new byte[BUFFER_SIZE];

			//System.out.println(Integer.toString(files.length));
			for (int i = 0; i < files.length; i++) {
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER_SIZE);
				try {
					ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
					out.putNextEntry(entry);
					int count;
					while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
						out.write(data, 0, count);
					}
				}
				finally {
					origin.close();
				}
			}
		}
		finally {
			out.close();
		}
	}
	
	/* ETC */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
