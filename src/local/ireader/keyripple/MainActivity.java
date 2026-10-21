package local.ireader.keyripple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public final class MainActivity extends Activity {
    private TextView status;
    private Button pause,mode,speed,targets;
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){update();handler.postDelayed(this,1200);}};

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.WHITE);
        LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22),dp(24),dp(22),dp(26));scroll.addView(column);
        TextView title=text("实体按键水波纹",27);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);column.addView(title);
        TextView intro=text("在你选择的应用中响应实体翻页键。触屏和自动翻页不触发助手动效。",18);
        intro.setPadding(0,dp(16),0,dp(14));column.addView(intro);
        status=text("",18);column.addView(status);
        addButton(column,"开启 / 管理无障碍监听",new View.OnClickListener(){public void onClick(View v){openAccessibility();}});
        targets=addButton(column,"",new View.OnClickListener(){public void onClick(View v){
            startActivity(new Intent(MainActivity.this,AppSelectionActivity.class));
        }});
        pause=addButton(column,"",new View.OnClickListener(){public void onClick(View v){
            SharedPreferences p=RippleEngine.prefs(MainActivity.this);
            p.edit().putBoolean("enabled",!p.getBoolean("enabled",true)).apply();update();
        }});
        mode=addButton(column,"",new View.OnClickListener(){public void onClick(View v){
            SharedPreferences p=RippleEngine.prefs(MainActivity.this);
            p.edit().putBoolean("waterfall",!p.getBoolean("waterfall",false)).apply();update();
        }});
        speed=addButton(column,"",new View.OnClickListener(){public void onClick(View v){
            SharedPreferences p=RippleEngine.prefs(MainActivity.this);
            p.edit().putInt("speed",(p.getInt("speed",0)+1)%3).apply();update();
        }});
        addButton(column,"打开已选应用",new View.OnClickListener(){public void onClick(View v){openTarget();}});
        TextView note=text("应用可以多选，保存后立即生效。已选应用的全部界面都会响应实体翻页键；应用本身需要支持按键翻页并更新画面。\n\n开启服务后，无需连接电脑。首次若显示“受限设置”，请到应用信息中允许受限设置，再开启服务。",16);
        note.setPadding(0,dp(20),0,0);column.addView(note);
        setContentView(scroll);update();
    }
    private void openTarget(){
        Set<String> selected=RippleEngine.targets(this);
        if(selected.isEmpty()){
            startActivity(new Intent(this,AppSelectionActivity.class));return;
        }
        final ArrayList<String> packages=new ArrayList<String>(selected);
        Collections.sort(packages);
        if(packages.size()==1){launchTarget(packages.get(0));return;}
        String[] names=new String[packages.size()];
        for(int i=0;i<names.length;i++){
            String pkg=packages.get(i);
            try{names[i]=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
            catch(Exception error){names[i]=pkg;}
        }
        new AlertDialog.Builder(this).setTitle("打开应用").setItems(names,new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface d,int which){launchTarget(packages.get(which));}
        }).setNegativeButton("取消",null).show();
    }
    private void launchTarget(String pkg){
        Intent intent=getPackageManager().getLaunchIntentForPackage(pkg);
        if(intent==null)Toast.makeText(this,"这个应用没有桌面入口，请自行打开",Toast.LENGTH_LONG).show();
        else startActivity(intent);
    }
    private void openAccessibility(){
        try{
            Intent intent=new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");
            intent.putExtra("android.intent.extra.COMPONENT_NAME",new ComponentName(this,RippleService.class));
            startActivity(intent);
        }catch(Exception error){startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
    }
    private void update(){
        if(status==null)return;
        SharedPreferences p=RippleEngine.prefs(this);
        boolean enabled=p.getBoolean("enabled",true);
        status.setText((RippleService.connected?"监听已连接":"监听未连接，请开启无障碍服务")
            +"\n效果："+(enabled?"开启":"暂停")+" · 已响应 "+p.getInt("turns",0)+" 次实体按键");
        pause.setText(enabled?"暂停动效":"启用动效");
        mode.setText("动效："+(p.getBoolean("waterfall",false)?"瀑布":"水波纹")+"（点击切换）");
        int index=Math.max(0,Math.min(2,p.getInt("speed",0)));
        speed.setText("速度："+RippleEngine.SPEED_LABELS[index]+"（点击切换）");
        targets.setText("选择监听应用（已选 "+RippleEngine.targets(this).size()+" 个）");
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.BLACK);t.setLineSpacing(dp(3),1);return t;}
    private Button addButton(LinearLayout column,String label,View.OnClickListener listener){
        Button b=new Button(this);b.setText(label);b.setTextSize(18);b.setTextColor(Color.BLACK);b.setAllCaps(false);
        b.setMinHeight(dp(54));b.setPadding(dp(10),dp(8),dp(10),dp(8));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setStroke(dp(2),Color.BLACK);bg.setCornerRadius(dp(5));b.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(16);b.setLayoutParams(lp);
        b.setOnClickListener(listener);column.addView(b);return b;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
