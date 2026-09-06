package com.moodrecorder.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver { public void onReceive(Context c,Intent i){ android.content.SharedPreferences sp=c.getSharedPreferences("mood",Context.MODE_PRIVATE); if(sp.getBoolean("reminder_on",false)) ReminderReceiver.schedule(c,sp.getInt("hour",20),sp.getInt("minute",30)); } }
