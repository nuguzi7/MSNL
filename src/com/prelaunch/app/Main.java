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
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {

	static int BUFFER_SIZE = 1024;
	int count = 0;
	public static TextView printApp;
	String Path = Environment.getExternalStorageDirectory().getPath() + "/MSNL/";
	String dataPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/Data/";
	String tmpPath = Environment.getExternalStorageDirectory().getPath() + "/MSNL/tmp/";

	Button bun, bun2, bun3;
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
		
		/*[StrictMode 정의]
		진저브레드에서 부터 추가된 일종의 개발툴로 개발자가 실수하는 것들을 감지하고 해결 할 수 있도록 돕는 모드
		(실재로 수정하지는 않음 단지 알려줌)
		
		[StrictMode의 주요기능]
		메인 스레드에서 디스크 접근, 네트워크 접근등의 비효율적인 작업을 하려는 것을 감지하여 프로그램 이 부드럽게 작동하도록 돕고, 
		빠른 응답을 가지도록 함
		
		이 코드로 NetworkOnMainThreadException 처리할 수 있다
		 */
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		// 폴더 체크 및 생성
		if( !(new File(Path).exists()) ) (new File(Path)).mkdir();
		if( !(new File(dataPath).exists()) ) (new File(dataPath)).mkdir();
		if( !(new File(tmpPath).exists()) ) (new File(tmpPath)).mkdir();

		bun = (Button)findViewById(R.id.start);
		if ( MyService.isRun ) bun.setText("Stop");
		bun2 = (Button)findViewById(R.id.toServer);
		bun3 = (Button)findViewById(R.id.check);

		printApp = (TextView)findViewById(R.id.printApp);
		printApp.setText(default_text);
		bun.setOnClickListener(new View.OnClickListener() { // Start
			public void onClick(View v) {
				if ( bun.getText().toString().equals("Start") )
				{
					if ( checkGPS() )
					{
						handler.post(new Runnable(){
							public void run(){
								bun.setText("Stop");
								printApp.setText(default_text);
							}
						});
						startActivity(new Intent(Main.this, MyService.class));
					}
				}
				else
				{
					handler.post(new Runnable(){
						public void run(){
							bun.setText("Start");
							printApp.setText(default_text);
						}
					});
					stopService(new Intent(Main.this, MyService.class));
				}
			}
		});
		bun2.setOnClickListener(new View.OnClickListener() { // toServer
			public void onClick(View v) {
				MyService.isRun = false;
				handler.post(new Runnable(){
					public void run(){
						bun.setText("Start");
					}
				});
				stopService(new Intent(Main.this, MyService.class));
				new Thread(new ftpSend()).start();
			}
		});
		bun3.setOnClickListener(new View.OnClickListener() { // ServiceCheck
			public void onClick(View v) {
				/*if ( checkGPS() )
				{
					if ( MyService.isRun )
					{
						Toast.makeText(Main.this, Double.toString(MyService.latPoint) + " "
								+ Double.toString(MyService.lngPoint) + " "
								+ Double.toString(MyService.accPoint), Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(Main.this, "서비스가 실행중이 아닙니다", Toast.LENGTH_SHORT).show();
					}
				}*/
				startActivity(new Intent(Main.this, Dispatcher.class));
			}
		});

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
			long now = System.currentTimeMillis();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
			Date date = new Date(now);
			String strNow = sdf.format(date);

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
			String ftpPath = "/Users/dcmichael/web/preteam/lab_data/" + phoneNumber;
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
