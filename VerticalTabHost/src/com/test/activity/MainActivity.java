package com.test.activity;

import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;

import com.test.widget.MyTabHost;

public class MainActivity extends ActivityGroup {
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        MyTabHost tabHost = (MyTabHost) findViewById(R.id.tabhost);
        tabHost.setup(getLocalActivityManager());
        
        tabHost.addTab(tabHost.newTabSpec("tab1")
        		.setIndicator("TAB1",R.drawable.contact)
        		.setContent(new Intent(this, TestActivity1.class)));
        tabHost.addTab(tabHost.newTabSpec("tab2")
        		.setIndicator("TAB2", R.drawable.group)
        		.setContent(new Intent(this, TestActivity2.class)));
        tabHost.addTab(tabHost.newTabSpec("tab3")
        		.setIndicator("TAB3", R.drawable.setting)
        		.setContent(R.id.view3));
        
    }
}