package org.coax.xposed.cpufreqstatusbar.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class LegacyPositionCallbackImpl implements PositionCallback {

	private LinearLayout mStatusIcons;
	private LinearLayout mIcons;
	private View cpufreq;
	private LinearLayout mNotificationIconArea;

	@Override
	public void setup(MethodHookParam param, View v) {
		 cpufreq = v;
		 mStatusIcons = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mStatusIcons");
		 mIcons = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mIcons");
		 mNotificationIconArea = (LinearLayout)mIcons.getChildAt(0);
	}

	@Override
	public void setAbsoluteLeft() {
		removeFromParent();
		mNotificationIconArea.addView(cpufreq, 0);
	}

	@Override
	public void setLeft() {
		removeFromParent();
		mIcons.addView(cpufreq, mIcons.indexOfChild(mStatusIcons));
	}

	@Override
	public void setRight() {
		removeFromParent();
		mIcons.addView(cpufreq);
	}
	
	private void removeFromParent() {
		if(cpufreq.getParent()!=null)
			((ViewGroup)cpufreq.getParent()).removeView(cpufreq);
	}

	@Override
	public LinearLayout getClockParent() {
		return mIcons;
	}
}
