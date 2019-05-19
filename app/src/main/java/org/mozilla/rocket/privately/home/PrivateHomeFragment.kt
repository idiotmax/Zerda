/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.privately.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE.SHOW_URL_INPUT
import org.mozilla.focus.widget.FragmentListener.TYPE.TOGGLE_PRIVATE_MODE
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.privately.ShortcutUtils
import org.mozilla.rocket.privately.ShortcutViewModel

class PrivateHomeFragment : LocaleAwareFragment(),
        ScreenNavigator.HomeScreen {

    private lateinit var btnBack: RelativeLayout
    private lateinit var lottieMask: LottieAnimationView
    private lateinit var logoMan: LottieAnimationView
    private lateinit var fakeInput: View

    @Override
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_private_homescreen, container, false)
        btnBack = view.findViewById(R.id.pm_home_back)
        lottieMask = view.findViewById(R.id.pm_home_mask)
        logoMan = view.findViewById(R.id.pm_home_logo)
        fakeInput = view.findViewById(R.id.pm_home_fake_input)

        btnBack.setOnClickListener {
            var parent = activity
            if (parent is FragmentListener) {
                parent.onNotified(this, TOGGLE_PRIVATE_MODE, null)
            }
        }

        fakeInput.setOnClickListener {
            var parent = activity
            if (parent is FragmentListener) {
                parent.onNotified(this, SHOW_URL_INPUT, null)
            }
        }

        observeViewModel()

        return view
    }

    override fun getFragment(): Fragment {
        return this
    }

    private fun observeViewModel() {
        activity?.apply {
            // since the view model is of the activity, use the fragment's activity instead of the fragment itself
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
                    .urlInputState()
                    .observe(this, Observer<Boolean> {
                        it?.apply {
                            onUrlInputScreenVisible(it)
                        }
                    })

            val shortcutViewModel = ViewModelProviders.of(this).get(ShortcutViewModel::class.java)
            monitorShortcutPromotion(this, shortcutViewModel)
            monitorShortcutMessage(this, shortcutViewModel)
            monitorShortcutCreation(this, shortcutViewModel)
        }
    }

    private fun monitorShortcutPromotion(context: Context, model: ShortcutViewModel) {
        model.eventPromoteShortcut.observe(viewLifecycleOwner, Observer { callback ->
            AlertDialog.Builder(context)
                    .setCustomTitle(View.inflate(context, R.layout.dialog_pb_shortcut, null))
                    .setPositiveButton(R.string.private_browsing_dialog_add_shortcut_yes) { _, _ ->
                        callback?.onPositive()
                    }
                    .setNegativeButton(R.string.private_browsing_dialog_add_shortcut_no) { _, _ ->
                        callback?.onNegative()
                    }
                    .setOnCancelListener {
                        callback?.onCancel()
                    }
                    .show()
        })
    }

    private fun monitorShortcutMessage(context: Context, model: ShortcutViewModel) {
        model.eventShowMessage.observe(viewLifecycleOwner, Observer {
            val msgId = it ?: return@Observer
            Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show()
        })
    }

    private fun monitorShortcutCreation(context: Context, model: ShortcutViewModel) {
        model.eventCreateShortcut.observe(viewLifecycleOwner, Observer {
            ShortcutUtils.createShortcut(context)
        })
    }

    override fun applyLocale() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        fun create(): PrivateHomeFragment {
            return PrivateHomeFragment()
        }
    }

    private fun animatePrivateHome() {
        lottieMask.playAnimation()
        logoMan.playAnimation()
    }

    override fun onUrlInputScreenVisible(visible: Boolean) {
        if (visible) {
            logoMan.visibility = View.INVISIBLE
            fakeInput.visibility = View.INVISIBLE
        } else {
            logoMan.visibility = View.VISIBLE
            fakeInput.visibility = View.VISIBLE
        }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (enter) {
            val anim = AnimationUtils.loadAnimation(activity, R.anim.pb_enter)

            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    animatePrivateHome()
                }
            })

            return anim
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim)
        }
    }
}
