package com.moodrecorder.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    final String[] keys={"mood","anxiety","manager","interest","leave","academia"};
    final String[] names={"整体情绪","工作焦虑","直属领导带来的痛苦","对工作内容的兴趣","想离开腾讯的程度","想去高校的程度"};
    SeekBar[] bars=new SeekBar[6]; TextView[] vals=new TextView[6];
    EditText event,anchor,note; TextView summary,history; Switch reminderSwitch; Button timeButton;
    SharedPreferences sp; int hour=20,minute=30;

    @Override public void onCreate(Bundle b){ super.onCreate(b); sp=getSharedPreferences("mood",MODE_PRIVATE); buildUI(); loadReminder(); requestNotif(); render(); }
    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(35,35,35)); if(bold)t.setTypeface(null,1); return t; }
    LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(16),dp(16),dp(16),dp(16)); c.setBackgroundColor(Color.rgb(255,253,248)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(12)); c.setLayoutParams(p); return c; }
    TextView title(String s){TextView t=text(s,18,true);t.setPadding(0,0,0,dp(8));return t;}

    void buildUI(){
        ScrollView sv=new ScrollView(this); sv.setBackgroundColor(Color.rgb(244,241,234)); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(20),dp(16),dp(40)); sv.addView(root); setContentView(sv);
        root.addView(text("情绪记录仪",28,true)); TextView sub=text("30 秒记录今天。不是给情绪打分，而是给未来的自己留下数据。",14,false); sub.setTextColor(Color.DKGRAY); sub.setPadding(0,dp(4),0,dp(16)); root.addView(sub);
        LinearLayout quick=card(); quick.addView(title("今天感觉怎么样？"));
        for(int i=0;i<6;i++){ LinearLayout row=new LinearLayout(this); TextView n=text(names[i],14,false); vals[i]=text("5 / 10",14,false); row.addView(n,new LinearLayout.LayoutParams(0,-2,1)); row.addView(vals[i]); quick.addView(row); bars[i]=new SeekBar(this); bars[i].setMax(10); bars[i].setProgress(5); final int j=i; bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){vals[j].setText(p+" / 10");} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}}); quick.addView(bars[i]); }
        root.addView(quick);
        LinearLayout notes=card(); notes.addView(title("一句话记录")); event=new EditText(this); event.setHint("今天发生了什么？"); event.setMinLines(2); notes.addView(event); anchor=new EditText(this); anchor.setHint("不考虑今天的情绪，我仍然知道的一件事"); anchor.setMinLines(2); notes.addView(anchor); note=new EditText(this); note.setHint("今天最需要提醒自己的话"); notes.addView(note); Button save=new Button(this); save.setText("保存今天"); save.setOnClickListener(v->save()); notes.addView(save); root.addView(notes);
        LinearLayout sum=card(); sum.addView(title("本周校准")); summary=text("记录几天后，这里会出现趋势摘要。",14,false); summary.setLineSpacing(0,1.2f); sum.addView(summary); root.addView(sum);
        LinearLayout remind=card(); remind.addView(title("每日提醒")); reminderSwitch=new Switch(this); reminderSwitch.setText("开启每天记录提醒"); remind.addView(reminderSwitch); timeButton=new Button(this); timeButton.setOnClickListener(v->pickTime()); remind.addView(timeButton); reminderSwitch.setOnCheckedChangeListener((btt,on)->{sp.edit().putBoolean("reminder_on",on).apply();if(on)ReminderReceiver.schedule(this,hour,minute);else ReminderReceiver.cancel(this);}); root.addView(remind);
        LinearLayout hist=card(); hist.addView(title("最近记录")); history=text("",14,false); history.setLineSpacing(0,1.15f); hist.addView(history); root.addView(hist);
    }

    void save(){ try{ String date=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date()); JSONObject obj=new JSONObject(); obj.put("date",date); for(int i=0;i<6;i++)obj.put(keys[i],bars[i].getProgress()); obj.put("event",event.getText().toString().trim()); obj.put("anchor",anchor.getText().toString().trim()); obj.put("note",note.getText().toString().trim()); JSONArray old=new JSONArray(sp.getString("records","[]")), out=new JSONArray(); boolean replaced=false; for(int i=0;i<old.length();i++){JSONObject o=old.getJSONObject(i);if(date.equals(o.optString("date"))){out.put(obj);replaced=true;}else out.put(o);} if(!replaced)out.put(obj); sp.edit().putString("records",out.toString()).apply(); Toast.makeText(this,"已保存。今天不用分析更多。",Toast.LENGTH_SHORT).show(); render(); }catch(Exception e){Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();} }
    ArrayList<JSONObject> records(){ArrayList<JSONObject> r=new ArrayList<>();try{JSONArray a=new JSONArray(sp.getString("records","[]"));for(int i=0;i<a.length();i++)r.add(a.getJSONObject(i));Collections.sort(r,(x,y)->x.optString("date").compareTo(y.optString("date")));}catch(Exception ignored){}return r;}
    double avg(List<JSONObject> r,String k){if(r.isEmpty())return 0;double s=0;for(JSONObject o:r)s+=o.optInt(k);return s/r.size();}
    void render(){ArrayList<JSONObject> all=records(); StringBuilder h=new StringBuilder(); for(int i=all.size()-1;i>=0&&i>=all.size()-10;i--){JSONObject o=all.get(i);h.append(o.optString("date")).append("\n情绪 ").append(o.optInt("mood")).append("/10 · 焦虑 ").append(o.optInt("anxiety")).append("/10 · 离开意愿 ").append(o.optInt("leave")).append("/10 · 高校意愿 ").append(o.optInt("academia")).append("/10\n");String e=o.optString("event");if(!e.isEmpty())h.append(e).append("\n");h.append("\n");} history.setText(h.length()==0?"还没有记录。":h.toString());
        if(all.size()<3){summary.setText("再记录 "+(3-all.size())+" 天，就能开始看到稳定趋势。\n先记录，不急着解释自己。");return;} List<JSONObject> recent=all.subList(Math.max(0,all.size()-7),all.size()); double mood=avg(recent,"mood"),anx=avg(recent,"anxiety"),mgr=avg(recent,"manager"),leave=avg(recent,"leave"),acad=avg(recent,"academia"); StringBuilder s=new StringBuilder(String.format(Locale.getDefault(),"最近 7 天：情绪 %.1f / 焦虑 %.1f / 领导压力 %.1f。\n离开腾讯意愿 %.1f，去高校意愿 %.1f。\n",mood,anx,mgr,leave,acad)); if(leave>=7&&acad>=7)s.append("“想离开”和“想去高校”都持续偏高：可以继续用真实岗位和 offer 验证。\n");else if(leave>=7&&acad<6)s.append("“想离开”明显高于“想去高校”：目前更确定的是不喜欢现状，不必仓促押注高校。\n");if(mgr>=7&&leave-mgr<2)s.append("领导压力与离开意愿都很高：值得验证换领导或换组是否会改变判断。\n"); summary.setText(s.toString()); }
    void loadReminder(){hour=sp.getInt("hour",20);minute=sp.getInt("minute",30);reminderSwitch.setChecked(sp.getBoolean("reminder_on",false));refreshTime();}
    void refreshTime(){timeButton.setText(String.format(Locale.getDefault(),"提醒时间  %02d:%02d",hour,minute));}
    void pickTime(){new TimePickerDialog(this,(v,h,m)->{hour=h;minute=m;sp.edit().putInt("hour",h).putInt("minute",m).apply();refreshTime();if(reminderSwitch.isChecked())ReminderReceiver.schedule(this,hour,minute);},hour,minute,true).show();}
    void requestNotif(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}
}
