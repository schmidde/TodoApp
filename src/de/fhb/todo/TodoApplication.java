package de.fhb.todo;

import android.app.Application;
import android.content.Context;

/**
 * Created by IntelliJ IDEA.
 * User: phr
 * Date: 22.01.12
 * Time: 12:25
 * To change this template use File | Settings | File Templates.
 */
public class TodoApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        TodoApplication.context = getApplicationContext();
    }

    public static Context getContext(){
        return TodoApplication.context;
    }

}
