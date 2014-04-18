package com.cedar.autologin;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;


public class LoginTask extends AsyncTask<BasicNameValuePair, Integer, Boolean> {
	Context context;
	String account;
	String passwd;
	int retrys = 0;
	Boolean exceedFlag = false;
	
	public LoginTask(Context context) {

	    this.context = context;
	}
	
	protected Boolean doInBackground(BasicNameValuePair... params) {
		if (!checkLogin()) {
			if (params.length > 0) {
				account = params[0].getName();
				passwd = params[0].getValue();
				if (account.equals("retrys")) {
					try {
						retrys = Integer.parseInt(passwd) + 1;
					}
					catch (NumberFormatException e) {
						Log.d("autologin", "Integer.parseInt error "+ e.toString());
						retrys = 0;
					}
					SharedPreferences sp = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
					account = sp.getString("account", "");  
					passwd = sp.getString("passwd", "");
					if (account.equals("") || passwd.equals("")) {
						Log.d("autologin", "account or passwd is empty");
						return false;
					}
				}
			} else {
				SharedPreferences sp = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
				account = sp.getString("account", "");  
				passwd = sp.getString("passwd", "");
				if (account.equals("") || passwd.equals("")) {
					Log.d("autologin", "account or passwd is empty");
					return false;
				}
			}
			if (login()) {
				Log.d("autologin", "login succeed");
				return true;
			} else {
				Log.d("autologin", "login failed");
				return false;
			}
		}
		return false;
	}

	protected void onPostExecute(Boolean result) {
		if (result) {
			Toast.makeText(context.getApplicationContext(),
					"AutoLogin: sign into seu-wlan succeed !", Toast.LENGTH_LONG)
					.show();
		} else if (retrys >= 3){
			Log.d("autologin", "retrys too many times");
			Toast.makeText(context.getApplicationContext(),
					"AutoLogin: sign into seu-wlan failed !", Toast.LENGTH_LONG)
					.show();
		} else if (exceedFlag) {
			Toast.makeText(context.getApplicationContext(),
					"AutoLogin: ������¼����������� !", Toast.LENGTH_LONG)
					.show();
		}
	}

	public Boolean checkLogin() {
		try {
			HttpClient client = new DefaultHttpClient();
			URI website = new URI("https://w.seu.edu.cn/portal/init.php");
			HttpGet request = new HttpGet();
			request.setURI(website);
			HttpResponse response = client.execute(request);
			// int statusCode = response.getStatusLine().getStatusCode();

			String responseStr = EntityUtils.toString(response.getEntity());
			//Log.d("autologin", responseStr);
			if (responseStr.contains("login_username")) {
				Log.d("autologin", "already logged in");
				return true;
			} else {
				// Log.d("autologin", "have not login");
				return false;
			}

		} catch (UnknownHostException e) {
			Log.d("autologin", "UnknownHostException");
			retry();
			return false;
		}  catch (Exception e) {
			Log.d("autologin", "Error in http connection " + e.toString());
			return false;
		}
	}

	public Boolean login() {
		try {
			HttpClient client = new DefaultHttpClient();
			URI website = new URI("https://w.seu.edu.cn/portal/login.php");
			HttpPost request = new HttpPost(website);
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("username", account));
			nameValuePairs.add(new BasicNameValuePair("password", passwd));
			request.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
				//Log.d("autologin", "��֤���  " + responseStr);
				if (responseStr.contains("login_username")) {
					Log.d("autologin", "logged in " + String.valueOf(statusCode));
					return true;
				} else if (responseStr.contains("\\u5e76\\u53d1\\u767b\\u5f55\\u8d85\\u8fc7\\u6700\\u5927\\u9650\\u5236")) {
					Log.d("autologin", "error: \u5e76\u53d1\u767b\u5f55\u8d85\u8fc7\u6700\u5927\u9650\u5236");
					exceedFlag = true;
				}
				return false;
			} else {
				Log.d("autologin", "logged failed, statusCode:" + String.valueOf(statusCode));
				return false;
			}
		} catch (Exception e) {
			Log.d("autologin", "Error in http connection " + e.toString());
			retry();
			return false;
		}
	}
	
	public void retry() {
		if (retrys < 3) {
			Log.d("autologin", "retrys " + String.valueOf(retrys));
			String UNIQUE_STRING = "com.cedar.autologin.unknownhostBroadcast";
			Intent intent = new Intent(UNIQUE_STRING);
			intent.putExtra("retrys", String.valueOf(retrys));
			context.sendBroadcast(intent);
		}
	}
}