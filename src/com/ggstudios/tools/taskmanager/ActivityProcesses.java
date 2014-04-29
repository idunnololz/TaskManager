package com.ggstudios.tools.taskmanager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ActivityProcesses extends SherlockFragment implements OnSharedPreferenceChangeListener {
	private static final String TAG = "ActivityProcesses";

	private View rootView;
	private ActivityManager activityMgr;
	private PackageManager packageMgr;
	private List<RunningAppProcessInfo> runningApps;
	private ListView lvProcesses;
	private TextView txtOverview;
	private BaseAdapter adapter;
	private List<ProcessItem> processList;
	private SharedPreferences prefs;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.activity_processes, container, false);

		prefs = Preferences.getInstance().getPrefs();

		setHasOptionsMenu(true);

		activityMgr = (ActivityManager) getActivity().getSystemService( Service.ACTIVITY_SERVICE );
		packageMgr = getActivity().getPackageManager();
		
		txtOverview = (TextView) rootView.findViewById(R.id.txtOverview);
		lvProcesses = (ListView) rootView.findViewById(R.id.lvProcesses);
		
		((Button) rootView.findViewById(R.id.btnKillAll)).setOnClickListener(onKillAllClicked);

		setupProcessList();
		prefs.registerOnSharedPreferenceChangeListener(this);

		return rootView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.activity_processes, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.item_refresh:
			refreshProcessList();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onResume(){
		DebugLog.d(TAG, "onResume");
		super.onResume();
		refreshProcessList();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();

		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	OnClickListener onKillAllClicked = new OnClickListener(){

		@Override
		public void onClick(View sender) {
			for(ProcessItem i : processList){
				killProcess(i.pkgName);
			}
			
			refreshProcessList();
		}
		
	};
	
	private String getTotalRAM() {
	    RandomAccessFile reader = null;
	    String load = null;
	    try {
	        reader = new RandomAccessFile("/proc/meminfo", "r");
	        load = reader.readLine();
	    } catch (IOException e) {
	        DebugLog.e(TAG, e);
	    }
	    return load;
	}

	private void refreshProcessList() {
		MemoryInfo mi = new MemoryInfo();
		activityMgr.getMemoryInfo(mi);
		long availableMegs = mi.availMem / 1048576L;
		
		String overview = getResources().getString(R.string.ram_info);
		if(Build.VERSION.SDK_INT >= 16){
			overview = String.format(overview, mi.totalMem / 1048576L, availableMegs);
		} else {
			overview = String.format(overview, (Integer.parseInt(getTotalRAM().replaceAll("[\\D]", ""))/ 1024) - availableMegs, availableMegs);
		}
		txtOverview.setText(overview);
		
		runningApps = activityMgr.getRunningAppProcesses();
		final Resources r = getActivity().getResources();

		processList.clear();
		adapter.notifyDataSetChanged();

		new Thread(){
			public void run(){
				Drawable defaultIcon = r.getDrawable(android.R.drawable.sym_def_app_icon);

				int importanceFilter = Integer.valueOf(prefs.getString(Keys.PREF_IMPORTANCE_FILTER, "400"));
				
				for(RunningAppProcessInfo i : runningApps){

					String pkgName = packageMgr.getNameForUid(i.uid);

					final ProcessItem item = new ProcessItem();

					if(i.importance < importanceFilter){
						continue;
					}
					
					try {
						ApplicationInfo info = packageMgr.getApplicationInfo(pkgName, 0);
						//info.

						if(!prefs.getBoolean(Keys.PREF_SHOW_SYSTEM_APPS, false)){
							if((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0){
								continue;
							}
						}

						item.appName = packageMgr.getApplicationLabel(info).toString();
						item.icon = packageMgr.getApplicationIcon(pkgName);
					} catch (NameNotFoundException e) {
						//DebugLog.e(TAG, e);
						/* do nothing */
						if(!prefs.getBoolean(Keys.PREF_SHOW_SYSTEM_APPS, false)){
							continue;
						}
					}

					if(item.icon == null){
						// if we can't get the icon for some reason
						item.icon = defaultIcon;
					}

					if(item.appName == null){
						item.appName = pkgName;
					}

					item.info = i;
					item.pkgName = pkgName;

					StringBuilder str = new StringBuilder();
					str.append("PID: ");
					str.append(i.pid);
					str.append(" UID: ");
					str.append(i.uid);
					str.append(" IMP: ");
					str.append(i.importance);
					item.details = str.toString();

					lvProcesses.post(new Runnable(){

						@Override
						public void run() {
							DebugLog.d(TAG, "adding item");
							processList.add(item);
							adapter.notifyDataSetChanged();
						}

					});
				}
			}
		}.start();
	}

	private void setupProcessList(){
		processList = new ArrayList<ProcessItem>();

		if(prefs.getBoolean(Keys.PREF_DETAILED_VIEW, false)){
			adapter = new DetailedProcessListAdapter(getActivity(), processList);
		} else {
			adapter = new ProcessListAdapter(getActivity(), processList);
		}
		lvProcesses.setAdapter(adapter);
		lvProcesses.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				ProcessItem item = processList.get(position);
				killProcess(item.pkgName);
				refreshProcessList();
			}

		});
	}
	
	private void killProcess(String packageName){
		activityMgr.killBackgroundProcesses(packageName);
	}

	private class ProcessItem {
		Drawable icon;
		String appName;
		String pkgName;
		String details;

		RunningAppProcessInfo info;
	}

	private class ViewHolder {
		ImageView icon;
		TextView txtAppName;
		TextView txtDetails;
	}

	private class ProcessListAdapter extends BaseAdapter implements ListAdapter {
		List<ProcessItem> processList;
		LayoutInflater inflater;

		ProcessListAdapter(Context con, List<ProcessItem> list){
			this.processList = list;
			inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}


		@Override
		public int getCount() {
			return processList.size();
		}

		@Override
		public Object getItem(int index) {
			return processList.get(index);
		}

		@Override
		public long getItemId(int index) {
			return index;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			ProcessItem item = processList.get(position);

			if(convertView == null){
				convertView = inflater.inflate(R.layout.process_item, parent, false);

				holder = new ViewHolder();
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.txtAppName = (TextView) convertView.findViewById(R.id.txtAppName);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.icon.setImageDrawable(item.icon);
			holder.txtAppName.setText(item.appName);

			return convertView;
		}

	}

	private class DetailedProcessListAdapter extends BaseAdapter implements ListAdapter {
		List<ProcessItem> processList;
		LayoutInflater inflater;

		DetailedProcessListAdapter(Context con, List<ProcessItem> list){
			this.processList = list;
			inflater = (LayoutInflater) con.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}


		@Override
		public int getCount() {
			return processList.size();
		}

		@Override
		public Object getItem(int index) {
			return processList.get(index);
		}

		@Override
		public long getItemId(int index) {
			return index;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			ProcessItem item = processList.get(position);

			if(convertView == null){
				convertView = inflater.inflate(R.layout.process_item_detailed, parent, false);

				holder = new ViewHolder();
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.txtAppName = (TextView) convertView.findViewById(R.id.txtAppName);
				holder.txtDetails = (TextView) convertView.findViewById(R.id.txtDetails);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.icon.setImageDrawable(item.icon);
			holder.txtAppName.setText(item.appName);
			holder.txtDetails.setText(item.details);

			return convertView;
		}

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key.equals(Keys.PREF_SHOW_SYSTEM_APPS)) {
			refreshProcessList();
		} else if (key.equals(Keys.PREF_DETAILED_VIEW)) {
			setupProcessList();
			refreshProcessList();
		} else if (key.equals(Keys.PREF_IMPORTANCE_FILTER)) {
			refreshProcessList();
		}
	}
}
