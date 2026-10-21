package local.ireader.keyripple;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import java.text.Collator;
import java.util.*;

public final class AppSelectionActivity extends Activity {
    private final ArrayList<App> all=new ArrayList<App>();
    private final ArrayList<App> visible=new ArrayList<App>();
    private Set<String> selected;
    private EditText search;
    private CheckBox showHidden;
    private TextView count;
    private ListView list;
    private Button save;
    private boolean loaded;
    private static final class App {
        final String pkg,label;
        final boolean launchable;
        App(String p,String l,boolean launch){pkg=p;label=l;launchable=launch;}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        selected=RippleEngine.targets(this);
        if(state!=null){
            ArrayList<String> pending=state.getStringArrayList("selection");
            if(pending!=null)selected=new HashSet<String>(pending);
        }
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(Color.WHITE);column.setPadding(dp(18),dp(16),dp(18),dp(12));
        TextView title=text("选择监听应用",25);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);column.addView(title);
        TextView note=text("可多选。应用在前台时响应实体翻页键；保存后立即生效。",16);
        note.setPadding(0,dp(10),0,dp(8));column.addView(note);
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.BLACK);
        search.setTextSize(17);search.setHint("搜索应用名或包名");column.addView(search);
        showHidden=new CheckBox(this);showHidden.setText("显示无桌面图标的应用");showHidden.setTextColor(Color.BLACK);
        showHidden.setTextSize(16);column.addView(showHidden);
        count=text("正在读取已安装应用…",15);column.addView(count);
        list=new ListView(this);list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        column.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(this);
        Button cancel=new Button(this);cancel.setText("取消");cancel.setTextColor(Color.BLACK);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(52),1));
        save=new Button(this);save.setTextColor(Color.BLACK);save.setEnabled(false);
        actions.addView(save,new LinearLayout.LayoutParams(0,dp(52),2));column.addView(actions);
        setContentView(column);
        cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View v){finish();}});
        save.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            RippleEngine.prefs(AppSelectionActivity.this).edit()
                .putStringSet(RippleEngine.TARGETS,new HashSet<String>(selected)).apply();
            Toast.makeText(AppSelectionActivity.this,"已保存，立即生效",Toast.LENGTH_SHORT).show();finish();
        }});
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int n,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int n){filter();}
            public void afterTextChanged(Editable s){}
        });
        showHidden.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton b,boolean checked){filter();}
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                String pkg=visible.get(position).pkg;
                if(list.isItemChecked(position))selected.add(pkg);else selected.remove(pkg);
                updateCount();
            }
        });
        new Thread(new Runnable(){public void run(){loadApps();}},"installed-apps").start();
    }
    private void loadApps(){
        final ArrayList<App> apps=new ArrayList<App>();
        try{
            PackageManager pm=getPackageManager();
            for(ApplicationInfo info:pm.getInstalledApplications(0)){
                if(getPackageName().equals(info.packageName))continue;
                String label;
                try{label=pm.getApplicationLabel(info).toString();}catch(Exception error){label=info.packageName;}
                Intent launch=pm.getLaunchIntentForPackage(info.packageName);
                apps.add(new App(info.packageName,label,launch!=null));
            }
            final Collator collator=Collator.getInstance(Locale.getDefault());
            Collections.sort(apps,new Comparator<App>(){public int compare(App a,App b){
                int labels=collator.compare(a.label,b.label);return labels!=0?labels:a.pkg.compareTo(b.pkg);
            }});
            runOnUiThread(new Runnable(){public void run(){
                if(isFinishing())return;
                all.addAll(apps);loaded=true;save.setEnabled(true);filter();
            }});
        }catch(final Exception error){runOnUiThread(new Runnable(){public void run(){
            if(!isFinishing())count.setText("无法读取应用列表："+error.getClass().getSimpleName());
        }});}
    }
    private void filter(){
        if(!loaded)return;
        String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        visible.clear();ArrayList<String> rows=new ArrayList<String>();
        for(App app:all){
            // Keep selected apps visible even when they have no launcher icon.
            if(!showHidden.isChecked() && !app.launchable && !selected.contains(app.pkg))continue;
            if(!query.isEmpty() && !app.label.toLowerCase(Locale.ROOT).contains(query)
                && !app.pkg.toLowerCase(Locale.ROOT).contains(query))continue;
            visible.add(app);rows.add(app.label+"\n"+app.pkg);
        }
        list.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,rows));
        list.clearChoices();
        for(int i=0;i<visible.size();i++)list.setItemChecked(i,selected.contains(visible.get(i).pkg));
        updateCount();
    }
    private void updateCount(){
        count.setText("已选 "+selected.size()+" 个 · 当前显示 "+visible.size()+" 个");
        save.setText("保存选择（"+selected.size()+"）");
    }
    @Override protected void onSaveInstanceState(Bundle out){
        out.putStringArrayList("selection",new ArrayList<String>(selected));super.onSaveInstanceState(out);
    }
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.BLACK);return t;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
