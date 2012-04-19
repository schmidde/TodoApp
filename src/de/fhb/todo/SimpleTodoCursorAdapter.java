package de.fhb.todo;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import de.fhb.todo.db.Todos;
import de.fhb.todo.db.Todos.TodoColumns;

/**
 * Eigene Adapter Klasse um den Anforderungen der Anwendung gerecht zu werden. Sie managed die einzelnen Listenelementen. So wird unter
 * anderem die Hintergrundfarbe bei überfälligen Tasks anders eingefärbt
 */
public class SimpleTodoCursorAdapter extends SimpleCursorAdapter {
	private final static String TAG = SimpleTodoCursorAdapter.class.getSimpleName();
	private Cursor c;
	private Context context;

	public SimpleTodoCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.c = c;
		this.context = context;
	}

	public View getView(final int pos, View inView, ViewGroup parent) {
		if (c.getCount() > pos) {
			if (inView == null) {
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				inView = inflater.inflate(R.layout.listentry, null);
			}
			TextView tvTaskTitle = (TextView) inView.findViewById(R.id.tvTaskTitle);
			TextView tvFinishDate = (TextView) inView.findViewById(R.id.tvFinishDate);
			LinearLayout llTaskEntry = (LinearLayout) inView.findViewById(R.id.llTaskEntry);

			Calendar cal = Calendar.getInstance();

			c.moveToPosition(pos);
			cal.setTimeInMillis(Long.parseLong(c.getString(c.getColumnIndex(Todos.TodoColumns.FINISH_DATE))));

			Calendar currentTime = Calendar.getInstance();
			if (currentTime.after(cal)) {
				llTaskEntry.setBackgroundColor(Color.RED);
			}

			Format formatter = new SimpleDateFormat("dd.MMM.yy HH:mm:ss");

			tvTaskTitle.setText(c.getString(c.getColumnIndex(Todos.TodoColumns.TITLE)));
			tvFinishDate.setText(formatter.format(cal.getTime()));

			final CheckBox cbTaskDone = (CheckBox) inView.findViewById(R.id.cbTaskDone);
			cbTaskDone.setTag(c.getString(c.getColumnIndex(TodoColumns._ID)));
			// this will Check or Uncheck the CheckBox in ListView according to their original position and CheckBox never loss his State when you Scroll the List Items.
			cbTaskDone.setChecked(c.getInt(c.getColumnIndex(Todos.TodoColumns.DONE)) == 1 ? true : false);
			cbTaskDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean newState) {
					Log.d(TAG, "checkbox using id: " + pos + " newstate: " + newState);
					CheckBox cb = (CheckBox) compoundButton;
					Cursor cursor = context.getContentResolver().query(
							ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, Long.parseLong(cb.getTag().toString())),
							new String[] { TodoColumns.DONE }, null, null, null);
					if (cursor.getCount() == 1) {
						cursor.moveToFirst();
						if (newState != (cursor.getInt(0) == 1 ? true : false)) {
							Log.d(TAG, "update");
							ContentValues values = new ContentValues();
							values.put(Todos.TodoColumns.DONE, newState);
							Uri taskUri = ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, Long.parseLong(cb.getTag().toString()));
							context.getContentResolver().update(taskUri, values, null, null);
						}
					}
				}
			});

			final CheckBox cbFavourite = (CheckBox) inView.findViewById(R.id.cbFavorite);
			cbFavourite.setTag(c.getString(c.getColumnIndex(TodoColumns._ID)));
			cbFavourite.setChecked(c.getInt(c.getColumnIndex(Todos.TodoColumns.FAVOURITE)) == 1 ? true : false);
			cbFavourite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton compoundButton, boolean newState) {
					Log.d(TAG, "checkbox using id: " + pos + " newstate: " + newState);
					CheckBox cb = (CheckBox) compoundButton;
					Cursor cursor = context.getContentResolver().query(
							ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, Long.parseLong(cb.getTag().toString())),
							new String[] { TodoColumns.FAVOURITE }, null, null, null);
					if (cursor.getCount() == 1) {
						cursor.moveToFirst();
						if (newState != (cursor.getInt(0) == 1 ? true : false)) {
							Log.d(TAG, "update");
							ContentValues values = new ContentValues();
							values.put(Todos.TodoColumns.FAVOURITE, newState);
							Uri taskUri = ContentUris.withAppendedId(Todos.TodoColumns.CONTENT_URI, Long.parseLong(cb.getTag().toString()));
							context.getContentResolver().update(taskUri, values, null, null);
						}
					}
				}
			});
		}
		return inView;
	}
}
