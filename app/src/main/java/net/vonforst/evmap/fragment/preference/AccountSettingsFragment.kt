package net.vonforst.evmap.fragment.preference

import android.app.Activity.RESULT_OK
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import net.vonforst.evmap.R

class AccountSettingsFragment : BaseSettingsFragment() {
    override val isTopLevel = false

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.settings_account)
        
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
            .build()
        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            updateUI()
        }
    }

    private fun updateUI() {
        val user = FirebaseAuth.getInstance().currentUser
        val accountStatus = findPreference<Preference>("account_status")
        val accountLogout = findPreference<Preference>("account_logout")
        
        if (user != null) {
            accountStatus?.title = "Signed in as"
            accountStatus?.summary = user.email ?: "Unknown Email"
            accountLogout?.isVisible = true
        } else {
            accountStatus?.title = "Not signed in"
            accountStatus?.summary = "Click to sign in or create an account"
            accountLogout?.isVisible = false
        }
    }
}
