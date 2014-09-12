package org.coax.xposed.cpufreqstatusbar.widget;

import info.mzimmermann.libxposed.LXTarget;
import org.coax.xposed.cpufreqstatusbar.Utils;
import org.coax.xposed.cpufreqstatusbar.XposedInit;
import org.coax.xposed.cpufreqstatusbar.activities.SettingsActivity;
import java.io.File;
import java.io.FileInputStream;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

@SuppressLint("HandlerLeak")
public class CpuFreq extends TextView implements OnSharedPreferenceChangeListener {
	final public static String INTENT_ACTION_UPDATE = "cpufreq_update_timer";
	final public static String PREF_KEY = "org.coax.xposed.cpufreqstatusbar_preferences";
	final private Context mContext;
	private PendingIntent pi = null;
	private File freqFile = null;

	public PositionCallback mPositionCallback = null;

	private boolean isScreenOn = true;
	private boolean isStatusBarVis = true;

	private String sWidestFreq;
	private int statusbarHeight = 0;
	private final static int paddingWidth = 3;
	
	private boolean loggingEnabled = false;

	public CpuFreq(Context context) {
		this(context, null);
	}

	public CpuFreq(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CpuFreq(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mContext = context;
		
		// init
		loggingEnabled = mContext.getSharedPreferences(PREF_KEY, 0).getBoolean("enable_logging", false);

		freqFile = Utils.getFreqFile(mContext, mContext.getSharedPreferences(PREF_KEY, 0).getString("frequency_file", null));
		if(loggingEnabled)
			Utils.log("init freqFile = " + (freqFile == null ? "null" : freqFile.getPath()));

		// set height
		setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if(resourceId > 0)
			statusbarHeight = getResources().getDimensionPixelSize(resourceId);
		if(statusbarHeight > 0) {
			setHeight(statusbarHeight);
		}
		if(loggingEnabled)
			Utils.log("init statusbarHeight = " + statusbarHeight);

		// set fixed width
		sWidestFreq = Utils.findWidestFreqString();
		if(sWidestFreq != null) {
			String sFormattedWidestFreq;
			String measurement = mContext.getSharedPreferences(PREF_KEY, 0).getString("measurement", "M");
			if(measurement.equals("G")){
				float fWidestFreq = Float.parseFloat(sWidestFreq)/1000F;
				sFormattedWidestFreq = String.format("%.2f", fWidestFreq);
			} else {
				sFormattedWidestFreq = sWidestFreq;
			}
			if(mContext.getSharedPreferences(PREF_KEY, 0).getBoolean("show_unit", false)) {
				sFormattedWidestFreq = sFormattedWidestFreq + measurement;
			}
			setMinimumWidth((int)getPaint().measureText(sFormattedWidestFreq) + paddingWidth * 2);
			if(loggingEnabled) {
				Utils.log("init sFormattedWidestFreq = " + sFormattedWidestFreq);
				Utils.log("init setMinimumWidth = " + getPaint().measureText(sFormattedWidestFreq));
			}
		} else if(loggingEnabled) {
			Utils.log("init could not set width");
		}

		// style
		setTextColor(Color.WHITE);
		setTextSize(TypedValue.COMPLEX_UNIT_SP,
					Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("font_size", "16")));
		setPadding(paddingWidth,
					Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("top_padding", "2")),
					paddingWidth,
					0);
		setGravity(Gravity.TOP | Gravity.RIGHT);  // gravity goes to TOP for manual top_padding
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		IntentFilter filter = new IntentFilter();
		filter.addAction(INTENT_ACTION_UPDATE);
		filter.addAction(SettingsActivity.ACTION_SETTINGS_UPDATE);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		mContext.registerReceiver(mBroadcastReceiver, filter);

		mContext.getSharedPreferences(PREF_KEY, 0).registerOnSharedPreferenceChangeListener(this);

		setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				if((visibility & SYSTEM_UI_FLAG_FULLSCREEN) > 0) {
					isStatusBarVis = false;
				} else {
					isStatusBarVis = true;
				}
			}
		});
		
		// start update interval
		int updateInterval = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("update_interval", "1000"));
		setAlarm(updateInterval);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		mContext.unregisterReceiver(mBroadcastReceiver);
		mContext.getSharedPreferences(PREF_KEY, 0).unregisterOnSharedPreferenceChangeListener(this);
		cancelAlarm();
		super.onDetachedFromWindow();
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				isScreenOn = true;
			}
			else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				isScreenOn = false;
			}
			else if(intent.getAction().equals(INTENT_ACTION_UPDATE) && isScreenOn && isStatusBarVis) {
				updateFrequency();
			}
			else if(intent.getAction().equals(SettingsActivity.ACTION_SETTINGS_UPDATE)) {
				LXTarget.receivePreferences(context.getSharedPreferences(PREF_KEY, 0), intent);
			}
		}
	};

	public void setAlarm(int interval) {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(INTENT_ACTION_UPDATE);
		pi = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), interval, pi);
	}
	
	public void cancelAlarm() {
		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		if(pi!=null)  {
			am.cancel(pi);
			pi.cancel();
		}
	}

	private void updateFrequency() {
		try {
			FileInputStream fis = new FileInputStream(freqFile);
			StringBuffer sbFreq = new StringBuffer("");

			// read frequency
			byte[] buffer = new byte[1024];
			while(fis.read(buffer) != -1) {
				sbFreq.append(new String(buffer));
			}
			fis.close();

			// get freq value and convert to MHz
			String sFreq = sbFreq.toString().replaceAll("[^0-9]+", "");
			Long lFreq = Long.valueOf(sFreq)/1000;
			int iFreq = lFreq.intValue();
			
			// measure system
			String measurement = mContext.getSharedPreferences(PREF_KEY, 0).getString("measurement", "M");
			if(measurement.equals("M")){
				sFreq = String.valueOf(iFreq);
			}
			else if(measurement.equals("G")) {
				float fFreq = iFreq/1000F;
				sFreq = String.format("%.2f", fFreq);
			}

			// unit label
			boolean show_unit = mContext.getSharedPreferences(PREF_KEY, 0).getBoolean("show_unit", false);
			if(show_unit) {
				sFreq = sFreq + measurement;
			}
			
			// set text
			setText(sFreq);
			
			// set text color
			int color_mode = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("color_mode", "0"));
			TextView mClock = XposedInit.getClock();
			
			// set alpha, this prevents wrong initial colors
			if(mClock != null) {
				setAlpha(mClock.getAlpha());
			}

			switch(color_mode) {
			// auto
			case 0:
				if(mClock != null) {
					setTextColor(mClock.getCurrentTextColor());
				}
				break;
			//manual
			case 1:
				int configured_color = mContext.getSharedPreferences(PREF_KEY, 0).getInt("configured_color", Color.WHITE);
				setTextColor(configured_color);
				break;
			//freq 
			case 2:
				int color_low = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_low", Color.GREEN);
				int color_middle = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_middle", Color.YELLOW);
				int color_high = mContext.getSharedPreferences(PREF_KEY, 0).getInt("color_high", Color.RED);
				int freq_middle = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("freq_middle", "500"));
				int freq_high = Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("freq_high", "1000"));
				
				if(iFreq >= freq_high)
					setTextColor(color_high);
				else if(iFreq >= freq_middle)
					setTextColor(color_middle);
				else
					setTextColor(color_low);
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			Utils.log(Log.getStackTraceString(e));
			setText("-");
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if(key.equals("update_interval")) {
			int updateInterval = Integer.parseInt(pref.getString("update_interval", "1000"));
			cancelAlarm();
			setAlarm(updateInterval);
		}
		
		else if(key.equals("frequency_file")) {
			String frequency_file = pref.getString("frequency_file", null);
			freqFile = Utils.getFreqFile(mContext, frequency_file);
		}

		else if(key.equals("position")) {
			int position = Integer.parseInt(pref.getString("position", "0"));
			if(getParent()!=null)
				((ViewGroup)getParent()).removeView(this);
			if(position == 0) {
				mPositionCallback.setLeft();
			}
			else if(position == 1) {
				mPositionCallback.setRight();
			}
			else if(position == 2) {
				mPositionCallback.setAbsoluteLeft();
			}
		}

		else if(key.equals("font_size") || key.equals("top_padding") ||
				key.equals("measurement") || key.equals("show_unit")) {
			// set font size
			setTextSize(TypedValue.COMPLEX_UNIT_SP,
					Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("font_size", "16")));
			// set padding
			setPadding(paddingWidth,
					Integer.parseInt(mContext.getSharedPreferences(PREF_KEY, 0).getString("top_padding", "2")),
					paddingWidth,
					0);
			// set fixed width
			if(sWidestFreq != null) {
				String sFormattedWidestFreq;
				String measurement = pref.getString("measurement", "M");
				if(measurement.equals("G")){
					float fWidestFreq = Float.parseFloat(sWidestFreq)/1000F;
					sFormattedWidestFreq = String.format("%.2f", fWidestFreq);
				} else {
					sFormattedWidestFreq = sWidestFreq;
				}
				if(pref.getBoolean("show_unit", false)) {
					sFormattedWidestFreq = sFormattedWidestFreq + measurement;
				}
				setMinimumWidth((int)getPaint().measureText(sFormattedWidestFreq) + paddingWidth * 2);
				if(loggingEnabled) {
					Utils.log("pref sFormattedWidestFreq = " + sFormattedWidestFreq);
					Utils.log("pref setMinimumWidth = " + getPaint().measureText(sFormattedWidestFreq));
				}
			}
		}

		else if(key.equals("enable_logging")) {
			loggingEnabled = mContext.getSharedPreferences(PREF_KEY, 0).getBoolean("enable_logging", false);
		}

		updateFrequency();
	}
}
