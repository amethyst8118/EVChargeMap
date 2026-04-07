package net.afsal.evmap.fragment.preference

import android.app.Activity.RESULT_OK
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import net.afsal.evmap.R
import net.afsal.evmap.storage.CloudRepository

class AccountSettingsFragment : BaseSettingsFragment() {
    override val isTopLevel = false

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_account, rootKey)
        
        val accountStatus = findPreference<Preference>("account_status")
        val accountLogout = findPreference<Preference>("account_logout")
        
        accountStatus?.setOnPreferenceClickListener {
            if (FirebaseAuth.getInstance().currentUser == null) {
                launchSignInFlow()
            }
            true
        }
        
        accountLogout?.setOnPreferenceClickListener {
            FirebaseAuth.getInstance().signOut()
            updateUI()
            true
        }

        updateUI()
    }

    fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .setTheme(R.style.FirebaseAuthTheme)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            updateUI()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: android.content.SharedPreferences, key: String?) {
        if (FirebaseAuth.getInstance().currentUser == null) return

        val vehicleName = sharedPreferences.getString("vehicle_name", "") ?: ""
        val vehicleReg = sharedPreferences.getString("vehicle_registration", "") ?: ""
        val chargeRateStr = sharedPreferences.getString("vehicle_charge_rate", "") ?: ""
        val chargeRate = chargeRateStr.toDoubleOrNull() ?: 0.0

        when (key) {
            "vehicle_name" -> {
                findPreference<EditTextPreference>("vehicle_name")?.summary =
                    if (vehicleName.isBlank()) "Enter your vehicle name" else vehicleName
                lifecycleScope.launch {
                    CloudRepository.setVehicleProfile(vehicleName, vehicleReg, chargeRate)
                }
            }
            "vehicle_registration" -> {
                findPreference<EditTextPreference>("vehicle_registration")?.summary =
                    if (vehicleReg.isBlank()) "Enter your registration" else vehicleReg
                lifecycleScope.launch {
                    CloudRepository.setVehicleProfile(vehicleName, vehicleReg, chargeRate)
                }
            }
            "vehicle_charge_rate" -> {
                findPreference<EditTextPreference>("vehicle_charge_rate")?.summary =
                    if (chargeRate > 0) "${chargeRate.toInt()} kW" else getString(R.string.vehicle_charge_rate_summary_default)
                lifecycleScope.launch {
                    CloudRepository.setVehicleProfile(vehicleName, vehicleReg, chargeRate)
                }
            }
        }
    }

    private fun updateUI() {
        val user = FirebaseAuth.getInstance().currentUser
        val accountStatus = findPreference<Preference>("account_status")
        val accountLogout = findPreference<Preference>("account_logout")
        val vehicleNamePref = findPreference<EditTextPreference>("vehicle_name")
        val vehicleRegPref = findPreference<EditTextPreference>("vehicle_registration")
        val chargeRatePref = findPreference<EditTextPreference>("vehicle_charge_rate")
        
        if (user != null) {
            accountStatus?.title = "Signed in as"
            accountStatus?.summary = user.email ?: "Unknown Email"
            accountLogout?.isVisible = true
            vehicleNamePref?.isVisible = true
            vehicleRegPref?.isVisible = true
            chargeRatePref?.isVisible = true
            
            // Sync vehicle profile from Firestore on login
            lifecycleScope.launch {
                val profile = CloudRepository.getVehicleProfile()
                if (profile != null) {
                    preferenceManager.sharedPreferences?.edit()?.apply {
                        putString("vehicle_name", profile.vehicleName)
                        putString("vehicle_registration", profile.vehicleRegistration)
                        putString("vehicle_charge_rate", if (profile.vehicleChargeRate > 0) profile.vehicleChargeRate.toInt().toString() else "")
                        apply()
                    }
                    
                    vehicleNamePref?.text = profile.vehicleName
                    vehicleRegPref?.text = profile.vehicleRegistration
                    chargeRatePref?.text = if (profile.vehicleChargeRate > 0) profile.vehicleChargeRate.toInt().toString() else ""
                    
                    vehicleNamePref?.summary = if (profile.vehicleName.isBlank()) "Enter your vehicle name" else profile.vehicleName
                    vehicleRegPref?.summary = if (profile.vehicleRegistration.isBlank()) "Enter your registration" else profile.vehicleRegistration
                    chargeRatePref?.summary = if (profile.vehicleChargeRate > 0) "${profile.vehicleChargeRate.toInt()} kW" else getString(R.string.vehicle_charge_rate_summary_default)
                } else {
                    vehicleNamePref?.summary = "Enter your vehicle name"
                    vehicleRegPref?.summary = "Enter your registration"
                    chargeRatePref?.summary = getString(R.string.vehicle_charge_rate_summary_default)
                }
            }
        } else {
            accountStatus?.title = "Not signed in"
            accountStatus?.summary = "Click to sign in or create an account"
            accountLogout?.isVisible = false
            vehicleNamePref?.isVisible = false
            vehicleRegPref?.isVisible = false
            chargeRatePref?.isVisible = false
        }
    }
}
