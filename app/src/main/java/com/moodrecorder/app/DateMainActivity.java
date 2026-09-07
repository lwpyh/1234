package com.moodrecorder.app;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

public class DateMainActivity extends MainActivity {
    String selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    Button dateButton;
    Button saveButton;

    @Override
    void buildTodayCard(){
        contentRoot.addView(sectionTitle("记录"));
        LinearLayout quick=card();

        LinearLayout h=new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout words=new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        TextView q=text("这一天感觉怎么样？",19,true);
        words.addView(q);
        TextView helper=text("可以补记过去漏掉的日期",12,false);
        helper.setTextColor(MUTED);
        helper.setPadding(0,dp(3),0,0);
        words.addView(helper);
        h.addView(words,new LinearLayout.LayoutParams(0,-2,1));

        dateButton=new Button(this);
        dateButton.setAllCaps(false);
        dateButton.setTextSize(13);
        dateButton.setTextColor(INK);
        dateButton.setGravity(Gravity.CENTER);
        dateButton.setPadding(dp(12),0,dp(12),0);
        dateButton.setBackground(rounded(YELLOW_SOFT,18));
        dateButton.setElevation(0);
        dateButton.setOnClickListener(v->pickRecordDate());
        h.addView(dateButton,new LinearLayout.LayoutParams(dp(132),dp(46)));
        quick.addView(h);

        TextView hint=text("不用想太久，第一感觉就好。",13,false);
        hint.setTextColor(MUTED);
        hint.setPadding(0,dp(12),0,dp(12));
        quick.addView(hint);

        for(int i=0;i<6;i++){
            LinearLayout row=new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0,dp(5),0,0);
            TextView n=text(names[i],14,false);
            vals[i]=pill("5",softFor(i));
            vals[i].setMinWidth(dp(42));
            row.addView(n,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(vals[i]);
            quick.addView(row);

            bars[i]=new SeekBar(this);
            bars[i].setMax(10);
            bars[i].setProgress(5);
            bars[i].setProgressTintList(android.content.res.ColorStateList.valueOf(accents[i]));
            bars[i].setThumbTintList(android.content.res.ColorStateList.valueOf(accents[i]));
            final int j=i;
            bars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean f){vals[j].setText(String.valueOf(p));}
                public void onStartTrackingTouch(SeekBar s){}
                public void onStopTrackingTouch(SeekBar s){}
            });
            quick.addView(bars[i],new LinearLayout.LayoutParams(-1,dp(38)));
        }

        View sep=new View(this);
        sep.setBackgroundColor(Color.rgb(239,236,228));
        LinearLayout.LayoutParams spm=new LinearLayout.LayoutParams(-1,dp(1));
        spm.setMargins(0,dp(8),0,dp(13));
        quick.addView(sep,spm);

        quick.addView(text("留一点给这一天的话",16,true));
        event=styledEdit("这一天发生了什么？",2); quick.addView(event);
        anchor=styledEdit("不考虑当时的情绪，我仍然知道的一件事",2); quick.addView(anchor);
        note=styledEdit("最需要提醒自己的话",1); quick.addView(note);

        saveButton=new Button(this);
        saveButton.setTextSize(16);
        saveButton.setTextColor(Color.WHITE);
        saveButton.setAllCaps(false);
        saveButton.setTypeface(null,android.graphics.Typeface.BOLD);
        saveButton.setBackground(rounded(PINK,24));
        saveButton.setElevation(0);
        LinearLayout.LayoutParams sb=new LinearLayout.LayoutParams(-1,dp(52));
        sb.setMargins(0,dp(8),0,0);
        quick.addView(saveButton,sb);
        saveButton.setOnClickListener(v->{save(); hideKeyboard();});

        contentRoot.addView(quick);
        updateDateUI();
        loadRecordForDate(selectedDate);
    }

    void pickRecordDate(){
        Calendar c=Calendar.getInstance();
        try{
            Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(selectedDate);
            if(d!=null)c.setTime(d);
        }catch(Exception ignored){}

        DatePickerDialog dialog=new DatePickerDialog(this,(view,year,month,day)->{
            Calendar picked=Calendar.getInstance();
            picked.set(year,month,day,12,0,0);
            selectedDate=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(picked.getTime());
            updateDateUI();
            loadRecordForDate(selectedDate);
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    void updateDateUI(){
        try{
            Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(selectedDate);
            String label=new SimpleDateFormat("MM/dd  EEE",Locale.CHINA).format(d);
            dateButton.setText("📅  "+label);
            String today=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
            if(selectedDate.equals(today)) saveButton.setText("保存今天的记录");
            else saveButton.setText("保存 "+new SimpleDateFormat("MM/dd",Locale.getDefault()).format(d)+" 的记录");
        }catch(Exception e){
            dateButton.setText(selectedDate);
            saveButton.setText("保存这一天的记录");
        }
    }

    void clearForm(){
        for(int i=0;i<6;i++)bars[i].setProgress(5);
        event.setText("");
        anchor.setText("");
        note.setText("");
    }

    void loadRecordForDate(String date){
        clearForm();
        for(JSONObject o:records()){
            if(date.equals(o.optString("date"))){
                for(int i=0;i<6;i++)bars[i].setProgress(o.optInt(keys[i],5));
                event.setText(o.optString("event"));
                anchor.setText(o.optString("anchor"));
                note.setText(o.optString("note"));
                Toast.makeText(this,"已载入这一天的记录，可直接修改",Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @Override
    void save(){
        try{
            String date=selectedDate;
            JSONObject obj=new JSONObject();
            obj.put("date",date);
            for(int i=0;i<6;i++)obj.put(keys[i],bars[i].getProgress());
            obj.put("event",event.getText().toString().trim());
            obj.put("anchor",anchor.getText().toString().trim());
            obj.put("note",note.getText().toString().trim());

            JSONArray old=new JSONArray(sp.getString("records","[]"));
            JSONArray out=new JSONArray();
            boolean replaced=false;
            for(int i=0;i<old.length();i++){
                JSONObject o=old.getJSONObject(i);
                if(date.equals(o.optString("date"))){out.put(obj);replaced=true;}
                else out.put(o);
            }
            if(!replaced)out.put(obj);
            sp.edit().putString("records",out.toString()).apply();

            String shown=date;
            try{
                Date d=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(date);
                shown=new SimpleDateFormat("MM/dd",Locale.getDefault()).format(d);
            }catch(Exception ignored){}
            Toast.makeText(this,"已保存 "+shown+" 的记录",Toast.LENGTH_SHORT).show();
            render();
        }catch(Exception e){
            Toast.makeText(this,"保存失败",Toast.LENGTH_SHORT).show();
        }
    }
}
