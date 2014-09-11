package org.coax.xposed.cpufreqstatusbar.activities;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import info.mzimmermann.libxposed.LXTools;
import info.mzimmermann.libxposed.apps.LXMyApp;
import org.coax.xposed.cpufreqstatusbar.R;
import org.coax.xposed.cpufreqstatusbar.Utils;
import org.coax.xposed.cpufreqstatusbar.widget.CpuFreq;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	public static final String ACTION_SETTINGS_UPDATE = "cpufreq-statusbar-settings-update";
	private static Context mContext = null;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mContext = getApplicationContext();
		LXTools.removeInvalidPreferences(Utils.prefs, mContext.getSharedPreferences(CpuFreq.PREF_KEY, 0));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		// Add 'general' preferences.
		addPreferencesFromResource(R.xml.pref_general);

		ListPreference frequency_file = (ListPreference)findPreference("frequency_file");
		String[] files = Utils.getFrequencyFiles();
		frequency_file.setEntries(files);
		frequency_file.setEntryValues(files);
		if (frequency_file.getValue() == null) {
			frequency_file.setValue("Auto");
		}

		bindPreferenceSummaryToValue(findPreference("frequency_file"));
		bindPreferenceSummaryToValue(findPreference("update_interval"));
		bindPreferenceSummaryToValue(findPreference("position"));
		bindPreferenceSummaryToValue(findPreference("top_padding"));
		bindPreferenceSummaryToValue(findPreference("measurement"));
		bindPreferenceSummaryToValue(findPreference("show_unit"));
		bindPreferenceSummaryToValue(findPreference("font_size"));
		bindPreferenceSummaryToValue(findPreference("color_mode"));
		bindPreferenceSummaryToValue(findPreference("configured_color"));
		bindPreferenceSummaryToValue(findPreference("color_low"));
		bindPreferenceSummaryToValue(findPreference("color_middle"));
		bindPreferenceSummaryToValue(findPreference("color_high"));
		bindPreferenceSummaryToValue(findPreference("freq_middle"));
		bindPreferenceSummaryToValue(findPreference("freq_high"));
		bindPreferenceSummaryToValue(findPreference("enable_logging"));
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return false;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS
				|| Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
				|| !isXLargeTablet(context);
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value==null ? "" : value.toString();
			PreferenceManager pm = preference.getPreferenceManager();

			// get color_mode
			String sColorMode = "0";
			int color_mode = 0;
			try {
				if(preference.getKey().equals("color_mode"))
					sColorMode = stringValue;
				else
					sColorMode = PreferenceManager.getDefaultSharedPreferences(
							preference.getContext()).getString("color_mode", "0");
				color_mode = Integer.parseInt(sColorMode);
			}
			catch(Exception e){
			}

			switch(color_mode) {
			case 1:
				pm.findPreference("configured_color").setEnabled(true);
				pm.findPreference("color_low").setEnabled(false);
				pm.findPreference("color_middle").setEnabled(false);
				pm.findPreference("color_high").setEnabled(false);
				pm.findPreference("freq_middle").setEnabled(false);
				pm.findPreference("freq_high").setEnabled(false);
				break;
			case 2:
				pm.findPreference("configured_color").setEnabled(false);
				pm.findPreference("color_low").setEnabled(true);
				pm.findPreference("color_middle").setEnabled(true);
				pm.findPreference("color_high").setEnabled(true);
				pm.findPreference("freq_middle").setEnabled(true);
				pm.findPreference("freq_high").setEnabled(true);
				break;
			case 0:
			default:
				pm.findPreference("configured_color").setEnabled(false);
				pm.findPreference("color_low").setEnabled(false);
				pm.findPreference("color_middle").setEnabled(false);
				pm.findPreference("color_high").setEnabled(false);
				pm.findPreference("freq_middle").setEnabled(false);
				pm.findPreference("freq_high").setEnabled(false);
				break;
			}

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			} else if(!(preference instanceof ColorPickerPreference) && !(preference instanceof CheckBoxPreference)) {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #sBindPreferenceSummaryToValueListener
	 */
	private static void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		LXMyApp.setTransferOnPreferenceChangeListener(ACTION_SETTINGS_UPDATE, preference, sBindPreferenceSummaryToValueListener);

		// Trigger the listener immediately with the preference's
		// current value.
		sBindPreferenceSummaryToValueListener.onPreferenceChange(
				preference,
				PreferenceManager.getDefaultSharedPreferences(preference.getContext())
					.getAll().get(preference.getKey()));
	}

	/**
	 * This fragment shows general preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class GeneralPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.pref_general);
		}
	}
}
