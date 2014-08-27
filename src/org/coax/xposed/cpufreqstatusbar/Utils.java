package org.coax.xposed.cpufreqstatusbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
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
		XposedBridge.log(s);
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
		result.add("AUTO");
		
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
		
		if(ret==null || fileName.equals("AUTO")) {
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
			Utils.log("Couldn't find any freq files!");
		}
		
		return ret;
	}
}
