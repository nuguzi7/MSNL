package com.prelaunch.app;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ListActivity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Dispatcher extends ListActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems=new ArrayList<String>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.dispatcher);
        
        adapter=new ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1,
            listItems);
        setListAdapter(adapter);
        
        ListView lv = getListView();
        lv.setOnItemClickListener(listViewClickListener);
        
        updateAppList(null);
    }
    
    OnItemClickListener listViewClickListener = new OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> parentView, View clickedView, int position, long id)
        {
        	String clickedItem = listItems.get(position);// getPackageName();
        	kill_test( clickedItem );
//        	requestKillProcess( clickedItem );
        }
    };
    
    public void updateAppList(View v) {
    	listItems.clear();
    	/* 어플 목록 받아오기 */
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> Info = am.getRunningTasks(20);
		for(Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext();){
			RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
			listItems.add(runningTaskInfo.topActivity.getPackageName());
		}
		adapter.notifyDataSetChanged();
		/*ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
		for(RunningAppProcessInfo i : list){
			listItems.add(i.processName + " " + i.importance);
		}
        adapter.notifyDataSetChanged();*/
    }

	public void kill_test( String kill_app )
	{
		System.out.println("kill_test started");
		/*ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> appProcess = am.getRunningAppProcesses();
		for(RunningAppProcessInfo i:appProcess){
            if( i.processName.equals(kill_app) ){
            	i.importance = RunningAppProcessInfo.IMPORTANCE_EMPTY;
            	am.killBackgroundProcesses(i.processName);
        		Toast.makeText(getApplicationContext(), i.processName + " " + i.importance + "\n" + RunningAppProcessInfo.IMPORTANCE_SERVICE, Toast.LENGTH_SHORT).show();
            }
        }*/
		System.out.println(kill_app);
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
		for(RunningAppProcessInfo i : list){
			System.out.println(i.processName);
			if ( i.processName.equals(kill_app) )
			{
				String toastMsg = i.processName + " kill test\n"
						+ "importance = " + i.importance;
				Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
				i.importance = RunningAppProcessInfo.IMPORTANCE_EMPTY;
				am.killBackgroundProcesses(i.processName);
				return;
			}
		}
		System.out.println("kill_test failed");
		/*List<RunningTaskInfo> Info = am.getRunningTasks(20);
		for(Iterator<RunningTaskInfo> iterator = Info.iterator(); iterator.hasNext();){
			RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
			String name = runningTaskInfo.topActivity.getPackageName();
			if ( name.equals(kill_app) )
			{
				Toast.makeText(getApplicationContext(), kill_app + " is killed", Toast.LENGTH_SHORT).show();
				am.killBackgroundProcesses(kill_app);
				break;
			}
		}
		Toast.makeText(getApplicationContext(), "NOT FOUND", Toast.LENGTH_SHORT).show();*/
		/*ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		am.restartPackage(kill_app);
		Toast.makeText(getApplicationContext(), kill_app + " is killed", Toast.LENGTH_SHORT).show();*/
	}
	
	public void requestKillProcess(final String arg){

		//#1. first check api level.
		int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
		if (sdkVersion < 8){
			//#2. if we can use restartPackage method, just use it.
			ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
			am.restartPackage(arg);
		}else{
			//#3. else, we should use killBackgroundProcesses method.
			new Thread(new Runnable() {
				@Override
				public void run() {
					ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
					RunningServiceInfo si;

					//pooling the current application process importance information.
					while(true){
						List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
						for(RunningAppProcessInfo i : list){
							if(i.processName.equals(arg) == true){
								//#4. kill the process,
								//only if current application importance is less than IMPORTANCE_BACKGROUND
								if(i.importance >= RunningAppProcessInfo.IMPORTANCE_BACKGROUND)
									am.restartPackage(arg); //simple wrapper of killBackgrounProcess
								else
									Thread.yield();
								break;
							}
						}
					}
				}
			}, "Process Killer").start();
		}
	}
}
