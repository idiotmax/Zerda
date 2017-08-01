/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.widget.BackKeyHandleable;

public class MainMediator {

    // For FragmentManager, there is no real top fragment.
    // Instead, we define this sequence for fragments of MainActivity
    // to define that, if there are two visible fragments, which one is top one.
    private final static String[] FRAGMENTS_SEQUENCE = {
            UrlInputFragment.FRAGMENT_TAG,
            BrowserFragment.FRAGMENT_TAG,
            FirstrunFragment.FRAGMENT_TAG,
            HomeFragment.FRAGMENT_TAG
    };

    private final MainActivity activity;

    private View btnMenu;
    private View btnSearch;
    private View btnHome;

    public MainMediator(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    public void registerMenu(@NonNull View menu) {
        this.btnMenu = menu;
    }

    public void registerHome(@NonNull View home) {
        this.btnHome = home;
    }

    public void registerSearch(@NonNull View search) {
        this.btnSearch = search;
    }

    public void showHomeScreen() {
        this.prepareHomeScreen().commit();
    }


    public void showFirstRun() {
        this.prepareFirstRun().commit();
    }

    public void showUrlInput(@Nullable String url) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        this.prepareUrlInput(url).addToBackStack(UrlInputFragment.FRAGMENT_TAG).commit();
    }

    public void showBrowserScreen(@Nullable String url) {
        final FragmentManager fragmentMgr = this.activity.getSupportFragmentManager();
        final Fragment urlInputFrg = fragmentMgr.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        final Fragment homeFrg = fragmentMgr.findFragmentByTag(HomeFragment.FRAGMENT_TAG);

        FragmentTransaction trans = this.prepareBrowsing(url);

        trans = (urlInputFrg == null) ? trans : trans.remove(urlInputFrg);
        trans = (homeFrg == null) ? trans : trans.remove(homeFrg);

        trans.commit();

        this.activity.sendBrowsingTelemetry();
    }

    public void dismissUrlInput() {
        final Fragment top = getTopFragment();
        if (UrlInputFragment.FRAGMENT_TAG.equals(top.getTag())) {
            this.activity.onBackPressed();
        }
    }

    public boolean handleBackKey() {
        final Fragment topFrg = getTopFragment();
        return (topFrg instanceof BackKeyHandleable) && ((BackKeyHandleable) topFrg).onBackPressed();
    }

    public void onFragmentStarted(@NonNull String tag) {
        if (HomeFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFloatingButtons(View.VISIBLE, View.GONE, View.VISIBLE);
        }
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFloatingButtons(View.GONE, View.GONE, View.GONE);
        }
        if (BrowserFragment.FRAGMENT_TAG.equals(tag)) {
            toggleFloatingButtons(View.GONE, View.GONE, View.GONE);
        }
    }

    public void onFragmentStopped(@NonNull String tag) {
        if (UrlInputFragment.FRAGMENT_TAG.equals(tag)) {
            Fragment top = getTopFragment();
            if (top == null) {
                return;
            }
            if (HomeFragment.FRAGMENT_TAG.equals(top.getTag())) {
                toggleFloatingButtons(View.VISIBLE, View.GONE, View.VISIBLE);
            }
            if (BrowserFragment.FRAGMENT_TAG.equals(top.getTag())) {
                toggleFloatingButtons(View.VISIBLE, View.VISIBLE, View.VISIBLE);
            }
        }
    }

    private Fragment getTopFragment() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        for (final String tag : FRAGMENTS_SEQUENCE) {
            final Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null && fragment.isVisible()) {
                return fragment;
            }
        }
        return null;
    }

    private FragmentTransaction prepareBrowsing(@Nullable String url) {
        final FragmentManager fragmentMgr = this.activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentMgr.beginTransaction();

        // Replace all fragments with a fresh browser fragment. This means we either remove the
        // HomeFragment with an UrlInputFragment on top or an old BrowserFragment with an
        // UrlInputFragment.
        final BrowserFragment browserFrg = (BrowserFragment) fragmentMgr
                .findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (browserFrg != null && browserFrg.isVisible()) {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFrg.loadUrl(url);
        } else {
            transaction.replace(R.id.container,
                    this.activity.createBrowserFragment(url),
                    BrowserFragment.FRAGMENT_TAG);
        }
        return transaction;
    }

    private FragmentTransaction prepareFirstRun() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final FirstrunFragment fragment = this.activity.createFirstRunFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container,
                    fragment,
                    FirstrunFragment.FRAGMENT_TAG);
        }

        return transaction;
    }

    private FragmentTransaction prepareHomeScreen() {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final HomeFragment fragment = this.activity.createHomeFragment();

        // We add the home fragment to the layout if it doesn't exist yet. I tried adding the fragment
        // to the layout directly but then I wasn't able to remove it later. It was still visible but
        // without an activity attached. So let's do it manually.
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG) == null) {
            transaction.replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG);
        }
        return transaction;
    }

    private FragmentTransaction prepareUrlInput(@Nullable String url) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        final UrlInputFragment urlFragment = this.activity.createUrlInputFragment(url);
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG);
        return transaction;
    }

    private void toggleFloatingButtons(int search, int home, int menu) {
        if (btnSearch != null) {
            btnSearch.setVisibility(search);
        }

        if (btnHome != null) {
            btnHome.setVisibility(home);
        }

        if (btnMenu != null) {
            btnMenu.setVisibility(menu);
        }
    }
}
