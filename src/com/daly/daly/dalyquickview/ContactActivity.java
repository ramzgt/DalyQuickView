package com.daly.daly.dalyquickview;

import java.util.Locale;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.*;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ContactActivity extends Activity{
	
	Intent intent;
	
	private EditText editText1;
	private EditText editText2;
	private EditText editText3;
	private EditText editText4;
	private EditText editText7;
	
	private static final String URL = "http://www.dalybase.com/quickview/quickview-droidcontact.php";
	private static AsyncHttpClient client = new AsyncHttpClient();
	private String account_id = "";
	private String address = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact);
		
		editText1 = (EditText) findViewById(R.id.editText1);
		editText2 = (EditText) findViewById(R.id.editText2);
		editText3 = (EditText) findViewById(R.id.editText3);
		editText4 = (EditText) findViewById(R.id.editText4);
		editText7 = (EditText) findViewById(R.id.editText7);
		
		intent = getIntent();// get account_id we passed from putExtra
		account_id = intent.getStringExtra("account_id");
		
		// get the contact info based on account_id
		RequestParams params = new RequestParams("accountid", account_id);
		client.post(URL, params, new JsonHttpResponseHandler(){
			@Override
			 public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                 try {
                	 editText1.setText(data.getString("name"));
                	 editText2.setText(data.getString("title"));
                	 editText3.setText(data.getString("email"));
                	 editText4.setText(data.getString("phone"));
                	 editText7.setText(data.getString("address"));
                	 address = data.getString("address");
                	 	
				 } catch (JSONException e) {
                     e.printStackTrace();
                 }
			 }
			@Override
			 public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse)  {}
		});
		
	}
	
	public void address(View v){
		String uri = String.format(Locale.ENGLISH, "geo:0,0?q="+address);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException ex)
        {
            try
            {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            }
            catch(ActivityNotFoundException innerEx)
            {
                Toast.makeText(this, "No Maps Application Found. Please install a maps application.", Toast.LENGTH_LONG).show();
            }
        }
	}
	
	public void weAreDone(View v){
		setResult(RESULT_OK, intent);
		finish();
	}

}
