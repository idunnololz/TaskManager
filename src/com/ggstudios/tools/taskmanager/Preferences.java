package com.ggstudios.tools.taskmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String PREF_SHOW_SYSTEM_APPS = "showSysApps";
	
	private static final String PREFS_NAME = "prefs";
	private static Preferences prefs;
	
	private SharedPreferences settings;
	private SharedPreferences.Editor editor;
	
	private Object editorLock = new Object();
	
	public static Preferences getInstance(){
		return prefs;
	}
	
	public static void initialize(Context context){
		if(prefs == null)
			prefs = new Preferences(context);
	}
	
	private Preferences(Context context){
		settings = PreferenceManager.getDefaultSharedPreferences(context);
		editor = settings.edit();
	}
	
	public void commit(){
		synchronized(editorLock){
			editor.commit();
		}
	}
	
	public SharedPreferences getPrefs(){
		return settings;
	}
	
	public SharedPreferences.Editor getEditor(){
		return editor;
	}
}
