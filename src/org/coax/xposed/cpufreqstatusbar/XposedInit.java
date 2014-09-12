package org.coax.xposed.cpufreqstatusbar;

import java.lang.reflect.Method;
import java.util.ArrayList;
import info.mzimmermann.libxposed.LXTools;
import org.coax.xposed.cpufreqstatusbar.widget.CpuFreq;
import org.coax.xposed.cpufreqstatusbar.widget.LegacyPositionCallbackImpl;
import org.coax.xposed.cpufreqstatusbar.widget.PositionCallback;
import org.coax.xposed.cpufreqstatusbar.widget.PositionCallbackImpl;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {
	private static ArrayList<TextView> tvClocks = new ArrayList<TextView>();
	private static PositionCallback mPositionCallback = null;
	private CpuFreq mCpuFreq = null;
	
	public static TextView getClock() {
		if(mPositionCallback==null) 
			return null;

		for(TextView tvClock : tvClocks) {
			if(mPositionCallback.getClockParent().findViewById(tvClock.getId())!=null) {
				return tvClock;
			}
		}

		Utils.log("init could not find clock object");
		return null;
	}
	
	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		if (!lpparam.packageName.equals("com.android.systemui"))
			return;
		
		XposedBridge.hookAllConstructors(XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader), new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				if(tvClocks.indexOf(param.thisObject)==-1) {
					tvClocks.add((TextView)param.thisObject);
				}
			}
		});
		
		// we hook this method to follow alpha changes in kitkat
		Method setAlpha = XposedHelpers.findMethodBestMatch(XposedHelpers.findClass("com.android.systemui.statusbar.policy.Clock", lpparam.classLoader), "setAlpha", Float.class);
		XposedBridge.hookMethod(setAlpha, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);

				TextView mClock = XposedInit.getClock();
				if(param.thisObject!=mClock)
					return;

				if(mCpuFreq!=null && mClock!=null) {
					mCpuFreq.setAlpha(mClock.getAlpha());
				}
			}
		});

		XposedHelpers.findAndHookMethod(
			"com.android.systemui.statusbar.phone.PhoneStatusBar",
			lpparam.classLoader, "makeStatusBarView", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						// create cpufreq view
						Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
						LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuFreq.PREF_KEY, 0));
						mCpuFreq = new CpuFreq(mContext);

						// identify legacy mode
						boolean legacy = false;
						try {
							XposedHelpers.getObjectField(param.thisObject, "mSystemIconArea");
						}
						catch(NoSuchFieldError e) {
							legacy = true;
						}
						
						// create callback
						if(legacy)
							mCpuFreq.mPositionCallback = new LegacyPositionCallbackImpl();
						else
							mCpuFreq.mPositionCallback = new PositionCallbackImpl();
						
						// initial setup
						mPositionCallback = mCpuFreq.mPositionCallback;
						mCpuFreq.mPositionCallback.setup(param, mCpuFreq);
						
						// set position
						LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuFreq.PREF_KEY, 0));
						int position = Integer.parseInt(mContext.getSharedPreferences(CpuFreq.PREF_KEY, 0).getString("position", "0"));
						if(position==0) {
							mCpuFreq.mPositionCallback.setLeft();
						}
						else if(position==1) {
							mCpuFreq.mPositionCallback.setRight();
						}
						else if(position==2) {
							mCpuFreq.mPositionCallback.setAbsoluteLeft();
						}
					}
					catch(Exception e) {
						Utils.log(Log.getStackTraceString(e));
					}
				}
			}
		);
	}
}
