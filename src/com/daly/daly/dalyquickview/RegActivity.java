package com.daly.daly.dalyquickview;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import org.json.JSONObject;

import com.loopj.android.http.*;
import org.apache.http.*;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;

public class RegActivity extends Activity {
	
	private static final String URL = "http://www.dalybase.com/quickview/quickview-droidregister.php";
	private static AsyncHttpClient client = new AsyncHttpClient();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reg);
		Log.w("Began RegActivity", "finding email accounts and setting up");
		findEmailAccountsRoutine();
	}
	
	private void findEmailAccountsRoutine(){
		
		// find ALL the email accounts before we prompt the user to choose
				Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
				android.accounts.Account[] accounts = AccountManager.get(getBaseContext()).getAccounts();
				ArrayList<String> accountsList = new ArrayList<String>();
				
				for (android.accounts.Account account : accounts) {
				    if (emailPattern.matcher(account.name).matches() && 
				    		(account.name.contains("@daly.com") || account.name.contains("@bcps.org"))) {
				        accountsList.add(account.name);
				    }
				}
				Log.w("emails found:", accountsList.toString());
				// if we find more than one valid email, or none, have the user choose the one closest to their organization's email
				if (accountsList.size() == 1){
					// authorize the one email found
					writeInstallationFile();
					writeFile("EMAIL",accountsList.get(0));
					addNewUserToDalybase();
					restartUponSuccessfulRegistration(); // used to be finish()
					
				} else if (accountsList.size() > 1) {
					// concatenate the emails together, we assume they must be unique in this way
					// technically, this case should NEVER happen in the field
					String emails = "";
					for (String email : accountsList){
						if (!emails.contains(email) && !emails.matches(email.trim())){
							emails += email + " "; // make sure we do not have duplicate email accounts concatenated
						}
					}
					// authorize using all the emails in one list
					writeInstallationFile();
					writeFile("EMAIL",emails);
					addNewUserToDalybase();
					restartUponSuccessfulRegistration(); // used to be finish()
					
				} else {
					// prompt message informing the user of the situation and quit out of application when press button
					  AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setMessage("You must have @Daly.com or @BCPS.org email account set up on your device to access. Please set up email account and try again.");
						builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					           public void onClick(DialogInterface dialog, int id) {
					        	   Intent homeIntent= new Intent(Intent.ACTION_MAIN);
					        	   homeIntent.addCategory(Intent.CATEGORY_HOME);
					        	   homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					        	   startActivity(homeIntent);
					           }
					       });
						AlertDialog dialog = builder.create();
						dialog.show();
				}
	}
	
	private void restartUponSuccessfulRegistration(){ 
		// ensure this activity is cleared and we restart everything from the beginning
		Log.w("RegActivity","sending back to MainActivity");
		Intent resultIntent = new Intent();
		resultIntent.putExtra("uid", readFile("INSTALLATION"));
		Log.w("restartUponSuccessfulRegistration()",""+readFile("INSTALLATION"));
		resultIntent.putExtra("email", readFile("EMAIL"));
		  setResult(Activity.RESULT_OK, resultIntent);
		  //resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		  finish();
		//Intent intent = new Intent(this, MainActivity.class);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		//startActivity(intent);
	}
	
	@SuppressWarnings("deprecation")
	private String[] getDeviceInfo(){
		String[] s= new String[6];
		s[0] = System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
		s[1] = android.os.Build.VERSION.SDK;
		s[2] = android.os.Build.DEVICE;
		s[3] = android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";
		s[4] = ""+getWindow().getWindowManager().getDefaultDisplay().getWidth();
	    s[5] = ""+getWindow().getWindowManager().getDefaultDisplay().getHeight();
		return s;
	}
	
	private void addNewUserToDalybase(){
		// send install hash created from random chars to PHP / MySQL server quickview_users
		String email, uid = "";
		uid = readFile("INSTALLATION");
		email = readFile("EMAIL");
		String[] deviceinfo = getDeviceInfo();
		// show in log for demo purposes
		Log.w("addNewUserToDalybase() - Install ID", uid);
		Log.w("addNewUserToDalybase() - EMAIL", email);
		/*Log.w("addNewUserToDalybase() - device info - OS Version", deviceinfo[0]);
		Log.w("addNewUserToDalybase() - device info - API Level", deviceinfo[1]);
		Log.w("addNewUserToDalybase() - device info - Device", deviceinfo[2]);
		Log.w("addNewUserToDalybase() - device info - Model", deviceinfo[3]);
		Log.w("addNewUserToDalybase() - device info - Screen Width", deviceinfo[4]);
		Log.w("addNewUserToDalybase() - device info - Screen Height", deviceinfo[5]);*/
		
		RequestParams params = new RequestParams();
		params.put("uid", uid);
		params.put("email", email);
		params.put("osversion", deviceinfo[0]);
		params.put("apilevel", deviceinfo[1]);
		params.put("device", deviceinfo[2]);
		params.put("model", deviceinfo[3]);
		params.put("width", deviceinfo[4]);
		params.put("height", deviceinfo[5]);
		
		client.post(URL, params, new JsonHttpResponseHandler(){
			
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject data) {  
				
				Log.w("addNewUserToDalybase()", "Registered new user (was successfully POSTed)");
			 }
			 
			 @Override
             public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) 
             {addNewUserToDalybase();}
		});
		Log.w("addNewUserToDalybase()", "Registered new user (ended)");
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
            throw new RuntimeException(e);
		}
    }
	
	private void writeFile(String name, String data){
		
		try {
			
		File installation = new File(this.getFilesDir(), name);
        FileOutputStream out = new FileOutputStream(installation);
        out.write(data.getBytes());
        out.close();
        Log.w("writeFile()","Wrote email file w/ value: " + data);
		} catch (Exception e) {
            throw new RuntimeException(e);
		}
    }
	
    private void writeInstallationFile(){
    	
    	try {
    	
    	File installation = new File(this.getFilesDir(), "INSTALLATION");
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
        Log.w("writeInstallationFile()","Wrote install file (uid) w/ value: " + id);
    	} catch (Exception e) {
            throw new RuntimeException(e);
		}
    }
	
	@Override
	public void onBackPressed()
	  {
		  //Make sure user cannot change state if back button accidentally pressed
		// also we must make sure that the user cannot go back to main screen if they are not authenticated
	  }
	
	@Override
	public void onResume(){
		super.onResume();
		findEmailAccountsRoutine();// try again to find valid email for this install
	}
	
}
