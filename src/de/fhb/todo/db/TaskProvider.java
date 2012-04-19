package de.fhb.todo.db;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import de.fhb.todo.db.Todos.TodoColumns;
import de.fhb.todo.db.Todos.TodoContactColumns;
import de.fhb.todo.net.ServerSyncer;

/**
 * Provides access to a database of notes. Each note has a title, the note itself, a creation date and a modified data.
 */
public class TaskProvider extends ContentProvider {

	private static final String TAG = "TaskProvider";

	private static final String DATABASE_NAME = "todo.db";
	private static final int DATABASE_VERSION = 8;
	private static final String TODO_TABLE_NAME = "todos";
	private static final String TODO_CONTACT_TABLE_NAME = "contacts";

	private static HashMap<String, String> sTaskProjectionMap;
	private static HashMap<String, String> sTaskContactProjectionMap;

	private static final int TASK = 1;
	private static final int TASK_ID = 2;
	private static final int TASK_CONTACT = 3;
	private static final int TASK_CONTACT_ID = 4;

	private static final UriMatcher sUriMatcher;

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TODO_TABLE_NAME + " (" + TodoColumns._ID + " INTEGER PRIMARY KEY," + TodoColumns.TITLE + " TEXT,"
					+ TodoColumns.DESCRIPTION + " TEXT," + TodoColumns.FINISH_DATE + " INTEGER," + TodoColumns.DONE + " INTEGER,"
					+ TodoColumns.FAVOURITE + " INTEGER," + TodoColumns.LONGITUDE + " INTEGER," + TodoColumns.LATITUDE + " INTEGER,"
					+ TodoColumns.IS_SYNCED + " INTEGER," + TodoColumns.SERVER_TASK_ID + " INTEGER"
					//+ TodoColumns.USER_ID + " INTEGER"
					+ ");");
			db.execSQL("CREATE TABLE " + TODO_CONTACT_TABLE_NAME + " (" + TodoContactColumns._ID + " INTEGER PRIMARY KEY,"
					+ TodoContactColumns.TASK_ID + " INTEGER," + TodoContactColumns.CONTACT_URI + " TEXT" + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + TODO_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TODO_CONTACT_TABLE_NAME);
			onCreate(db);
		}
	}

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = TodoColumns.DEFAULT_SORT_ORDER;
		}
		else {
			orderBy = sortOrder;
		}

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String table = TODO_TABLE_NAME;

		switch (sUriMatcher.match(uri)) {
		case TASK:
			qb.setProjectionMap(sTaskProjectionMap);
			break;

		case TASK_ID:
			qb.setProjectionMap(sTaskProjectionMap);
			qb.appendWhere(TodoColumns._ID + "=" + uri.getPathSegments().get(1));
			break;
		case TASK_CONTACT:
			qb.setProjectionMap(sTaskContactProjectionMap);
			table = TODO_CONTACT_TABLE_NAME;
			orderBy = TodoContactColumns.DEFAULT_SORT_ORDER;
			break;

		case TASK_CONTACT_ID:
			qb.setProjectionMap(sTaskContactProjectionMap);
			qb.appendWhere(TodoContactColumns._ID + "=" + uri.getPathSegments().get(1));
			table = TODO_CONTACT_TABLE_NAME;
			orderBy = TodoContactColumns.DEFAULT_SORT_ORDER;
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		qb.setTables(table);

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case TASK:
		case TASK_ID:
			return TodoColumns.CONTENT_ITEM_TYPE;
		case TASK_CONTACT:
		case TASK_CONTACT_ID:
			return TodoContactColumns.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		String tableName = "";
		boolean isContact = false;
		// Validate the requested uri
		ContentValues values = new ContentValues();
		switch (sUriMatcher.match(uri)) {
		case TASK:
			tableName = TODO_TABLE_NAME;
			values = checkTaskValues(initialValues);
			break;

		case TASK_CONTACT:
			isContact = true;
			if (checkTaskContactValues(initialValues)) {
				tableName = TODO_CONTACT_TABLE_NAME;
				values = initialValues;
			} else {
				throw new IllegalArgumentException("values not correctly filled");
			}
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, values);
		if (rowId > 0) {
			values.put(TodoColumns._ID, rowId);
			Uri insertedUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertedUri, null);
			if (!isContact) {
				saveTaskToServer(values);
			}
			return insertedUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	private boolean checkTaskContactValues(ContentValues initialValues) {
		boolean valuesCorrectlyFilled = false;

		// Make sure that the fields are all set
		if (initialValues != null && initialValues.containsKey(TodoContactColumns.TASK_ID)
				&& initialValues.containsKey(TodoContactColumns.CONTACT_URI)) {
			valuesCorrectlyFilled = true;
		}

		return valuesCorrectlyFilled;
	}

	private ContentValues checkTaskValues(ContentValues initialValues) {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		}
		else {
			values = new ContentValues();
		}

		// Make sure that the fields are all set
		if (!values.containsKey(TodoColumns.TITLE)) {
			Resources r = Resources.getSystem();
			values.put(TodoColumns.TITLE, r.getString(android.R.string.untitled));
		}

		if (!values.containsKey(TodoColumns.DESCRIPTION)) {
			values.put(TodoColumns.DESCRIPTION, "");
		}
		if (!values.containsKey(TodoColumns.FINISH_DATE)) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(System.currentTimeMillis());
			// default auf morgigen Tag setzen
			cal.add(Calendar.DAY_OF_YEAR, 1);
			values.put(TodoColumns.FINISH_DATE, cal.getTimeInMillis());
		}
		if (!values.containsKey(TodoColumns.DONE)) {
			values.put(TodoColumns.DONE, false);
		}
		if (!values.containsKey(TodoColumns.FAVOURITE)) {
			values.put(TodoColumns.FAVOURITE, false);
		}
		if (!values.containsKey(TodoColumns.LATITUDE)) {
			double[] position = getGPS();
			values.put(TodoColumns.LATITUDE, position[0]);
			values.put(TodoColumns.LONGITUDE, position[1]);
		}
		return values;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TASK:
			count = db.delete(TODO_TABLE_NAME, where, whereArgs);
			break;

		case TASK_ID:
			String taskId = uri.getPathSegments().get(1);
			count = db.delete(TODO_TABLE_NAME, TodoColumns._ID + "=" + taskId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;
		case TASK_CONTACT:
			count = db.delete(TODO_CONTACT_TABLE_NAME, where, whereArgs);
			break;
			
		case TASK_CONTACT_ID:
			String todoContactEntryId = uri.getPathSegments().get(1);
			count = db.delete(TODO_CONTACT_TABLE_NAME, TodoContactColumns._ID + "=" + todoContactEntryId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case TASK:
			count = db.update(TODO_TABLE_NAME, values, where, whereArgs);
			break;

		case TASK_ID:
			String noteId = uri.getPathSegments().get(1);
			count = db.update(TODO_TABLE_NAME, values,
					TodoColumns._ID + "=" + noteId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			if (values.containsKey(TodoColumns.IS_SYNCED)) {
				boolean isSynced = values.getAsBoolean(TodoColumns.IS_SYNCED);
				if (!isSynced) {
					values.put(TodoColumns._ID, noteId);
					saveTaskToServer(values);
				}
			}
			break;
		case TASK_CONTACT:
			count = db.update(TODO_CONTACT_TABLE_NAME, values, where, whereArgs);
			break;
			
		case TASK_CONTACT_ID:
			String contactEntryId = uri.getPathSegments().get(1);
			count = db.update(TODO_CONTACT_TABLE_NAME, values,
					TodoContactColumns._ID + "=" + contactEntryId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private void saveTaskToServer(ContentValues values) {
		if (haveInternetConnection()) {
			ServerSyncer.getInstance().postData(values);
		}
	}

	/**
	 * Checks if a Internet Connection is available
	 * 
	 * @return true if a connection is available, otherwise false
	 */
	private boolean haveInternetConnection() {
		ConnectivityManager connMgr = (ConnectivityManager) this.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connMgr.getActiveNetworkInfo();
		boolean connection = info != null && info.isConnected();

		return connection;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(Todos.AUTHORITY, "tasks", TASK);
		sUriMatcher.addURI(Todos.AUTHORITY, "tasks/#", TASK_ID);
		sUriMatcher.addURI(Todos.AUTHORITY, "contacts", TASK_CONTACT);
		sUriMatcher.addURI(Todos.AUTHORITY, "contacts/#", TASK_CONTACT_ID);

		sTaskProjectionMap = new HashMap<String, String>();
		sTaskProjectionMap.put(TodoColumns._ID, TodoColumns._ID);
		sTaskProjectionMap.put(TodoColumns.TITLE, TodoColumns.TITLE);
		sTaskProjectionMap.put(TodoColumns.DESCRIPTION, TodoColumns.DESCRIPTION);
		sTaskProjectionMap.put(TodoColumns.FINISH_DATE, TodoColumns.FINISH_DATE);
		sTaskProjectionMap.put(TodoColumns.DONE, TodoColumns.DONE);
		sTaskProjectionMap.put(TodoColumns.FAVOURITE, TodoColumns.FAVOURITE);
		sTaskProjectionMap.put(TodoColumns.LATITUDE, TodoColumns.LATITUDE);
		sTaskProjectionMap.put(TodoColumns.LONGITUDE, TodoColumns.LONGITUDE);
		sTaskProjectionMap.put(TodoColumns.IS_SYNCED, TodoColumns.IS_SYNCED);
		//sTaskProjectionMap.put(TodoColumns.USER_ID, TodoColumns.USER_ID);
		sTaskProjectionMap.put(TodoColumns.SERVER_TASK_ID, TodoColumns.SERVER_TASK_ID);

		sTaskContactProjectionMap = new HashMap<String, String>();
		sTaskContactProjectionMap.put(TodoContactColumns._ID, TodoContactColumns._ID);
		sTaskContactProjectionMap.put(TodoContactColumns.TASK_ID, TodoContactColumns.TASK_ID);
		sTaskContactProjectionMap.put(TodoContactColumns.CONTACT_URI, TodoContactColumns.CONTACT_URI);
	}

	/**
	 * This is a fast code to get the last known location of the phone. If there is no exact gps-information it falls back to the
	 * network-based location info.
	 * 
	 * @return array with latitude and longitude. array[0] = latitude, array[1] = longitude
	 */
	private double[] getGPS() {
		LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getAllProviders();

		/* Loop over the array backwards, and if you get an accurate location, then break out the loop */
		Location l = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			l = lm.getLastKnownLocation(providers.get(i));
			if (l != null)
				break;
		}

		double[] gps = new double[2];
		if (l != null) {
			gps[0] = l.getLatitude();
			gps[1] = l.getLongitude();
		}
		return gps;
	}
}
