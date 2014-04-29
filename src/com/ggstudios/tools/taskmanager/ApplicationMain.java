package com.ggstudios.tools.taskmanager;

import android.app.Application;

public class ApplicationMain extends Application{
	@Override
	public void onCreate(){
		Preferences.initialize(getApplicationContext());
	}
}
