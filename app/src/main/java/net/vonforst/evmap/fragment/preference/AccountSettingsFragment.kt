package net.vonforst.evmap.fragment.preference

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
import net.vonforst.evmap.R
import net.vonforst.evmap.storage.CloudRepository

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

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .setTheme(R.style.AppTheme)
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

        when (key) {
            "vehicle_name" -> {
                findPreference<EditTextPreference>("vehicle_name")?.summary =
                    if (vehicleName.isBlank()) "Enter your vehicle name" else vehicleName
                // Push merged profile to cloud
                lifecycleScope.launch {
                    CloudRepository.setVehicleProfile(vehicleName, vehicleReg)
                }
            }
            "vehicle_registration" -> {
                findPreference<EditTextPreference>("vehicle_registration")?.summary =
                    if (vehicleReg.isBlank()) "Enter your registration" else vehicleReg
                // Push merged profile to cloud
                lifecycleScope.launch {
                    CloudRepository.setVehicleProfile(vehicleName, vehicleReg)
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
        
        if (user != null) {
            accountStatus?.title = "Signed in as"
            accountStatus?.summary = user.email ?: "Unknown Email"
            accountLogout?.isVisible = true
            vehicleNamePref?.isVisible = true
            vehicleRegPref?.isVisible = true
            
            // Sync vehicle profile from Firestore on login
            lifecycleScope.launch {
                val profile = CloudRepository.getVehicleProfile()
                if (profile != null) {
                    preferenceManager.sharedPreferences?.edit()?.apply {
                        putString("vehicle_name", profile.vehicleName)
                        putString("vehicle_registration", profile.vehicleRegistration)
                        apply()
                    }
                    
                    vehicleNamePref?.text = profile.vehicleName
                    vehicleRegPref?.text = profile.vehicleRegistration
                    vehicleNamePref?.summary = if (profile.vehicleName.isBlank()) "Enter your vehicle name" else profile.vehicleName
                    vehicleRegPref?.summary = if (profile.vehicleRegistration.isBlank()) "Enter your registration" else profile.vehicleRegistration
                } else {
                    vehicleNamePref?.summary = "Enter your vehicle name"
                    vehicleRegPref?.summary = "Enter your registration"
                }
            }
        } else {
            accountStatus?.title = "Not signed in"
            accountStatus?.summary = "Click to sign in or create an account"
            accountLogout?.isVisible = false
            vehicleNamePref?.isVisible = false
            vehicleRegPref?.isVisible = false
        }
    }
}
