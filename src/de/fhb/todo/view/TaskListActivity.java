package de.fhb.todo.view;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import de.fhb.todo.R;
import de.fhb.todo.SimpleTodoCursorAdapter;
import de.fhb.todo.db.Todos;
import de.fhb.todo.db.Todos.TodoColumns;

/**
 * Created by IntelliJ IDEA. Date: 21.12.11 Time: 11:42 To change this template use File | Settings | File Templates.
 */
public class TaskListActivity extends ListActivity {

	private final static String TAG = TaskListActivity.class.getSimpleName();

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listview);
		fillList();
	}

	private void fillList() {
		// alle Tasks aus der DB holen
		Cursor c = getContentResolver().query(Todos.TodoColumns.CONTENT_URI, null, null, null, TodoColumns.SORT_ORDER_DATE_FAVORITE);
		startManagingCursor(c);

		String[] from = new String[] { Todos.TodoColumns.TITLE, Todos.TodoColumns.FINISH_DATE };
		int[] to = new int[] { R.id.tvTaskTitle, R.id.tvFinishDate };

		// Now create an array adapter and set it to display using our row
		//        SimpleCursorAdapter tasks =
		//              new SimpleCursorAdapter(this, R.layout.listentry, c, from, to);
		SimpleTodoCursorAdapter tasks = new SimpleTodoCursorAdapter(this, R.layout.listentry, c, from, to);
		setListAdapter(tasks);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Uri taskUri = ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, id);
		startActivity(new Intent(Intent.ACTION_EDIT, taskUri));
	}

	public void addTask(View view) {
		if (view.getId() == R.id.btnNewTask) {
			String title = ((EditText) findViewById(R.id.etNewTask)).getText().toString();
			if (!TextUtils.isEmpty(title)) {
				ContentValues values = new ContentValues();
				values.put(Todos.TodoColumns.TITLE, title);
				values.put(Todos.TodoColumns.IS_SYNCED, false);
				Uri newInsertUri = getContentResolver().insert(Todos.TodoColumns.CONTENT_URI, values);
				Log.d(TAG, newInsertUri.toString());
			}
		}
	}
}
