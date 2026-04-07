package net.afsal.evmap.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import net.afsal.evmap.R
import net.afsal.evmap.databinding.FragmentOnboardingBinding
import net.afsal.evmap.databinding.FragmentOnboardingDataSourceBinding
import net.afsal.evmap.databinding.FragmentOnboardingIconsBinding
import net.afsal.evmap.databinding.FragmentOnboardingLoginBinding
import net.afsal.evmap.databinding.FragmentOnboardingWelcomeBinding
import net.afsal.evmap.model.FILTERS_DISABLED
import net.afsal.evmap.navigation.safeNavigate
import net.afsal.evmap.storage.PreferenceDataSource
import net.afsal.evmap.ui.CustomUrlSpan
import net.afsal.evmap.ui.replaceUrlSpansWithCustom
import net.afsal.evmap.waitForLayout

class OnboardingFragment : Fragment() {
    private lateinit var binding: FragmentOnboardingBinding
    private lateinit var adapter: OnboardingViewPagerAdapter
    private lateinit var prefs: PreferenceDataSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = PreferenceDataSource(requireContext())
        binding = FragmentOnboardingBinding.inflate(inflater)

        adapter = OnboardingViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.pageIndicatorView.count = adapter.itemCount
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                binding.pageIndicatorView.onPageScrollStateChanged(state)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                binding.pageIndicatorView.onPageScrolled(
                    position,
                    positionOffset,
                    positionOffsetPixels
                )
            }

            override fun onPageSelected(position: Int) {
                binding.forward?.visibility =
                    if (position == adapter.itemCount - 1) View.INVISIBLE else View.VISIBLE
                binding.backward?.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            }
        })
        binding.forward?.setOnClickListener {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
        }
        binding.backward?.setOnClickListener {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem - 1, true)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.waitForLayout {
            binding.viewPager.currentItem = if (prefs.welcomeDialogShown) {
                // skip to last page for selecting data source or accepting the privacy policy
                adapter.itemCount - 1
            } else {
                0
            }
        }
    }

    fun goToNext() {
        if (binding.viewPager.currentItem == adapter.itemCount - 1) {
            findNavController().safeNavigate(OnboardingFragmentDirections.actionOnboardingToMap())
        } else {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
        }
    }
}

class OnboardingViewPagerAdapter(fragment: Fragment) :
    FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> WelcomeFragment()
        1 -> IconsFragment()
        2 -> LoginOnboardingFragment()
        3 -> DataSourceSelectFragment()
        else -> throw IllegalArgumentException()
    }
}

abstract class OnboardingPageFragment : Fragment() {
    lateinit var parent: OnboardingFragment

    override fun onAttach(context: Context) {
        super.onAttach(context)
        parent = parentFragment as OnboardingFragment
    }
}

class WelcomeFragment : OnboardingPageFragment() {
    private lateinit var binding: FragmentOnboardingWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingWelcomeBinding.inflate(inflater, container, false)

        binding.btnGetStarted.setOnClickListener {
            parent.goToNext()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val drawable = (binding.animationView as ImageView).drawable
        if (drawable is AnimatedVectorDrawable) {
            drawable.start()
        }
    }

    override fun onPause() {
        super.onPause()
        val drawable = (binding.animationView as ImageView).drawable
        if (drawable is AnimatedVectorDrawable) {
            drawable.stop()
        }
    }
}

class IconsFragment : OnboardingPageFragment() {
    private lateinit var binding: FragmentOnboardingIconsBinding

    val labels
        get() = listOf(
            binding.iconLabel1,
            binding.iconLabel2,
            binding.iconLabel3,
            binding.iconLabel4,
            binding.iconLabel5
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingIconsBinding.inflate(inflater, container, false)

        binding.btnGetStarted.setOnClickListener {
            parent.goToNext()
        }
        labels.forEach { it.alpha = 0f }

        return binding.root
    }

    @SuppressLint("Recycle")
    override fun onResume() {
        super.onResume()
        val animators = labels.flatMapIndexed { i, view ->
            listOf(
                ObjectAnimator.ofFloat(view, "translationY", -20f, 0f).apply {
                    startDelay = 40L * i
                    interpolator = DecelerateInterpolator()
                },
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    startDelay = 40L * i
                    interpolator = DecelerateInterpolator()
                }
            )
        }
        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        labels.forEach { it.alpha = 0f }
    }
}

class LoginOnboardingFragment : OnboardingPageFragment() {
    private lateinit var binding: FragmentOnboardingLoginBinding

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingLoginBinding.inflate(inflater, container, false)

        binding.btnSignIn.setOnClickListener {
            launchSignInFlow()
        }

        binding.btnSkip.setOnClickListener {
            parent.goToNext()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateLoginState()
    }

    private fun launchSignInFlow() {
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
            updateLoginState()
            // Auto-advance to next page after successful sign-in
            parent.goToNext()
        }
    }

    private fun updateLoginState() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            binding.loginTitle.text = "Signed in as"
            binding.loginDesc.text = user.email ?: "Unknown Email"
            binding.btnSignIn.text = getString(R.string.get_started)
            binding.btnSignIn.setOnClickListener { parent.goToNext() }
            binding.btnSkip.visibility = View.GONE
        }
    }
}

class DataSourceSelectFragment : OnboardingPageFragment() {
    private lateinit var prefs: PreferenceDataSource
    private lateinit var binding: FragmentOnboardingDataSourceBinding

    val animatedItems
        get() = listOf(
            binding.rgDataSource.rbOpenChargeMap,
            binding.rgDataSource.textView28,
            binding.dataSourceHint
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingDataSourceBinding.inflate(inflater, container, false)
        prefs = PreferenceDataSource(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnGetStarted.visibility = View.INVISIBLE

        for (rb in listOf(
            binding.rgDataSource.rbOpenChargeMap
        )) {
            rb.setOnCheckedChangeListener { _, _ ->
                if (binding.btnGetStarted.visibility == View.INVISIBLE) {
                    binding.btnGetStarted.visibility = View.VISIBLE
                    ObjectAnimator.ofFloat(binding.btnGetStarted, "alpha", 0f, 1f).apply {
                        interpolator = DecelerateInterpolator()
                    }.start()
                }
            }
        }
        if (prefs.dataSourceSet) {
            when (prefs.dataSource) {
                "openchargemap" -> binding.rgDataSource.rbOpenChargeMap.isChecked = true
            }
        }

        binding.btnGetStarted.setOnClickListener {
            val result = if (binding.rgDataSource.rbOpenChargeMap.isChecked) {
                "openchargemap"
            } else {
                return@setOnClickListener
            }
            prefs.dataSource = result
            prefs.privacyAccepted = true
            prefs.filterStatus = FILTERS_DISABLED
            prefs.dataSourceSet = true
            prefs.welcomeDialogShown = true
            parent.goToNext()
        }
        animatedItems.forEach { it.alpha = 0f }
    }

    @SuppressLint("Recycle")
    override fun onResume() {
        super.onResume()
        val animators = animatedItems.flatMapIndexed { i, view ->
            listOf(
                ObjectAnimator.ofFloat(view, "translationY", 20f, 0f).apply {
                    startDelay = 40L * i
                    interpolator = DecelerateInterpolator()
                },
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                    startDelay = 40L * i
                    interpolator = DecelerateInterpolator()
                }
            )
        }
        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        animatedItems.forEach { it.alpha = 0f }
    }
}
