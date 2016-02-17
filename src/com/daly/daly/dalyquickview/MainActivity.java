package com.daly.daly.dalyquickview;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.Header;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.loopj.android.http.*;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
//import android.widget.AutoCompleteTextView; functionality has been overriden (filtering must be overridden)
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements TextWatcher {// APR Nov 2014
	
	private static final String getVersionURL = "http://www.dalybase.com/quickview/quickview-getlatestversion.php";
	private static final String checkTicketsURL = "http://www.dalybase.com/quickview/quickview-daly-checktickets.php";
	
	private static final String URL1 = "http://www.dalybase.com/quickview/quickview-daly-droidsearcher.php";
	private static final String URL2 = "http://www.dalybase.com/quickview/quickview-daly-droidquery.php";
	private static final String URL1_1 = "http://www.dalybase.com/quickview/quickview-daly-droidsearcher-serial.php";
	private static final String URL2_1 = "http://www.dalybase.com/quickview/quickview-daly-droidquery-serial.php";
	
	private static final String URL3 = "http://www.dalybase.com/quickview/quickview-bcps-droidsearcher.php";
	private static final String URL4 = "http://www.dalybase.com/quickview/quickview-bcps-droidquery.php";
	private static final String URL3_1 = "http://www.dalybase.com/quickview/quickview-bcps-droidsearcher-serial.php";
	private static final String URL4_1 = "http://www.dalybase.com/quickview/quickview-bcps-droidquery-serial.php";
	
	private static AsyncHttpClient client = new AsyncHttpClient(); // runs asynchronously, so we don't have to worry about starving the UI thread
	
	private AutoCompleteEditText autoComplete_asset;
	private ArrayAdapter<String> adapter_asset;
	private AutoCompleteEditText autoComplete_serial;
	private ArrayAdapter<String> adapter_serial;
	private ProgressBar spinner;
	
	private JSONArray JAasset;
	private JSONArray JAserial;
	
	private Button clearbutton;
	private Button ticketsbutton;
	private EditText editText3;
	private EditText editText4;
	private EditText editText5;
	private EditText editText6;
	private EditText editText7;
	
	private String account_id;
	private String installed_product_id;
	private String SEARCH_ASSET_URL;
	private String SEARCH_SERIAL_URL;
	private String QUERY_ASSET_URL;
	private String QUERY_SERIAL_URL;
	private String uid;
	private String latest_version;
	private String myVersionName;
	private int tixCnt;// count of tickets returned from server
	private boolean firstTimeRunning = true;
    private boolean versionMatches = false;
	private final int typingThreshold = 4;
	private final int typingDelayms = 450;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// init vars (doing it the correct way rather than outside of constructor)
		account_id = "";
		installed_product_id = "";
		SEARCH_ASSET_URL = ""; 
		QUERY_ASSET_URL = ""; 
		QUERY_SERIAL_URL = "";
		SEARCH_SERIAL_URL = "";
		uid = "";
		latest_version = "";
		myVersionName = "";
		tixCnt = 0;// count of tickets returned from server
		
		spinner = (ProgressBar) findViewById(R.id.progressBar1);
		spinner.setVisibility(View.GONE);
		clearbutton = (Button) findViewById(R.id.button1);
		ticketsbutton = (Button) findViewById(R.id.button2);
		ticketsbutton.setVisibility(View.INVISIBLE); // set to not show yet... only for Daly staff it will be shown
		editText3 = (EditText) findViewById(R.id.editText3);
		editText4 = (EditText) findViewById(R.id.editText4);
		editText5 = (EditText) findViewById(R.id.editText5);
		editText6 = (EditText) findViewById(R.id.editText6);
		editText7 = (EditText) findViewById(R.id.editText7);
		
		initializeAutoCompletes();
		
		// always confirm the version with server before allowing user install on server
		/*if (versionMatches){
			validateInstall();
		} else { checkVersion(); }*/
		//testing fix to disregard version checking:/
		validateInstall();
	}
	
	private void initializeAutoCompletes() {
		// ASSETS SEARCH
		adapter_asset = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_dropdown_item);
		adapter_asset.setNotifyOnChange(true);
		autoComplete_asset = (AutoCompleteEditText) findViewById(R.id.autoCompleteTextView1);
		autoComplete_asset.addTextChangedListener(this);
		autoComplete_asset.setThreshold(typingThreshold);
		autoComplete_asset.setAdapter(adapter_asset);
		// when item from dropdown is clicked, send to getAssetData() which
		// fills in the form
		autoComplete_asset.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						getAssetData(position); // call getAssetData() and
												// display entry from DB on UI
												// elements
					}
				});
		// SERIALS SEARCH
		adapter_serial = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_dropdown_item);
		adapter_serial.setNotifyOnChange(true);
		autoComplete_serial = (AutoCompleteEditText) findViewById(R.id.autoCompleteTextView2);
		autoComplete_serial.addTextChangedListener(this);
		autoComplete_serial.setThreshold(typingThreshold);
		autoComplete_serial.setAdapter(adapter_serial);
		// when item from dropdown is clicked, send to getAssetData()
		autoComplete_serial.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						getSerialData(position);
					}
				});

	}
	
	private void checkVersion(){
		
		Context context = getApplicationContext(); // or activity.getApplicationContext()
		PackageManager packageManager = context.getPackageManager();
		String packageName = context.getPackageName();
		
		try {
		    myVersionName = packageManager.getPackageInfo(packageName, 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
		    e.printStackTrace();
		}
		if (!myVersionName.isEmpty()){
			
			// get the current version from PHP and then compare
			client.post(getVersionURL, new JsonHttpResponseHandler(){
				@Override 
				public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                     try {
                    	 latest_version = data.getString("latest_version");
                    	 Log.w("CheckVersion() - Latest Version", latest_version);
                    	 if (latest_version.equals(myVersionName)){
         					
         					Log.w("CheckVersion()","App is updated to latest version."); 
         					versionMatches = true;
         					validateInstall();
         					
         				} else if (!latest_version.equals(myVersionName)){
         					Log.w("CheckVersion()","App is sending user to update to latest version."); 
         					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
         					builder.setMessage("A new version is available. Press OK to update to the latest version: " + latest_version + " from your current version: " + myVersionName);
         					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
         			           public void onClick(DialogInterface dialog, int id) {
         			        	   
         			        	   Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=com.daly.daly.dalyquickview"));
         			        	   startActivity(goToMarket);
         			           }
         			       });
         				   AlertDialog dialog = builder.create();
         				   dialog.show();
         				}
                    	 
					 } catch (JSONException e) {
	                     e.printStackTrace();
	                 }
				 }
				 @Override
                 public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
			});
		}
	}
	
	private void validateInstall(){
		
		if (!tryToReadInstallationFile() && firstTimeRunning){
			firstTimeRunning = false;
			Log.w("validateInstall()", "Starting RegActivity to find the emails and auto-register device");
	        Intent regIntent = new Intent(getApplicationContext(), RegActivity.class);
	        //regIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivityForResult(regIntent, 1);
	        Log.w("validateInstall()", "After started RegActivity... firstTimeRunning = false");
		} else {
			Log.w("validateInstall()","uid was read successfully... ");
			// has been installed, so there is a valid email to read from the local memory
			setURLS();
			if (SEARCH_ASSET_URL.isEmpty() || QUERY_ASSET_URL.isEmpty()){
				Log.w("validateInstall()","Must set URLs for Daly vs. BCPS");
				if (tryToReadEmail()){
                	setURLS();
                } else {Log.w("validateInstall()", "Something happen wrong...?"); }
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1 && resultCode == RESULT_OK) {
			Log.w("onActivityResult()","Returned from RegActivity");
			// has been installed, so there is a valid email to read from the local memory
			/*if (SEARCH_ASSET_URL.isEmpty() || QUERY_ASSET_URL.isEmpty()){
				Log.w("onActivityResult()","Setting the URLs for Daly vs. BCPS (mandatory after registration)");
				if (tryToReadEmail()){
			           	setURLS();
			    } else {Log.w("onActivityResult()", "Returned from RegActivity but no email found!"); }
			}*/
			try {
				uid = data.getStringExtra("uid");
				setURLS(data.getStringExtra("email"));// pass the vars directly back instead of relying on HD storage
				// somehow this works better when trying to set the URLs
				// I HAVE NO IDEA WHYYYYYYYYYYYYYYYYYYY!!! Thanks, Android
			} catch (Exception e) {		}
			return;
		}
	}
	
	private void setURLS(){
		String email = "";
		if (tryToReadEmail()){
			email = readFile("EMAIL");
		} else {
			Log.w("setURLS()", "*** No emails found when trying to set URLs");
			return;
		}
		if (!email.isEmpty()){
			Log.w("setURLS()", "email(s) found: " + email);
			if (email.contains("@daly.com")){
				
				SEARCH_ASSET_URL = URL1;
				QUERY_ASSET_URL = URL2;
				SEARCH_SERIAL_URL = URL1_1;
				QUERY_SERIAL_URL = URL2_1;
				
				ticketsbutton.setVisibility(View.VISIBLE);//show this feature for Daly staff
				
			} else if (email.contains("@bcps.org")){ // change to BCPS.org on final version
				
				SEARCH_ASSET_URL = URL3;
				QUERY_ASSET_URL = URL4;
				SEARCH_SERIAL_URL = URL3_1;
				QUERY_SERIAL_URL = URL4_1;
				
			} else {
				Log.w("setURLS()","No email was found");
			}
			Log.w("setURLS()", SEARCH_ASSET_URL + " " + QUERY_ASSET_URL);
		} else {Log.w("setURLS()","It is extremely odd that this is being run.");}
	}
	
	private void setURLS(String email){
		
		Log.w("Set URLs", "email(s) found: " + email);
		if (email.contains("@daly.com")){
			
			SEARCH_ASSET_URL = URL1;
			QUERY_ASSET_URL = URL2;
			SEARCH_SERIAL_URL = URL1_1;
			QUERY_SERIAL_URL = URL2_1;
			
			ticketsbutton.setVisibility(View.VISIBLE);//show this feature for Daly staff
			
		} else if (email.contains("@bcps.org")){ // change to BCPS.org on final version
			
			SEARCH_ASSET_URL = URL3;
			QUERY_ASSET_URL = URL4;
			SEARCH_SERIAL_URL = URL3_1;
			QUERY_SERIAL_URL = URL4_1;
			
		} else {
			Log.w("setURLS()","No email was found");
		}
		Log.w("Set URLs", SEARCH_ASSET_URL + " " + QUERY_ASSET_URL);
	}
	
	private boolean tryToReadEmail(){
		
		if (readFile("EMAIL").isEmpty()){
			Log.w("trying to read email","None found :(");
			return false;
		} else {
			Log.w("trying to read email","Yes - Found: " + readFile("EMAIL"));
			return true;
		}
	}
	
	private boolean tryToReadInstallationFile(){
		uid = readFile("INSTALLATION");
		Log.w("readInstallationFile()","reading uid value: " + uid);
		if (uid.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	private String readFile(String name) {
		
		try {
		File installation = new File(this.getFilesDir(), name);
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
        
		} catch (Exception e) {
			Log.w("File IO Error","Could not read File: " + name);
			return "";
		}
    }
	
	public void infoActivity(View v){
		startActivity(new Intent(this, InfoActivity.class));
	}
	
	public void contactActivity(View v){
		if (!account_id.isEmpty()){
			Intent contactIntent = new Intent(this, ContactActivity.class);
			contactIntent.putExtra("account_id", account_id);
			startActivityForResult(contactIntent,0);
		}
	}
	
	public void ticketsActivity(View v){
		Intent ticketsIntent = new Intent(this, TicketsActivity.class);
		ticketsIntent.putExtra("installed_product_id", installed_product_id);
		startActivityForResult(ticketsIntent,0);
	}
	
	private void checkTickets(String IPID){
		// try "USN29161" as test case
		//Log.w("checkTickets()","Sending to checkTix: " + installed_product_id.toString() + " IPID: " + IPID);
		
		if (!IPID.isEmpty()){
		
		client.post(checkTicketsURL, new RequestParams("product_id", IPID), new JsonHttpResponseHandler(){
			@Override 
			public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                	 
					tixCnt = 0;//reset this guy
                	try {
						tixCnt = data.getInt("count");
					} catch (JSONException e) {
						e.printStackTrace();
					}
                	 //Log.w("checkTickets()","Tickets found - " + tixCnt);
                	 if (SEARCH_ASSET_URL.equals(URL1)){ // only Daly version of PHP file will pass the `installed_product_id` to get the tickets from `wh_task` table
                 	 
                 	 	if (tixCnt > 0){
 	                	 	//Log.w("checkTickets()","Tickets button enabled - found open tickets");
 	                	 	ticketsbutton.setEnabled(true);
 	             		 	ticketsbutton.setTextColor(Color.parseColor("#53CFCE"));
 	                	} 
                 	 }
			 }
			 @Override
			public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
		});
		} else {Log.w("checkTickets() - product id is ",installed_product_id.toString() + " " + IPID);}
	}
	
	public void clearEntry(View v){
		autoComplete_asset.dismissDropDown();
		autoComplete_asset.setText("");
		autoComplete_serial.dismissDropDown();
		autoComplete_serial.setText("");
		initializeAutoCompletes(); // must do this re-init for some reason (Android sucks)
		editText3.setText("");
		editText4.setText("");
		editText5.setText("");
		editText6.setText("");
		editText7.setText("");
		installed_product_id = "";
		account_id = "";
		tixCnt = 0;
		ticketsbutton.setEnabled(false);
		ticketsbutton.setTextColor(Color.parseColor("#444444"));
		adapter_asset.clear();
		adapter_serial.clear();
	}
	
	@Override
	public void afterTextChanged(Editable arg0) {
		//if timer object has run out of time then keep going, query the server ?
				// some way to boost search performance by waiting until user finished typing fully (~60 ms response time)
				
				
				if (autoComplete_asset.hasFocus() && autoComplete_asset.length() > 3){// ASSETS
					//Log.w("onTextChanged()","Focus is on autoComplete asset");
					
					//if (s.length() > 0 && SEARCH_ASSET_URL.length() > 1 && QUERY_ASSET_URL.length() > 1){// ensure that the URL is set for BCPS or Daly use
						
						String queryToServer = autoComplete_asset.getText().toString().toLowerCase();
						RequestParams params = new RequestParams("letters", queryToServer);
						// the following is handled asynchronously by the android-async-http.jar plugin
						client.post(SEARCH_ASSET_URL, params, new JsonHttpResponseHandler(){
							@Override 
							public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
			                     try {
			                    	 JAasset = data.getJSONArray("results");
			                    	 JAserial = data.getJSONArray("serials");// get the data from PHP/MySQL
			                    	 // ensure there is data before attempting to process it into the ArrayAdapter
			                    	 Handler mHandler = new Handler();
			                    	 mHandler.post(new Runnable(){
			                             @Override
			                             public void run(){
			                             	if (JAasset.length() > 0 && JAserial.length() > 0){
			                            		 adapter_asset.clear();
			                            		 if (adapter_asset.getCount() > 0){
			                            			 Log.w("onTextChanged() - count Error",""+adapter_asset.getCount());
			                            		 }
			                            		 for(int i = 0; i < JAasset.length(); ++i){
			             			         	 		adapter_asset.add(JAasset.optString(i) + " - " + JAserial.optString(i));
			             			         	 //Log.w("onTextChanged(): adding data to arrayAdapter","Data: " + JAasset.optString(i) + " - " + JAserial.optString(i));
			             			         	 }
			             			         	 if (adapter_asset.getCount() > 0){
			             			         		 autoComplete_asset.showDropDown();// FORCED dropdown
			             			         	 } else {
			             			         		 Log.w("onTextChanged() - ArrayAdapter Error"," // ********* NUM DATAS: "+ JAasset.length() + " " + JAserial.length() + " " + adapter_serial.getCount() + " ************ //"); 
			             			         	 }
			                            	 }
			                             }
			                         });
			                    	 
			                     } catch (JSONException e) {
				                     e.printStackTrace();
				                 }
							 }
							 @Override
							public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
						});
						
					/*} else {
						Log.w("Fatal Error OnTextChanged()", "Tried to search while missing URLs set requirement..!");
						//setURLS();
					}*/
				} else if (autoComplete_serial.hasFocus() && autoComplete_serial.length() > 3) { // SERIALS
					//Log.w("onTextChanged()","Focus is on autoComplete serial");
						
					//if (s.length() > 0 && SEARCH_SERIAL_URL.length() > 1 && QUERY_SERIAL_URL.length() > 1){// ensure that the URL is set for BCPS or Daly use
										
						String queryToServer = autoComplete_serial.getText().toString().toLowerCase();
						RequestParams params = new RequestParams("letters", queryToServer);
						// the following is handled asynchronously by the android-async-http.jar plugin
						client.post(SEARCH_SERIAL_URL, params, new JsonHttpResponseHandler(){
							@Override 
							public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
			                     try {
			                    	 JAasset = data.getJSONArray("results");
			                    	 JAserial = data.getJSONArray("serials");// get the data from PHP/MySQL
			                    	 // ensure there is data before attempting to process it into the ArrayAdapter
			                    	 Handler mHandler = new Handler();
			                    	 mHandler.post(new Runnable(){
			                             @Override
			                             public void run(){
			                             	if (JAasset.length() > 0 && JAserial.length() > 0){
			                            		 adapter_serial.clear();
			                            		 if (adapter_serial.getCount() > 0){
			                            			 Log.w("onTextChanged() - count Error",""+adapter_serial.getCount());
			                            		 }
			             			         	 for(int i = 0; i < JAserial.length(); ++i){
			             			         	 		adapter_serial.add(JAserial.optString(i) + " - " + JAasset.optString(i));
			             			         	 // Log.w("onTextChanged(): adding data to arrayAdapter","Data: " + JAasset.optString(i) + " - " + JAserial.optString(i));
			             			         	 }
			             			         	 if (adapter_serial.getCount() > 0){
			             			         		 autoComplete_serial.showDropDown();// FORCED dropdown
			             			         	 } else {
			             			         		 Log.w("onTextChanged() - ArrayAdapter Error"," // ********** NUM DATAS: "+ JAasset.length() + " " + JAserial.length() + " " + adapter_serial.getCount() + " ************ //"); 
			             			         	 }
			                            	 }
			                             }
			                         });
			                    	 
			                     } catch (JSONException e) {
				                     e.printStackTrace();
				                 }
							 }
							 @Override
							public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
						});
						
					/*} else {
						//initializeAutoCompletes();
						Log.w("Fatal Error OnTextChanged()", "Tried to search while missing URLs set requirement..!");
						//setURLS();
					}*/
				}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		if ((SEARCH_ASSET_URL.isEmpty() || QUERY_ASSET_URL.isEmpty() || SEARCH_SERIAL_URL.isEmpty() || QUERY_SERIAL_URL.isEmpty()) && tryToReadEmail()){
			Log.w("onTextChanged()","Just in case-ing, but this should NEVER EVER HAPPEN. NOT EVEN ONCE.");
			setURLS();
		}
	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// do ~1/3 of a second delay before afterTextChanged() is called
		Timer uploadCheckerTimer = new Timer();
		uploadCheckerTimer.scheduleAtFixedRate(
		    new TimerTask() {  public void run() {} }, 0, typingDelayms); 
	}
	
	private void getAssetData(int selection)
	{
		// hide the keyboard
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(autoComplete_asset.getWindowToken(), 0);
		spinner.setVisibility(View.VISIBLE);
		String installedProductID = "";
		try {
			installedProductID = JAserial.getString(selection);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		RequestParams params = new RequestParams("selection", installedProductID);		
		params.add("uid", uid);//add the unique installation code to request: this will ID the user to update `last_accessed` field in `quickview_users` table
		client.post(QUERY_ASSET_URL, params, new JsonHttpResponseHandler(){
			 
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                 try {
                	 installed_product_id = data.getString("installed_product_id");
                	 Log.w("getAssetData() - installed_product_id",""+installed_product_id);
                	 account_id = data.getString("contactid");
                	 autoComplete_serial.setText(data.getString("serialNumber"));
                	 editText3.setText(data.getString("warrantyEnd"));
                	 editText4.setText(data.getString("contact"));
                	 editText6.setText(data.getString("delivered"));
                	 editText7.setText(data.getString("accountName"));
                	 setDaysLeft(data.getString("warrantyEnd"));
                	 checkTickets(installed_product_id);
                	 
				 } catch (JSONException e) {
                     e.printStackTrace();
                 }
			 }
			 @Override
			 public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
		});
		spinner.setVisibility(View.GONE);
	}
	
	private void getSerialData(int selection)
	{
		// hide the keyboard
		InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(autoComplete_serial.getWindowToken(), 0);
		spinner.setVisibility(View.VISIBLE);
		String installedProductID = "";
		try {
			installedProductID = JAserial.getString(selection);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		RequestParams params = new RequestParams("selection", installedProductID);		
		params.add("uid", uid);//add the unique installation code to request: this will ID the user to update `last_accessed` field in `quickview_users` table
		client.post(QUERY_SERIAL_URL, params, new JsonHttpResponseHandler(){
			 
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject data) {
                 try {
                	 installed_product_id = data.getString("installed_product_id");
                	 Log.w("getSerialData() - installed_product_id",""+installed_product_id);
                	 account_id = data.getString("contactid");
                	 autoComplete_asset.setText(data.getString("assetName"));
                	 editText3.setText(data.getString("warrantyEnd"));
                	 editText4.setText(data.getString("contact"));
                	 editText6.setText(data.getString("delivered"));
                	 editText7.setText(data.getString("accountName"));
                	 setDaysLeft(data.getString("warrantyEnd"));
     	 			
                	 checkTickets(installed_product_id);
                	 
				 } catch (JSONException e) {
                     e.printStackTrace();
                 }
			 }
			 @Override
			 public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {}
		});
		spinner.setVisibility(View.GONE);
	}
	
	private void setDaysLeft(String warrantyEnd){
		
		try {
			// joda tools
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
			DateTime wend = fmt.parseDateTime(warrantyEnd);
			// difference in days
			int diff = Days.daysBetween(new DateTime(), wend).getDays();
			// display years and days or just days if less than a year
			if (diff >= 365){
				int yrs = (int)(diff/365);
				editText5.setText(yrs + "y " + (int)(diff - yrs*365) + "d");
			} else {
				editText5.setText(diff + " d");
			}
			// set text color according to severity level to undeploy from service
			if (diff <= 0){
				editText5.setTextColor(Color.RED); // red (severe)
			} else if (diff <= 7 && diff > 0) {
				editText5.setTextColor(Color.YELLOW); // yellow (medium)
			} else {
				editText5.setTextColor(Color.GREEN); // green (OK)
			}
		
		} catch (Exception e) {
			
			editText5.setText("~");
		}
	}

	@Override
	protected void onRestart() {
		initializeAutoCompletes();
		super.onRestart();
	}

}