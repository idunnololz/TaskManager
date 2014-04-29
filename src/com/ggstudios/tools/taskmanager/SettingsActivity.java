package com.ggstudios.tools.taskmanager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import android.os.Bundle;

public class SettingsActivity extends SherlockPreferenceActivity {
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
