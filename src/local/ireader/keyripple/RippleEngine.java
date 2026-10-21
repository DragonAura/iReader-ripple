package local.ireader.keyripple;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.WindowManager;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

final class RippleEngine {
    static final String TARGETS="target_packages";
    static final int[] SPEED_BITS={128,64,0};
    static final String[] SPEED_LABELS={"慢","标准","快"};
    private static final int[] NEXT={1,4,2,3}, PREV={2,3,1,4};
    private static final int[] WATER_NEXT={4,2,3,1}, WATER_PREV={3,1,4,2};
    private static Method post,focus;
    static synchronized SharedPreferences prefs(Context c){
        SharedPreferences p=c.getSharedPreferences("settings",Context.MODE_PRIVATE);
        if(!p.contains(TARGETS)){
            Set<String> targets=new HashSet<String>();
            // Preserve the working v1.0 configuration on upgrade. Fresh installs start empty.
            if(p.contains("turns"))targets.add("com.qidian.eink");
            p.edit().putStringSet(TARGETS,targets).commit();
        }
        return p;
    }
    static Set<String> targets(Context c){
        return new HashSet<String>(prefs(c).getStringSet(TARGETS,new HashSet<String>()));
    }
    static synchronized void init() throws Exception {
        if(post==null)post=Class.forName("android.eink.EPDCDevice").getMethod("nativePostCommand",String.class);
        if(focus==null)focus=Class.forName("android.eink.api.EinkAPI").getMethod("getFocusPackage");
    }
    static String focusPackage() throws Exception {init();return String.valueOf(focus.invoke(null));}
    static int prepare(Context c,boolean next) throws Exception {
        init();
        SharedPreferences p=prefs(c);
        int speed=Math.max(0,Math.min(2,p.getInt("speed",0)));
        int rotation=((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if(rotation<0 || rotation>3)throw new IllegalStateException("rotation");
        boolean waterfall=p.getBoolean("waterfall",false);
        int[] directions=waterfall?(next?WATER_NEXT:WATER_PREV):(next?NEXT:PREV);
        int effect=SPEED_BITS[speed]|directions[rotation];
        post.invoke(null,"next-effect-type "+effect);
        post.invoke(null,"epdc-force-next-post-mode 1000063");
        return effect;
    }
}
