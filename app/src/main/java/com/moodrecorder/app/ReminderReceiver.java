package com.moodrecorder.app;
import android.app.*; import android.content.*; import android.os.*; import java.util.*;

public class ReminderReceiver extends BroadcastReceiver {
  static final int ID=20260906;
  public void onReceive(Context c,Intent i){
    NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
    String ch="mood_reminder"; if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(ch,"情绪记录提醒",NotificationManager.IMPORTANCE_DEFAULT));
    Intent open=new Intent(c,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,ch):new Notification.Builder(c);
    b.setSmallIcon(com.moodrecorder.app.R.drawable.ic_mood).setContentTitle("情绪记录仪").setContentText("用 30 秒记录今天。只记录，不评判。").setContentIntent(pi).setAutoCancel(true);
    nm.notify(1001,b.build());
  }
  public static void schedule(Context c,int h,int m){ AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE); PendingIntent pi=alarmPI(c); Calendar cal=Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY,h); cal.set(Calendar.MINUTE,m); cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0); if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1); am.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi); }
  public static void cancel(Context c){((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(alarmPI(c));}
  private static PendingIntent alarmPI(Context c){return PendingIntent.getBroadcast(c,ID,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
}
