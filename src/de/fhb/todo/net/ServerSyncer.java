package de.fhb.todo.net;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import de.fhb.todo.R;
import de.fhb.todo.TodoApplication;
import de.fhb.todo.db.Todos;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by IntelliJ IDEA. User: phr Date: 01.01.12 Time: 15:00 To change this template use File | Settings | File Templates.
 */
public class ServerSyncer {
	private final String TAG = ServerSyncer.class.getSimpleName();
	private final static ServerSyncer INSTANCE = new ServerSyncer();
	private final Queue<HttpPost> postQueue;
	private final HttpClient httpclient;

	private ServerSyncer() {
		httpclient = new DefaultHttpClient();
		postQueue = new LinkedList<HttpPost>();
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					if (postQueue.isEmpty()) {
						try {
							synchronized (postQueue) {
								postQueue.wait();
							}
						}
						catch (InterruptedException e) {
							Log.e(TAG, e.getMessage());
						}
					}
					else {
						// Execute HTTP Post Request
						try {
							HttpResponse response = httpclient.execute(postQueue.poll());
							Log.d(TAG, "Status code: " + response.getStatusLine().getStatusCode());
							// TODO response verarbeiten
						}
						catch (IOException e) {
							Log.e(TAG, e.getMessage());
						}

					}
				}
			}
		}, "Sync Queue").start();
	};

	public static ServerSyncer getInstance() {
		return INSTANCE;
	}

	public void checkLogin(final String username, final String password, final ResponseHandler handler) {
		if (haveInternetConnection()) {
			
		new Thread(new Runnable() {

			@Override
			public void run() {
				Context context = TodoApplication.getContext();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
				String baseUrl = prefs.getString("serverPref", "http://10.0.2.2:8080");

				String url = baseUrl + "/TodoServer/user/checkLogin";
				Log.d(TAG, "using url: " + url);
				HttpPost httppost = new HttpPost(url);

				try {
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("username", username));
					nameValuePairs.add(new BasicNameValuePair("password", password));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					HttpResponse response = httpclient.execute(httppost);

					// if we have a valid response, update our local database
					int statusCode = response.getStatusLine().getStatusCode();
					String responseBody = slurp(response.getEntity().getContent());
					if (statusCode == 200 && !TextUtils.isEmpty(responseBody)) {
						Log.d(TAG, "Status code: " + statusCode);
						Log.d(TAG, "response from server: " + responseBody);
						if (TextUtils.isDigitsOnly(responseBody)) {
							Integer intResponse = Integer.parseInt(responseBody);
							switch (intResponse) {
							case 1:
								handler.successfull();
								break;
							case 0:
							default:
								handler.failure(ResponseHandler.FAILURE_REASON_WRONG_CREDENTIALS);
								break;
							}
						} else {
							handler.failure(ResponseHandler.FAILURE_REASON_WRONG_CREDENTIALS);
						}
					} else {
						handler.failure(ResponseHandler.FAILURE_REASON_WRONG_CREDENTIALS);
					}

				}
				catch (IOException e) {
					handler.failure(ResponseHandler.FAILURE_REASON_NO_INTERNET);
				}
			}
		}, "Sync Queue").start();
		}
		else {
			handler.failure(ResponseHandler.FAILURE_REASON_NO_INTERNET);
		}
		
	}

	public void postData(ContentValues values) {
		Context context = TodoApplication.getContext();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String baseUrl = prefs.getString("serverPref", "http://10.0.2.2:8080");

		String url = baseUrl + "/TodoServer/task/save";
		if (values.containsKey(Todos.TodoColumns.SERVER_TASK_ID)) {
			url = baseUrl + "/TodoServer/task/update/" + values.getAsString(Todos.TodoColumns.SERVER_TASK_ID);
		}
		Log.d(TAG, "using url: " + url);
		HttpPost httppost = new HttpPost(url);

		try {
			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			if (values.containsKey(Todos.TodoColumns.TITLE)) {
				nameValuePairs.add(new BasicNameValuePair("title", values.getAsString(Todos.TodoColumns.TITLE)));
			}
			if (values.containsKey(Todos.TodoColumns.DESCRIPTION)) {
				nameValuePairs.add(new BasicNameValuePair("description", values.getAsString(Todos.TodoColumns.DESCRIPTION)));
			}
			if (values.containsKey(Todos.TodoColumns.LATITUDE)) {
				nameValuePairs.add(new BasicNameValuePair("latitude", values.getAsString(Todos.TodoColumns.LATITUDE)));
			}
			if (values.containsKey(Todos.TodoColumns.LONGITUDE)) {
				nameValuePairs.add(new BasicNameValuePair("longitude", values.getAsString(Todos.TodoColumns.LONGITUDE)));
			}
			if (values.containsKey(Todos.TodoColumns.DONE)) {
				nameValuePairs.add(new BasicNameValuePair("isDone", values.getAsString(Todos.TodoColumns.DONE)));
			}
			if (values.containsKey(Todos.TodoColumns.FAVOURITE)) {
				nameValuePairs.add(new BasicNameValuePair("isFavorite", values.getAsString(Todos.TodoColumns.FAVOURITE)));
			}
			if (values.containsKey(Todos.TodoColumns.FINISH_DATE)) {
				nameValuePairs.add(new BasicNameValuePair("finishDate", values.getAsString(Todos.TodoColumns.FINISH_DATE)));
			}
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			//postQueue.add(httppost);
			// TODO do this async
			HttpResponse response = httpclient.execute(httppost);

			// if we have a valid response, update our local database
			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = slurp(response.getEntity().getContent());
			if (statusCode == 200 && !TextUtils.isEmpty(responseBody) && !responseBody.equalsIgnoreCase("-1")) {
				Log.d(TAG, "Status code: " + statusCode);
				Log.d(TAG, "response from server: " + responseBody);
				if (TextUtils.isDigitsOnly(responseBody)) {
					int serverTaskId = Integer.parseInt(responseBody);
					values.put(Todos.TodoColumns.SERVER_TASK_ID, serverTaskId);
					values.put(Todos.TodoColumns.IS_SYNCED, true);
					if (values.containsKey(Todos.TodoColumns._ID)) {
						Long taskId = values.getAsLong(Todos.TodoColumns._ID);
						Uri updateUri = ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, taskId);
						TodoApplication.getContext().getContentResolver().update(updateUri, values, null, null);
						Toast.makeText(context, R.string.sync_success, Toast.LENGTH_SHORT).show();
					}
					else {
						Log.e(TAG, "could not update our local task. no task id was provided");
						Toast.makeText(context, R.string.sync_failure, Toast.LENGTH_SHORT).show();
					}
				}
			}

		}
		catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}

	/**
	 * Converts a InputStream to an String
	 * 
	 * @param in
	 *          - the InputStream to read from
	 * @return
	 * @throws IOException
	 */
	public static String slurp(InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return out.toString();
	}

	/**
	 * Checks if a Internet Connection is available
	 * 
	 * @return true if a connection is available, otherwise false
	 */
	private boolean haveInternetConnection() {
		ConnectivityManager connMgr = (ConnectivityManager) TodoApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		boolean connection = info != null && info.isConnected();

		return connection;
	}
}
