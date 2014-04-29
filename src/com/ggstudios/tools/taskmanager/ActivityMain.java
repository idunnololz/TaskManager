package com.ggstudios.tools.taskmanager;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.os.Bundle;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

public class ActivityMain extends SherlockFragmentActivity {
	private static final String TAG = "ActivityMain";

	ActionBar actionBar;
	ViewPager pager;
	MainPagerAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		actionBar = getSupportActionBar();
		// Specify that tabs should be displayed in the action bar.
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		pager = (ViewPager) findViewById(R.id.pager);
		pager.setOnPageChangeListener(
	            new ViewPager.SimpleOnPageChangeListener() {
	                @Override
	                public void onPageSelected(int position) {
	                    // When swiping between pages, select the
	                    // corresponding tab.
	                    actionBar.setSelectedNavigationItem(position);
	                }
	            });

		List<FragmentContainer> frags = new ArrayList<FragmentContainer>();
		frags.add(new FragmentContainer(ActivityProcesses.class, R.string.Processes));
		frags.add(new FragmentContainer(ActivityApps.class, R.string.Apps));

		for(FragmentContainer c : frags){
			actionBar.addTab(actionBar.newTab().setText(c.resTitle).setTabListener(tabListener));
		}

		adapter = new MainPagerAdapter(getSupportFragmentManager(), frags);

		pager.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.activity_main, menu);
	    return true;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		Preferences.getInstance().commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_settings:
	            Intent i = new Intent(this, SettingsActivity.class);
	            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	            startActivity(i);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	ActionBar.TabListener tabListener = new ActionBar.TabListener() {
		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
			// When the tab is selected, switch to the
			// corresponding page in the ViewPager.
			pager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
			// TODO Auto-generated method stub

		}
	};

	private class FragmentContainer {
		Class<?> fragment;
		int resTitle;

		FragmentContainer(Class<?> frag, int resIdTitle){
			fragment = frag;
			resTitle = resIdTitle;
		}

	}

	// Since this is an object collection, use a FragmentStatePagerAdapter,
	// and NOT a FragmentPagerAdapter.
	public class MainPagerAdapter extends FragmentPagerAdapter {
		List<FragmentContainer> fragments;

		public MainPagerAdapter(FragmentManager fm, List<FragmentContainer> fragments) {
			super(fm);

			this.fragments = fragments;
		}

		@Override
		public Fragment getItem(int i) {
			try {
				return (Fragment) fragments.get(i).fragment.newInstance();
			} catch (InstantiationException e) {
				DebugLog.e(TAG, e);
			} catch (IllegalAccessException e) {
				DebugLog.e(TAG, e);
			}

			return null;
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getResources().getString(fragments.get(position).resTitle);
		}
	}
}
