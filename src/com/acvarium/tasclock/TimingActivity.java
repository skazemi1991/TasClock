package com.acvarium.tasclock;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class TimingActivity extends Activity implements OnClickListener,
		OnLongClickListener {

	final String LOG_TAG = "myLogs";
	final String NameSTable = "tasks_timing";
	private ImageButton startBtn, editBtn, resetBtn;
	private TextView mainTV;
	private Handler myHandler = new Handler();
	private ListView list;
	private ArrayAdapter<TimePeriods> listAdapter;
	private TimePeriods timePeriods;
	private SharedPreferences sPref;
	private Calendar cal;
	private Editor ed;
	private String label;
	private int sElenetPosition = -1;
	private Intent intent;

	private TimingDB timingDB;
	private SQLiteDatabase tDB;

	private SimpleDateFormat timeFormat;
	private SimpleDateFormat dateFormat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.timing);

		intent = getIntent();
		label = intent.getStringExtra("name");
		setTitle(label);

		timingDB = new TimingDB(this);
		tDB = timingDB.getWritableDatabase();

		timePeriods = new TimePeriods(label);

		timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
		dateFormat = new SimpleDateFormat("dd MM yyyy", Locale.US);
		cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);

		startBtn = (ImageButton) findViewById(R.id.start_button);
		editBtn = (ImageButton) findViewById(R.id.edit_button);
		resetBtn = (ImageButton) findViewById(R.id.reset_button);
		mainTV = (TextView) findViewById(R.id.mainTV);

		list = (ListView) findViewById(R.id.lvTimes);

		listAdapter = new CustomListAdapter(this, R.layout.list_time);
		list.setAdapter(listAdapter);

		startBtn.setOnClickListener(this);
		editBtn.setOnClickListener(this);
		resetBtn.setOnClickListener(this);
		resetBtn.setOnLongClickListener(this);
		readData();
		showTP();

		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				sElenetPosition = position;

			}
		});

		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				return true;
			}
		});

	}

	private void showTP() {
		mainTV.setText(timeToString(timePeriods.getSumOfAllPeriods()));
	}

	private String timeToString(long time) {
		time = time / 1000;
		String ss = String.format("%02d:%02d:%02d", time / 3600,
				(time % 3600) / 60, (time % 60), Locale.US);
		return ss;
	}

	private String perionToString(int period) {
		long c = timePeriods.getSumOfPeriod(period);
		return timeToString(c);
	}

	private Runnable updateTimerMethod = new Runnable() {

		public void run() {
			showTP();
			myHandler.postDelayed(this, 1000);
		}

	};

	class CustomListAdapter extends ArrayAdapter<TimePeriods> {

		public CustomListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);

		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			String ss = "";
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.list_time,
						null);
			}
			((TextView) convertView.findViewById(R.id.title))
					.setText(perionToString(position));

			cal.setTimeInMillis(timePeriods.getStartTime(position));
			cal.setTimeInMillis(timePeriods.getStartTime(position));

			ss = timeFormat.format(cal.getTime());
			((TextView) convertView.findViewById(R.id.start_time_title))
					.setText(ss);
			ss = dateFormat.format(cal.getTime());
			((TextView) convertView.findViewById(R.id.start_date_title))
					.setText(ss);

			cal.setTimeInMillis(timePeriods.getEndTime(position));
			ss = timeFormat.format(cal.getTime());
			((TextView) convertView.findViewById(R.id.end_time_title))
					.setText(ss);
			ss = dateFormat.format(cal.getTime());
			((TextView) convertView.findViewById(R.id.end_date_title))
					.setText(ss);

			return convertView;
		}
	}

	private void readData() {

		listAdapter.clear();
		timePeriods.clear();
		listAdapter.clear();

		Log.d(LOG_TAG, "--- Read data: ---");

		// Робимо запрос всіх даинх з таблиці, получаємо Cursor
		Cursor c = tDB.query(NameSTable, null, "name = ?",
				new String[] { label }, null, null, null);

		// ставимо позицію курсора на першу строку виборки
		// якщо в виборці немає строк, то false

		if (c.moveToFirst()) {
			// визначаємо номер стовбця по виборці
			int idColIndex = c.getColumnIndex("id");
			int nameColIndex = c.getColumnIndex("name");
			int startColIndex = c.getColumnIndex("start");
			int endColIndex = c.getColumnIndex("end");
			do {
				timePeriods.add(c.getLong(startColIndex),
						c.getLong(endColIndex));
				listAdapter.add(timePeriods);
			} while (c.moveToNext());
		} else
			Log.d(LOG_TAG, "0 rows");
		c.close();

		listAdapter.notifyDataSetChanged();

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_button:

			if (timePeriods.tpStarted) { // --STOP---

				timePeriods.stop();
				startBtn.setImageResource(R.drawable.play);
				startBtn.setBackgroundResource(R.drawable.buttonshape);
				myHandler.removeCallbacks(updateTimerMethod);
				listAdapter.add(timePeriods);
				listAdapter.notifyDataSetChanged();
				showTP();

				ContentValues cv = new ContentValues();
				cv.put("name", label);
				cv.put("start",
						timePeriods.getStartTime(timePeriods.getSize() - 1));
				cv.put("end", timePeriods.getEndTime(timePeriods.getSize() - 1));
				long rowID = tDB.insert(NameSTable, null, cv);
				Log.d(LOG_TAG, "row inserted, ID = " + rowID);

			} else { // --START---

				timePeriods.start();
				startBtn.setImageResource(R.drawable.stop);
				startBtn.setBackgroundResource(R.drawable.stopbuttonshape);
				myHandler.postDelayed(updateTimerMethod, 0);
				showTP();
			}

			break;
		case R.id.edit_button:
			Log.d(LOG_TAG, "--- Rows in mytable: ---");
			// Робимо запрос всіх даинх з таблиці, получаємо Cursor
			Cursor c = tDB
					.query(NameSTable, null, null, null, null, null, null);

			// ставимо позицію курсора на першу строку виборки
			// якщо в виборці немає строк, то false
			if (c.moveToFirst()) {

				// визначаємо номер стовбця по виборці
				int idColIndex = c.getColumnIndex("id");
				int nameColIndex = c.getColumnIndex("name");
				int startColIndex = c.getColumnIndex("start");
				int endColIndex = c.getColumnIndex("end");

				do {
					// отримуємо значення по номерам стовбців і пишемо все в лог
					Log.d(LOG_TAG,
							"ID = " + c.getInt(idColIndex) + ", name = "
									+ c.getString(nameColIndex) + ", time = "
									+ c.getLong(startColIndex) + ", comment = "
									+ c.getLong(endColIndex));
					// перехід на наступну строку
					// а якщо наступної нема (поточна остання), то false -
					// виходимо з циклу
				} while (c.moveToNext());
			} else
				Log.d(LOG_TAG, "0 rows");
			c.close();
			break;
		case R.id.reset_button:

			if (sElenetPosition >= 0) {
				Log.d(LOG_TAG, "Rmove item No " + sElenetPosition);
				Log.d(LOG_TAG,
						"Element = "
								+ timePeriods.getSumOfPeriod(sElenetPosition));
				int clearCount = tDB.delete(NameSTable, "start = ?",
						new String[] { String.valueOf(timePeriods
								.getStartTime(sElenetPosition)) });
				timePeriods.remove(sElenetPosition);
				listAdapter.remove(listAdapter.getItem(sElenetPosition));
				listAdapter.notifyDataSetChanged();

				sElenetPosition = -1;
			}

			break;

		default:
			break;
		}

	}

	@Override
	public boolean onLongClick(View v) {
		switch (v.getId()) {
		case R.id.reset_button:

			Log.d(LOG_TAG, "--- Clear mytable: ---");
			// Видаляємо всі записи
			int clearCount = tDB.delete(NameSTable, "name = ?",
					new String[] { label });
			Log.d(LOG_TAG, "deleted rows count = " + clearCount);
			listAdapter.clear();
			timePeriods.clear();
			listAdapter.notifyDataSetChanged();
			showTP();
			break;

		default:
			break;
		}
		return false;
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "Pause ");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "Resume ");
		

	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(LOG_TAG, "Stop ");

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy ");
	}

	// Робота з базою данних
	class TimingDB extends SQLiteOpenHelper {

		public TimingDB(Context context) {
			super(context, "db", null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}

}
