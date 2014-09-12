package org.coax.xposed.cpufreqstatusbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;

public class Utils {
	public static HashMap<String, Class<?>> prefs = new HashMap<String, Class<?>>();

	static {
		prefs.put("update_interval", String.class);
		prefs.put("position", String.class);
		prefs.put("frequency_file", String.class);
		prefs.put("measurement", String.class);
		prefs.put("manual_color", Boolean.class);
		prefs.put("color_mode", String.class);
		prefs.put("configured_color", Integer.class);
		prefs.put("color_low", Integer.class);
		prefs.put("color_middle", Integer.class);
		prefs.put("color_high", Integer.class);
		prefs.put("freq_middle", String.class);
		prefs.put("freq_high", String.class);
	}

	public static void log(String s) {
		XposedBridge.log("CPUFreqStatusbar: " + s);
	}
	
	public static String convertStreamToString(InputStream is) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = reader.readLine()) != null) {
	      sb.append(line).append("\n");
	    }
	    return sb.toString();
	}
	
	public static String[] getFrequencyFiles() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("Auto");
		
		try {
			InputStream in = Runtime.getRuntime()
					.exec("busybox find /sys -type f -name *cur_freq*")
					.getInputStream();
			BufferedReader inBuffered = new BufferedReader(
					new InputStreamReader(in));

			String line = null;
			while ((line = inBuffered.readLine()) != null) {
				result.add(line.trim());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result.toArray(new String[]{});
	}
	
	private static String[] freqFiles = {
		"/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
		"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",
	};
	
	public static File getFreqFile(Context context, String fileName) {
		File ret = null;
		
		if(fileName!=null) { 
			ret = new File(fileName);
			if(!ret.exists() || !ret.canRead())
				ret = null;
		}
		
		if(ret==null || fileName.equals("Auto")) {
			for(String freqFileName : freqFiles) {
				ret = new File(freqFileName);
				if(!ret.exists() || !ret.canRead()) {
					ret = null;
					continue;
				}
				else break;
			}
		}
		
		if(ret==null) {
			Utils.log("init could not find any frequency files");
		}
		
		return ret;
	}
	
	public static String findWidestFreqString() {
		File file = null;
		Scanner mScanner;
		String freq;
		String widestFreq = null;
		Paint mPaint = new Paint();

		float maxwidth = 0;
		float width;
		
		// size doesn't really matter here, it's the relative width that counts
		mPaint.setTextSize(16);

		file = new File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
		if(file.exists() && file.canRead()) {
			// parse scaling_available_frequencies and find widest string 
			try {
				mScanner = new Scanner(file);
				while(mScanner.hasNext()) {
					freq = mScanner.next();
					freq = String.valueOf(Long.valueOf(freq)/1000);
					width = mPaint.measureText(freq);
					if(width > maxwidth) {
						widestFreq = freq;
						maxwidth = width;
					}
				}
				mScanner.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Utils.log(Log.getStackTraceString(e));
			}
		} else {
			file = new File("/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state");
			if(file.exists() && file.canRead()) {
				// parse time_in_state and find widest string 
				try {
					mScanner = new Scanner(file).useDelimiter("\\s+\\d+\n");
					while(mScanner.hasNext()) {
						freq = mScanner.next();
						freq = String.valueOf(Long.valueOf(freq)/1000);
						width = mPaint.measureText(freq);
						if(width > maxwidth) {
							widestFreq = freq;
							maxwidth = width;
						}
					}
					mScanner.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					Utils.log(Log.getStackTraceString(e));
				}
			}
		}
		
		return widestFreq;
	}
}
