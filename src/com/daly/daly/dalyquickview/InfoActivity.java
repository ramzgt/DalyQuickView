package com.daly.daly.dalyquickview;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class InfoActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_infoactivity);
	}
	
	public void weAreDone(View v){
		
		finish();
	}
	
}
