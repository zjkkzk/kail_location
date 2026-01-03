package com.kail.location

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.elvishew.xlog.XLog
import com.kail.utils.GoUtils

class FragmentSettings : PreferenceFragmentCompat() {

    // Set a non-empty decimal EditTextPreference
    private fun setupDecimalEditTextPreference(preference: EditTextPreference?) {
        if (preference != null) {
            preference.summaryProvider = Preference.SummaryProvider<EditTextPreference> { it.text }
            preference.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.length())
            }
            preference.setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().trim().isEmpty()) {
                    if (context != null) {
                        GoUtils.DisplayToast(requireContext(), resources.getString(R.string.app_error_input_null))
                    }
                    false
                } else {
                    true
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main)

        val pfJoystick = findPreference<ListPreference>("setting_joystick_type")
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.summaryProvider = Preference.SummaryProvider<ListPreference> { preference -> preference.entry }
            pfJoystick.setOnPreferenceChangeListener { _, newValue -> newValue.toString().trim().isNotEmpty() }
        }

        val pfWalk = findPreference<EditTextPreference>("setting_walk")
        setupDecimalEditTextPreference(pfWalk)

        val pfRun = findPreference<EditTextPreference>("setting_run")
        setupDecimalEditTextPreference(pfRun)

        val pfBike = findPreference<EditTextPreference>("setting_bike")
        setupDecimalEditTextPreference(pfBike)

        val pfAltitude = findPreference<EditTextPreference>("setting_altitude")
        setupDecimalEditTextPreference(pfAltitude)

        val pfLatOffset = findPreference<EditTextPreference>("setting_lat_max_offset")
        setupDecimalEditTextPreference(pfLatOffset)

        val pfLonOffset = findPreference<EditTextPreference>("setting_lon_max_offset")
        setupDecimalEditTextPreference(pfLonOffset)

        val pLog = findPreference<SwitchPreferenceCompat>("setting_log_off")
        pLog?.setOnPreferenceChangeListener { preference, newValue ->
            if ((preference as SwitchPreferenceCompat).isChecked != newValue as Boolean) {
                XLog.d(preference.key + newValue)

                if (newValue.toString().toBoolean()) {
                    XLog.d("on")
                } else {
                    XLog.d("off")
                }
                true
            } else {
                false
            }
        }

        val pfPosHisValid = findPreference<EditTextPreference>("setting_history_expiration")
        setupDecimalEditTextPreference(pfPosHisValid)

        // 设置版本号
        val verName: String = GoUtils.getVersionName(requireContext())
        val pfVersion = findPreference<Preference>("setting_version")
        pfVersion?.summary = verName
    }
}
