package de.fhb.todo.view;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import de.fhb.todo.R;
import de.fhb.todo.db.Todos;
import de.fhb.todo.db.Todos.TodoContactColumns;

/**
 * Created by IntelliJ IDEA. User: phr Date: 21.12.11 Time: 15:57 To change this
 * template use File | Settings | File Templates.
 */
public class EditTaskActivity extends ListActivity {
	private final static String LOG_TAG = EditTaskActivity.class
			.getSimpleName();
	private Uri taskUri;
	private Cursor taskCursor;
	private String serverTaskId;
	private final static int CONTACT_PICKER_RESULT = 1289379128; // really
																	// random, i
																	// swear!
	private static final int DATE_DIALOG_ID = 0;
	private static final int TIME_DIALOG_ID = 1;
	private TextView mDateDisplay;
	private TextView mTimeDisplay;
	private Calendar finishDateCal;
	private List<String> contactsForTask;
	private List<Uri> contactsUriForTask;
	private ArrayAdapter<String> listAdapter;
	private String taskId;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.taskdetail);
		contactsForTask = new ArrayList<String>();
		contactsUriForTask = new ArrayList<Uri>();
		mDateDisplay = (TextView) findViewById(R.id.tvFinishDate);
		mTimeDisplay = (TextView) findViewById(R.id.tvFinishTime);
		finishDateCal = Calendar.getInstance();

		taskUri = getIntent().getData();
		fillView(taskUri);

		listAdapter = new ArrayAdapter<String>(this,
				R.layout.contactlist_entry, contactsForTask);
		setListAdapter(listAdapter);

		// Alle Kontakte zu dem todo aus der Datenbank holen
		taskId = taskUri.getPathSegments().get(1);
		Log.d(LOG_TAG, "get contacts for task: " + taskId);
		Cursor c = getContentResolver().query(TodoContactColumns.CONTENT_URI,
				null, TodoContactColumns.TASK_ID + "=" + taskId, null, null);
		if (c.getCount() > 0) {
			Log.d(LOG_TAG, "found contacts for task: " + c.getCount());
			while (!c.isLast()) {
				c.moveToNext();
				Uri contactUri = Uri.parse(c.getString(c
						.getColumnIndex(TodoContactColumns.CONTACT_URI)));
				addContactToList(contactUri);
			}
		}
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// When clicked, show a toast with the TextView text
				Toast.makeText(
						getApplicationContext(),
						((TextView) view).getText()
								+ EditTaskActivity.this.contactsUriForTask.get(
										position).toString(),
						Toast.LENGTH_SHORT).show();
			}
		});

		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Uri contactUri = EditTaskActivity.this.contactsUriForTask
						.get(position);
				deleteContactFromList(contactUri, position);
				return true;
			}

		});
	}

	// the callback received when the user "sets" the date in the dialog
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			finishDateCal.set(Calendar.YEAR, year);
			finishDateCal.set(Calendar.MONTH, monthOfYear);
			finishDateCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			updateDateDisplay(year, monthOfYear, dayOfMonth);
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			finishDateCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
			finishDateCal.set(Calendar.MINUTE, minute);
			updateTimeDisplay(hourOfDay, minute);

		}
	};

	/**
	 * Holt den aktuellen Task aus der DB und fÃ¼llt die View mit Werten
	 * 
	 * @param taskUri
	 */
	private void fillView(Uri taskUri) {
		if (taskUri != null) {
			taskCursor = managedQuery(taskUri, null, null, null, null);
			taskCursor.moveToFirst();
			String title = taskCursor.getString(taskCursor
					.getColumnIndex(Todos.TodoColumns.TITLE));
			String description = taskCursor.getString(taskCursor
					.getColumnIndex(Todos.TodoColumns.DESCRIPTION));
			String finishDate = taskCursor.getString(taskCursor
					.getColumnIndex(Todos.TodoColumns.FINISH_DATE));
			int currentState = taskCursor.getInt(taskCursor
					.getColumnIndex(Todos.TodoColumns.DONE));
			this.serverTaskId = taskCursor.getString(taskCursor
					.getColumnIndex(Todos.TodoColumns.SERVER_TASK_ID));
			toggleDoneButton(currentState == 0 ? true : false);
			Log.d(LOG_TAG, title + currentState);
			((EditText) findViewById(R.id.etTaskTitle)).setText(title);
			((EditText) findViewById(R.id.etTaskDescription))
					.setText(description);
			finishDateCal.setTimeInMillis(Long.parseLong(finishDate));

			updateDateDisplay(finishDateCal.get(Calendar.YEAR),
					finishDateCal.get(Calendar.MONTH),
					finishDateCal.get(Calendar.DAY_OF_MONTH));
			updateTimeDisplay(finishDateCal.get(Calendar.HOUR_OF_DAY),
					finishDateCal.get(Calendar.MINUTE));

			setTitle(title);
		}
	}

	private void toggleDoneButton(boolean newState) {
		((Button) findViewById(R.id.btnTaskDone))
				.setText(newState ? R.string.btnTaskDone
						: R.string.btnTaskNotDone);
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btnTaskDone:
			toggleDoneState();
			break;
		case R.id.btnTaskDelete:
			deleteTask();
			break;
		case R.id.btnTaskSave:
			saveTask();
			break;
		case R.id.tvFinishDate:
			showDialog(DATE_DIALOG_ID);
			break;
		case R.id.tvFinishTime:
			showDialog(TIME_DIALOG_ID);
			break;
		case R.id.btnAddContact:
			pickContact();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DATE_DIALOG_ID:
			final Calendar c = Calendar.getInstance();
			return new DatePickerDialog(this, mDateSetListener,
					c.get(Calendar.YEAR), c.get(Calendar.MONTH),
					c.get(Calendar.DAY_OF_MONTH));
		case TIME_DIALOG_ID:
			final Calendar cal = Calendar.getInstance();
			return new TimePickerDialog(this, mTimeSetListener,
					cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					true);
		}
		return null;
	}

	// updates the date in the TextView
	private void updateDateDisplay(int year, int month, int day) {
		mDateDisplay.setText(new StringBuilder()
				// Month is 0 based so add 1
				.append(day).append(".").append(month + 1).append(".")
				.append(year).append(" "));
	}

	private void updateTimeDisplay(int hourOfDay, int minute) {
		String convertedMinute = minute < 10 ? "0" + minute : minute + "";
		mTimeDisplay.setText(new StringBuilder()
				// Month is 0 based so add 1
				.append(hourOfDay).append(":").append(convertedMinute)
				.append(" "));
	}

	private void saveTask() {
		String title = ((EditText) findViewById(R.id.etTaskTitle)).getText()
				.toString();
		if (title != null && title.length() > 0) {
			saveTask(title);
			saveContacts();
			finish();
		} else {
			Toast.makeText(this, R.string.toast_task_no_title,
					Toast.LENGTH_LONG).show();
		}
	}

	private void saveContacts() {
		for (Uri contactUri : this.contactsUriForTask) {
			ContentValues values = new ContentValues();
			values.put(TodoContactColumns.CONTACT_URI, contactUri.toString());
			values.put(TodoContactColumns.TASK_ID, this.serverTaskId);
			getContentResolver().insert(TodoContactColumns.CONTENT_URI, values);
		}

	}

	private void saveTask(String title) {
		ContentValues values = new ContentValues();
		String description = ((EditText) findViewById(R.id.etTaskDescription))
				.getText().toString();
		values.put(Todos.TodoColumns.TITLE, title);
		values.put(Todos.TodoColumns.DESCRIPTION, description);
		values.put(Todos.TodoColumns.IS_SYNCED, false);
		values.put(Todos.TodoColumns.FINISH_DATE,
				finishDateCal.getTimeInMillis());
		if (!TextUtils.isEmpty(this.serverTaskId)) {
			values.put(Todos.TodoColumns.SERVER_TASK_ID, this.serverTaskId);
		}
		getContentResolver().update(taskUri, values, null, null);
	}

	private void toggleDoneState() {
		if (taskUri != null) {
			int currentState = taskCursor.getInt(taskCursor
					.getColumnIndex(Todos.TodoColumns.DONE));
			boolean newState = currentState == 0 ? true : false;
			ContentValues values = new ContentValues();
			values.put(Todos.TodoColumns.DONE, newState);
			getContentResolver().update(taskUri, values, null, null);
			toggleDoneButton(newState);
		}
	}

	private void deleteTask() {
		if (taskUri != null) {
			getContentResolver().delete(taskUri, null, null);
			finish();
		}
	}

	public void pickContact() {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Uri contactUri = data.getData();
				addContactToList(contactUri);

				break;
			}
		}
	}

	/**
	 * fügt einen kontakt zu dem listadapter der kontaktliste hinzu
	 * 
	 * @param contactUri
	 */
	private void addContactToList(Uri contactUri) {
		Cursor c = getContentResolver().query(contactUri,
				new String[] { Contacts.DISPLAY_NAME }, null, null, null);
		try {
			c.moveToFirst();
			String displayName = c.getString(0);
			if (!TextUtils.isEmpty(displayName)) {
				this.listAdapter.add(displayName);
				this.listAdapter.notifyDataSetChanged();
				contactsUriForTask.add(contactUri);
			}
		} finally {
			c.close();
		}
	}

	protected void deleteContactFromList(Uri contactUri, int position) {
		int rowsDeleted = getContentResolver().delete(
				TodoContactColumns.CONTENT_URI,
				TodoContactColumns.CONTACT_URI + "=? AND "
						+ TodoContactColumns.TASK_ID + "=?",
				new String[] { contactUri.toString(), taskId });
		Log.d(LOG_TAG, "rows deleted. " + rowsDeleted + ", delete item " + position);
		this.listAdapter.remove(this.listAdapter.getItem(position));
		this.listAdapter.notifyDataSetChanged();
		contactsUriForTask.remove(contactUri);
	}
}
