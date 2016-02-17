package com.daly.daly.dalyquickview;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TicketsActivity extends Activity{
	
	private static final String URL = "http://www.dalybase.com/quickview/quickview-daly-gettickets.php";
	private static AsyncHttpClient client = new AsyncHttpClient();
	private Intent intent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tickets);
				
		intent = getIntent();// get account_id we passed from putExtra
		String installed_product_id = intent.getStringExtra("installed_product_id");
		getTicketData(installed_product_id);
	}
	
	private void getTicketData(String id){
		
		RequestParams params = new RequestParams("product_id", id);
		
		client.post(URL, params, new JsonHttpResponseHandler(){
			@Override 
			public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                 try {                	 
                	 JSONArray tasks = data.getJSONArray("results");
                	 //Log.w("getTicketData()","Length of JSON array from server: "+tasks.length());
                	 LinearLayout v = (LinearLayout)findViewById(R.id.linearLayoutTickets);
                	 android.widget.LinearLayout.LayoutParams llparams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
                	 
     	 			 for(int i = 0; i < tasks.length(); ++i){
     	 				 // in order to get the data from server,
     	 				 //go into the first JSONObject layer, THEN parse the JSON based off of {name:pair, name:pair}
     	 				 String task_number = tasks.getJSONObject(i).optString("task_number");
     	 				 String task_name = tasks.getJSONObject(i).optString("task_name");
     	 				 
     	 				 LinearLayout ll = new LinearLayout(TicketsActivity.this);
     	 				 ll.setLayoutParams(llparams);
    	 				 
    	 				 TextView tv1 = new TextView(TicketsActivity.this);
    	 				 tv1.setTextColor(Color.parseColor("#53CFCE"));
    	 				 tv1.setText(task_number);
    	 				 
    	 				 TextView tv2 = new TextView(TicketsActivity.this);
    	 				 tv2.setTextColor(Color.parseColor("#53CFCE"));
    	 				 tv2.setText("     /     "+task_name);
    	 				 
    	 				 ll.addView(tv2,0);
    	 				 ll.addView(tv1,0);
    	 				 v.addView(ll);
     	 			 }
     	        	 
				 } catch (JSONException e) {
                     e.printStackTrace();
                 }
			 }
			 @Override
			public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
		});
	}
	
	public void weAreDone(View v){
		finish();
	}
}
