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
import android.util.Log;
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
        	String clickedItem = listItems.get(position);
        	kill_test( clickedItem );
        	updateAppList(null);
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
    }

	public void kill_test( String kill_app )
	{
		String tag = "kill_test";
		Log.i(tag, "kill_test started");
		Log.i(tag, "kill_app : "+kill_app);
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
		for(RunningAppProcessInfo i : list){
			Log.v(tag, "process name : " + i.processName);
			if ( i.processName.equals(kill_app) )
			{
				Log.i(tag, kill_app + " found! start kill");
				Log.v(tag, "pid = " + i.pid);
				String toastMsg = i.processName + " kill test\n"
						+ "importance = " + i.importance;
				Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();

				// PERFORMANCE IMPROVEMENT #1
				i.importance = RunningAppProcessInfo.IMPORTANCE_EMPTY;
				// PERFORMANCE IMPROVEMENT #2
				android.os.Process.sendSignal(i.pid, android.os.Process.SIGNAL_KILL);

				// KILLING METHOD #1
				android.os.Process.killProcess(i.pid);
				// KILLING METHOD #2
				am.killBackgroundProcesses(i.processName);
				
				return;
			}
		}
		// Not returned == Process not found
		Log.e(tag, "Process not found");
	}
}