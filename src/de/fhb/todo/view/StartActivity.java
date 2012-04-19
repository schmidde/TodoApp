package de.fhb.todo.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.fhb.todo.R;
import de.fhb.todo.net.ResponseHandler;
import de.fhb.todo.net.ServerSyncer;

public class StartActivity extends Activity {
	private EditText etEMail;
	private EditText etPassword;
	private TextView tvError;
	private Button btnLogin;
	private boolean isInErrorMode = false;
	private boolean hasEmailEntered = false;
	private boolean hasPasswordEntered = false;
	private int failureReason;
	private ProgressDialog progressDialog;
	private Handler updateUIHandler;
	private Runnable successRunnable;
	private Runnable failureRunnable;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		// handler für unser UI update aus einem anderen thread heraus
		updateUIHandler = new Handler();
		// runnable die vom Handler ausgeführt wird
		successRunnable = new Runnable() {

			@Override
			public void run() {
				// ACHTUNG!!! Das wird nicht auf dem UI Thread ausgeführt -> daher an den UI Thread Handler übergeben
				progressDialog.dismiss();
				Intent i = new Intent(StartActivity.this, TaskListActivity.class);
				startActivity(i);
			}
		};

		// runnable die vom Handler ausgeführt wird
		failureRunnable = new Runnable() {

			@Override
			public void run() {
				// ACHTUNG!!! Das wird nicht auf dem UI Thread ausgeführt -> daher an den UI Thread Handler übergeben
				progressDialog.dismiss();
				switch (failureReason) {
				case ResponseHandler.FAILURE_REASON_NO_INTERNET:
					Toast.makeText(StartActivity.this, R.string.err_no_internet, Toast.LENGTH_SHORT).show();
					break;
				case ResponseHandler.FAILURE_REASON_WRONG_CREDENTIALS:
				default:
					showError(R.string.err_wrong_credentials);
					break;
				}

			}
		};

		// Da unser Login Asynchron erfolgt, hier ein Handler der im UI Thread läuft
		tvError = (TextView) findViewById(R.id.tvError);
		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnLogin.setEnabled(false);
		etEMail = (EditText) findViewById(R.id.etUsername);
		etEMail.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				dismissError();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 0) {
					hasEmailEntered = true;
				}
				else {
					hasEmailEntered = false;
				}
				toggleButton();
			}
		});
		etPassword = (EditText) findViewById(R.id.etPassword);
		// workaround weil 2 inputtype methoden im xml nicht funktionieren
		etPassword.setInputType(InputType.TYPE_CLASS_NUMBER);
		etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
		etPassword.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				dismissError();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// interessiert uns nicht
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 0) {
					hasPasswordEntered = true;
				}
				else {
					hasPasswordEntered = false;
				}
				toggleButton();
			}
		});

	}

	protected void toggleButton() {
		if (hasEmailEntered && hasPasswordEntered) {
			btnLogin.setEnabled(true);
		}
		else {
			btnLogin.setEnabled(false);
		}

	}

	protected void dismissError() {
		if (isInErrorMode) {
			tvError.setVisibility(View.INVISIBLE);
			isInErrorMode = false;
		}

	}

	/**
	 * Login methode, Clickhandler wird nicht benötigt, da diese Methode in der XML definiert wurde
	 * 
	 * @param view
	 *          die View, die angeklickt wurde
	 */
	public void doLogin(View view) {
		if (view.getId() == R.id.btnLogin) {
			String email = etEMail.getText().toString();
			if (isValidEmail(email)) {
				String password = etPassword.getText().toString();
				if (password.length() == 6) {
					checkPassword(email, password);
				}
				else {
					showError(R.string.err_wrong_credentials);
				}
			}
			else {
				showError(R.string.err_no_mail);
			}
		}
	}

	private void checkPassword(String email, String password) {
		Log.d("test", "checkPassword");
		progressDialog = ProgressDialog.show(StartActivity.this, "", "Check Login. Please wait...", true);
		ServerSyncer.getInstance().checkLogin(email, password, new ResponseHandler() {

			/**
			 * Callback vom Login Thread, wird nicht im UI Thread ausgeführt
			 */
			@Override
			public void successfull() {
				// ACHTUNG!!! Das wird nicht auf dem UI Thread ausgeführt -> daher an den UI Thread Handler übergeben
				updateUIHandler.post(successRunnable);
			}

			/**
			 * Callback vom Login Thread, wird nicht im UI Thread ausgeführt
			 */
			@Override
			public void failure(int reason) {
				// ACHTUNG!!! Das wird nicht auf dem UI Thread ausgeführt -> daher an den UI Thread Handler übergeben
				failureReason = reason;
				updateUIHandler.post(failureRunnable);
			}
		});
	}

	private void showError(int errorId) {
		tvError.setText(errorId);
		isInErrorMode = true;
		tvError.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the currently selected menu XML resource.
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, PrefActivity.class));
			break;
		}
		return true;
	}

	public final static boolean isValidEmail(CharSequence target) {
		try {
			return Patterns.EMAIL_ADDRESS.matcher(target).matches();
		}
		catch (NullPointerException exception) {
			return false;
		}
	}
}
