package de.fhb.todo.view;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import de.fhb.todo.R;

/**
 * Created by IntelliJ IDEA.
 * User: phr
 * Date: 25.01.12
 * Time: 19:45
 * To change this template use File | Settings | File Templates.
 */
public class PrefActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        addPreferencesFromResource(R.xml.preferences);
    }
}
