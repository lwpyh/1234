package com.moodrecorder.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

public class MainActivity extends Activity {
    static final int BG = Color.rgb(248,246,239);
    static final int INK = Color.rgb(36,36,42);
    static final int MUTED = Color.rgb(120,116,110);
    static final int PINK = Color.rgb(241,112,102);
    static final int PINK_SOFT = Color.rgb(253,231,228);
    static final int MINT_SOFT = Color.rgb(229,244,232);
    static final int BLUE_SOFT = Color.rgb(229,237,250);
    static final int LAV_SOFT = Color.rgb(239,232,249);
    static final int YELLOW_SOFT = Color.rgb(249,243,216);
    static final int CARD = Color.rgb(255,254,250);

    final String[] keys={"mood","anxiety","manager","interest","leave","academia"};
    final String[] names={"整体情绪","工作焦虑","直属领导带来的痛苦","对工作内容的兴趣","想离开腾讯的程度","想去高校的程度"};
    final int[] accents={PINK,Color.rgb(99,154,224),Color.rgb(172,119,202),Color.rgb(93,166,130),Color.rgb(231,148,83),Color.rgb(111,144,208)};

    SeekBar[] bars=new SeekBar[6];
    TextView[] vals=new TextView[6];
    EditText event,anchor,note;
    TextView summary, weeklyMood, streakText;
    LinearLayout historyContainer;
    Switch reminderSwitch;
    Button timeButton;
    SharedPreferences sp;
    int hour=20,minute=30;
    GridLinearLayout contentRoot;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("mood",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        if(Build.VERSION.SDK_INT>=23)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUI(); loadReminder(); requestNotif(); render();
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    GradientDrawable rounded(int color,float radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp((int)radius));
        return g;
    }

    TextView text(String s,int size,boolean bold){
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(INK);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    TextView sectionTitle(String s){
        TextView t=text(s,20,true);
        t.setPadding(dp(2),dp(18),0,dp(12));
        return t;
    }

    LinearLayout card(){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18),dp(18),dp(18),dp(18));
        c.setBackground(rounded(CARD,24));
        c.setElevation(dp(2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(14)); c.setLayoutParams(p);
        return c;
    }

    TextView pill(String s,int color){
        TextView t=text(s,12,true);
        t.setTextColor(INK); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10),dp(6),dp(10),dp(6));
        t.setBackground(rounded(color,14));
        return t;
    }

    void buildUI(){
        FrameLayout page=new FrameLayout(this);
        page.setBackgroundColor(BG);

        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        contentRoot=new GridLinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setPadding(dp(18),dp(18),dp(18),dp(120));
        sv.addView(contentRoot,new ScrollView.LayoutParams(-1,-2));
        page.addView(sv,new FrameLayout.LayoutParams(-1,-1));

        buildHeader();
        buildOverview();
        buildTodayCard();
        buildCalibration();
        buildReminder();
        buildHistory();
        buildBottomBar(page,sv);
        setContentView(page);
    }

    void buildHeader(){
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setGravity(Gravity.CENTER_HORIZONTAL);
        top.setPadding(0,dp(12),0,dp(18));

        FrameLayout titleWrap=new FrameLayout(this);
        View brush=new View(this); brush.setBackground(rounded(Color.rgb(250,184,183),2));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(210),dp(20),Gravity.CENTER|Gravity.BOTTOM);
        bp.bottomMargin=dp(2); titleWrap.addView(brush,bp);
        TextView title=text("情绪记录仪",32,true); title.setGravity(Gravity.CENTER); title.setLetterSpacing(0.05f);
        titleWrap.addView(title,new FrameLayout.LayoutParams(-1,dp(58),Gravity.CENTER));
        top.addView(titleWrap,new LinearLayout.LayoutParams(-1,dp(62)));

        TextView sub=text("记录情绪 · 更好地生活",14,false); sub.setTextColor(MUTED); sub.setGravity(Gravity.CENTER);
        top.addView(sub);
        contentRoot.addView(top);
    }

    void buildOverview(){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.setMargins(0,0,0,dp(6)); row.setLayoutParams(rp);

        LinearLayout left=miniCard(PINK_SOFT,"♡","本周平均情绪");
        weeklyMood=text("— / 10",28,true); weeklyMood.setPadding(0,dp(5),0,0); left.addView(weeklyMood);
        LinearLayout right=miniCard(MINT_SOFT,"↗","连续记录");
        streakText=text("0 天",28,true); streakText.setPadding(0,dp(5),0,0); right.addView(streakText);

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(138),1); lp.setMargins(0,0,dp(7),0);
        LinearLayout.LayoutParams rp2=new LinearLayout.LayoutParams(0,dp(138),1); rp2.setMargins(dp(7),0,0,0);
        row.addView(left,lp); row.addView(right,rp2); contentRoot.addView(row);
    }

    LinearLayout miniCard(int bg,String icon,String label){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(18),dp(14),dp(16),dp(12)); c.setBackground(rounded(bg,26));
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic=text(icon,25,true); ic.setGravity(Gravity.CENTER); ic.setBackground(rounded(Color.argb(90,255,255,255),24));
        head.addView(ic,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView lab=text(label,14,false); lab.setPadding(dp(10),0,0,0); head.addView(lab,new LinearLayout.LayoutParams(0,-2,1));
        c.addView(head); return c;
    }

    void buildTodayCard(){
        contentRoot.addView(sectionTitle("今天"));
        LinearLayout quick=card();
        LinearLayout h=new LinearLayout(this); h.setGravity(Gravity.CENTER_VERTICAL);
        TextView q=text("今天感觉怎么样？",19,true); h.addView(q,new LinearLayout.LayoutParams(0,-2,1));
        TextView tag=pill(new SimpleDateFormat("MM/dd EEE",Locale.CHINA).format(new Date()),YELLOW_SOFT); h.addView(tag);
        quick.addView(h);
        TextView hint=text("不用想太久，第一感觉就好。",13,false); hint.setTextColor(MUTED); hint.setPadding(0,dp(4),0,dp(12)); quick.addView(hint);

        for(int i=0;i<6;i++){
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,dp(5),0,0);
            TextView n=text(names[i],14,false); vals[i]=pill("5",softFor(i)); vals[i].setMinWidth(dp(42));
            row.addView(n,new LinearLayout.LayoutParams(0,-2,1)); row.addView(vals[i]); quick.addView(row);
            bars[i]=new SeekBar(this); bars[i].setMax(10); bars[i].setProgress(5);
            bars[i].setProgressTintList(ColorStateList.valueOf(accents[i]));
            bars[i].setThumbTintList(ColorStateList.valueOf(accents[i]));
            final int j=i; bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean f){vals[j].setText(String.valueOf(p));}
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
            });
            quick.addView(bars[i],new LinearLayout.LayoutParams(-1,dp(38)));
        }

        View sep=new View(this); sep.setBackgroundColor(Color.rgb(239,236,228)); LinearLayout.LayoutParams spm=new LinearLayout.LayoutParams(-1,dp(1)); spm.setMargins(0,dp(8),0,dp(13)); quick.addView(sep,spm);
        quick.addView(text("留一点给今天的话",16,true));
        event=styledEdit("今天发生了什么？",2); quick.addView(event);
        anchor=styledEdit("不考虑今天的情绪，我仍然知道的一件事",2); quick.addView(anchor);
        note=styledEdit("今天最需要提醒自己的话",1); quick.addView(note);

        Button save=new Button(this); save.setText("保存今天的记录"); save.setTextSize(16); save.setTextColor(Color.WHITE); save.setAllCaps(false);
        save.setTypeface(null,android.graphics.Typeface.BOLD); save.setBackground(rounded(PINK,24)); save.setElevation(0);
        LinearLayout.LayoutParams sb=new LinearLayout.LayoutParams(-1,dp(52)); sb.setMargins(0,dp(8),0,0); quick.addView(save,sb);
        save.setOnClickListener(v->{save(); hideKeyboard();});
        contentRoot.addView(quick);
    }

    int softFor(int i){ int[] s={PINK_SOFT,BLUE_SOFT,LAV_SOFT,MINT_SOFT,Color.rgb(251,234,220),Color.rgb(232,238,251)}; return s[i]; }

    EditText styledEdit(String hint,int minLines){
        EditText e=new EditText(this); e.setHint(hint); e.setTextSize(14); e.setTextColor(INK); e.setHintTextColor(Color.rgb(155,150,142));
        e.setMinLines(minLines); e.setGravity(Gravity.TOP|Gravity.START); e.setPadding(dp(14),dp(12),dp(14),dp(12));
        e.setBackground(rounded(Color.rgb(250,247,238),16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(9),0,0); e.setLayoutParams(p); return e;
    }

    void buildCalibration(){
        contentRoot.addView(sectionTitle("本周校准"));
        LinearLayout sum=card();
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=pill("趋势",MINT_SOFT); head.addView(icon); TextView t=text(" 不是评价你，只是在找规律",15,true); head.addView(t);
        sum.addView(head);
        summary=text("记录几天后，这里会出现趋势摘要。",14,false); summary.setTextColor(Color.rgb(74,72,68)); summary.setLineSpacing(dp(4),1.15f); summary.setPadding(0,dp(12),0,0); sum.addView(summary);
        contentRoot.addView(sum);
    }

    void buildReminder(){
        contentRoot.addView(sectionTitle("每日提醒"));
        LinearLayout remind=card();
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        words.addView(text("每天留 30 秒给自己",16,true)); TextView small=text("到点轻轻提醒，不打扰。",13,false); small.setTextColor(MUTED); words.addView(small);
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        reminderSwitch=new Switch(this); row.addView(reminderSwitch); remind.addView(row);
        timeButton=new Button(this); timeButton.setAllCaps(false); timeButton.setTextColor(INK); timeButton.setTextSize(14); timeButton.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        timeButton.setBackground(rounded(BLUE_SOFT,16)); timeButton.setOnClickListener(v->pickTime());
        LinearLayout.LayoutParams tb=new LinearLayout.LayoutParams(-1,dp(48)); tb.setMargins(0,dp(12),0,0); remind.addView(timeButton,tb);
        reminderSwitch.setOnCheckedChangeListener((btt,on)->{sp.edit().putBoolean("reminder_on",on).apply();if(on)ReminderReceiver.schedule(this,hour,minute);else ReminderReceiver.cancel(this);});
        contentRoot.addView(remind);
    }

    void buildHistory(){
        contentRoot.addView(sectionTitle("最近记录"));
        historyContainer=new LinearLayout(this); historyContainer.setOrientation(LinearLayout.VERTICAL); contentRoot.addView(historyContainer);
    }

    void buildBottomBar(FrameLayout page,ScrollView sv){
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER); bar.setPadding(dp(12),dp(8),dp(12),dp(9)); bar.setBackground(rounded(Color.rgb(255,254,251),28)); bar.setElevation(dp(10));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(82),Gravity.BOTTOM); bp.setMargins(dp(10),0,dp(10),dp(10)); page.addView(bar,bp);

        bar.addView(navItem("⌂","首页",true,()->sv.smoothScrollTo(0,0)),new LinearLayout.LayoutParams(0,-1,1));
        bar.addView(navItem("▥","统计",false,()->sv.smoothScrollTo(0,dp(850))),new LinearLayout.LayoutParams(0,-1,1));
        TextView plus=text("＋",36,false); plus.setTextColor(Color.WHITE); plus.setGravity(Gravity.CENTER); plus.setBackground(rounded(PINK,36)); plus.setElevation(dp(5));
        plus.setOnClickListener(v->{sv.smoothScrollTo(0,dp(260)); event.requestFocus();});
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(70),dp(70)); pp.setMargins(dp(4),dp(-14),dp(4),0); bar.addView(plus,pp);
        bar.addView(navItem("☰","总结",false,()->sv.smoothScrollTo(0,dp(1150))),new LinearLayout.LayoutParams(0,-1,1));
        bar.addView(navItem("⚙","提醒",false,()->sv.smoothScrollTo(0,dp(1500))),new LinearLayout.LayoutParams(0,-1,1));
    }

    View navItem(String icon,String label,boolean active,Runnable action){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(2),dp(2),dp(2),dp(2));
        TextView i=text(icon,22,true); i.setGravity(Gravity.CENTER); if(active)i.setTextColor(PINK); box.addView(i);
        TextView l=text(label,11,active); l.setGravity(Gravity.CENTER); l.setTextColor(active?INK:MUTED); box.addView(l);
        box.setOnClickListener(v->action.run()); return box;
    }

    void save(){
        try{
            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
            JSONObject obj=new JSONObject(); obj.put("date",date);
            for(int i=0;i<6;i++)obj.put(keys[i],bars[i].getProgress());
            obj.put("event",event.getText().toString().trim()); obj.put("anchor",anchor.getText().toString().trim()); obj.put("note",note.getText().toString().trim());
            JSONArray old=new JSONArray(sp.getString("records","[]")), out=new JSONArray(); boolean replaced=false;
            for(int i=0;i<old.length();i++){JSONObject o=old.getJSONObject(i);if(date.equals(o.optString("date"))){out.put(obj);replaced=true;}else out.put(o);} if(!replaced)out.put(obj);
            sp.edit().putString("records",out.toString()).apply();
            Toast.makeText(this,"已保存。今天不用分析更多。",Toast.LENGTH_SHORT).show(); render();
        }catch(Exception e){Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();}
    }

    ArrayList<JSONObject> records(){
        ArrayList<JSONObject> r=new ArrayList<>();
        try{JSONArray a=new JSONArray(sp.getString("records","[]"));for(int i=0;i<a.length();i++)r.add(a.getJSONObject(i));Collections.sort(r,(x,y)->x.optString("date").compareTo(y.optString("date")));}catch(Exception ignored){}
        return r;
    }

    double avg(List<JSONObject> r,String k){if(r.isEmpty())return 0;double s=0;for(JSONObject o:r)s+=o.optInt(k);return s/r.size();}

    int streak(ArrayList<JSONObject> all){
        if(all.isEmpty())return 0;
        HashSet<String> days=new HashSet<>(); for(JSONObject o:all)days.add(o.optString("date"));
        Calendar c=Calendar.getInstance(); SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
        if(!days.contains(f.format(c.getTime()))){c.add(Calendar.DAY_OF_YEAR,-1); if(!days.contains(f.format(c.getTime())))return 0;}
        int n=0; while(days.contains(f.format(c.getTime()))){n++; c.add(Calendar.DAY_OF_YEAR,-1);} return n;
    }

    void render(){
        ArrayList<JSONObject> all=records();
        List<JSONObject> recent=all.subList(Math.max(0,all.size()-7),all.size());
        weeklyMood.setText(recent.isEmpty()?"— / 10":String.format(Locale.getDefault(),"%.1f / 10",avg(recent,"mood")));
        streakText.setText(streak(all)+" 天");

        if(all.size()<3){summary.setText("再记录 "+(3-all.size())+" 天，就能开始看到稳定趋势。\n先记录，不急着解释自己。");}
        else{
            double mood=avg(recent,"mood"),anx=avg(recent,"anxiety"),mgr=avg(recent,"manager"),leave=avg(recent,"leave"),acad=avg(recent,"academia");
            StringBuilder s=new StringBuilder(String.format(Locale.getDefault(),"最近 7 天 · 情绪 %.1f · 焦虑 %.1f · 领导压力 %.1f\n离开腾讯 %.1f · 去高校 %.1f\n\n",mood,anx,mgr,leave,acad));
            if(leave>=7&&acad>=7)s.append("“想离开”和“想去高校”都持续偏高，可以继续用真实岗位和 offer 验证。\n");
            else if(leave>=7&&acad<6)s.append("目前更确定的是不喜欢现状，不必仓促押注高校。\n");
            if(mgr>=7&&leave-mgr<2)s.append("领导压力与离开意愿都很高，值得验证换领导或换组是否会改变判断。\n");
            summary.setText(s.toString());
        }
        renderHistory(all);
    }

    void renderHistory(ArrayList<JSONObject> all){
        historyContainer.removeAllViews();
        if(all.isEmpty()){
            LinearLayout empty=card(); TextView e=text("还没有记录。\n第一条记录会从今天开始。",14,false); e.setTextColor(MUTED); empty.addView(e); historyContainer.addView(empty); return;
        }
        for(int i=all.size()-1;i>=0&&i>=all.size()-8;i--){
            JSONObject o=all.get(i); LinearLayout c=card();
            LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
            TextView date=text(formatDate(o.optString("date")),17,true); head.addView(date,new LinearLayout.LayoutParams(0,-2,1));
            int m=o.optInt("mood"); TextView mood=pill(moodLabel(m)+"  "+m, moodSoft(m)); head.addView(mood); c.addView(head);
            TextView meta=text("焦虑 "+o.optInt("anxiety")+" · 领导压力 "+o.optInt("manager")+" · 离开 "+o.optInt("leave")+" · 高校 "+o.optInt("academia"),12,false); meta.setTextColor(MUTED); meta.setPadding(0,dp(7),0,0); c.addView(meta);
            String e=o.optString("event"); if(!e.isEmpty()){TextView noteView=text(e,14,false); noteView.setPadding(dp(12),dp(9),dp(12),dp(9)); noteView.setBackground(rounded(Color.rgb(250,247,238),13)); LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2); np.setMargins(0,dp(10),0,0); c.addView(noteView,np);}
            historyContainer.addView(c);
        }
    }

    String formatDate(String d){
        try{Date x=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(d); return new SimpleDateFormat("MM/dd  EEEE",Locale.CHINA).format(x);}catch(Exception e){return d;}
    }
    String moodLabel(int m){if(m>=8)return "很好"; if(m>=6)return "不错"; if(m>=4)return "一般"; if(m>=2)return "低落"; return "很难受";}
    int moodSoft(int m){if(m>=7)return MINT_SOFT; if(m>=4)return YELLOW_SOFT; return BLUE_SOFT;}

    void loadReminder(){hour=sp.getInt("hour",20);minute=sp.getInt("minute",30);reminderSwitch.setChecked(sp.getBoolean("reminder_on",false));refreshTime();}
    void refreshTime(){timeButton.setText(String.format(Locale.getDefault(),"  提醒时间     %02d:%02d",hour,minute));}
    void pickTime(){new TimePickerDialog(this,(v,h,m)->{hour=h;minute=m;sp.edit().putInt("hour",h).putInt("minute",m).apply();refreshTime();if(reminderSwitch.isChecked())ReminderReceiver.schedule(this,hour,minute);},hour,minute,true).show();}
    void requestNotif(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}
    void hideKeyboard(){View v=getCurrentFocus(); if(v!=null)((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);}

    class GridLinearLayout extends LinearLayout{
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        GridLinearLayout(Context c){super(c);setWillNotDraw(false);p.setColor(Color.rgb(238,235,227));p.setStrokeWidth(1);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int step=dp(24);for(int x=0;x<getWidth();x+=step)c.drawLine(x,0,x,getHeight(),p);for(int y=0;y<getHeight();y+=step)c.drawLine(0,y,getWidth(),y,p);}
    }
}
