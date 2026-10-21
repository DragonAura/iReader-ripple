package local.ireader.keyripple;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public final class RippleService extends AccessibilityService {
    static volatile boolean connected;
    private static final String TAG="OceanRippleService";

    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        AccessibilityServiceInfo info=getServiceInfo();
        info.flags|=AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.eventTypes=0;
        setServiceInfo(info);
        connected=true;
        try{RippleEngine.init();Log.i(TAG,"CONNECTED focus="+RippleEngine.focusPackage());}
        catch(Throwable error){Log.e(TAG,"INIT_FAILED",error);}
        RippleEngine.prefs(this).edit().putString("status","监听已连接").apply();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        // No window contents or accessibility event data are needed.
    }
    @Override protected boolean onKeyEvent(KeyEvent event){
        if(event.getAction()!=KeyEvent.ACTION_DOWN || event.getRepeatCount()!=0)return false;
        int code=event.getKeyCode();
        boolean next=code==KeyEvent.KEYCODE_VOLUME_DOWN || code==KeyEvent.KEYCODE_PAGE_DOWN;
        boolean previous=code==KeyEvent.KEYCODE_VOLUME_UP || code==KeyEvent.KEYCODE_PAGE_UP;
        if(!next && !previous)return false;
        InputDevice device=event.getDevice();
        if(device==null || device.isVirtual() || (event.getFlags()&KeyEvent.FLAG_SOFT_KEYBOARD)!=0){
            Log.i(TAG,"IGNORE_VIRTUAL key="+code);return false;
        }
        SharedPreferences p=RippleEngine.prefs(this);
        if(!p.getBoolean("enabled",true))return false;
        try{
            String foreground=RippleEngine.focusPackage();
            if(!RippleEngine.targets(this).contains(foreground))return false;
            long start=SystemClock.uptimeMillis();
            int effect=RippleEngine.prepare(this,next);
            int turns=p.getInt("turns",0)+1;
            p.edit().putInt("turns",turns).putString("status","按键动效调用正常")
                .putString("last","effect="+effect+" · "+(SystemClock.uptimeMillis()-start)+" ms").apply();
            Log.i(TAG,"TURN "+turns+" package="+foreground+" key="+code+" scan="+event.getScanCode()+" effect="+effect
                +" elapsedMs="+(SystemClock.uptimeMillis()-start)+" consumed=false");
        }catch(Throwable error){
            p.edit().putString("status","调用失败："+error.getClass().getSimpleName()).apply();
            Log.e(TAG,"PREPARE_FAILED",error);
        }
        // Let the foreground application receive and handle its own key event.
        return false;
    }
    @Override public void onInterrupt(){ }
    @Override public boolean onUnbind(Intent intent){connected=false;Log.i(TAG,"UNBOUND");return super.onUnbind(intent);}
    @Override public void onDestroy(){connected=false;super.onDestroy();}
}
