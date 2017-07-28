/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.View;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;
import org.mozilla.focus.fragment.FirstrunFragment;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.home.TopSitesPresenter;
import org.mozilla.focus.locale.LocaleAwareAppCompatActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.urlinput.UrlInputFragment;
import org.mozilla.focus.urlinput.UrlInputPresenter;
import org.mozilla.focus.utils.SafeIntent;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.web.BrowsingSession;
import org.mozilla.focus.web.IWebView;
import org.mozilla.focus.web.WebViewProvider;
import org.mozilla.focus.widget.FragmentListener;

public class MainActivity extends LocaleAwareAppCompatActivity implements FragmentListener {
    public static final String ACTION_OPEN = "open";

    public static final String EXTRA_TEXT_SELECTION = "text_selection";

    private String pendingUrl;

    private BottomSheetDialog menu;
    private BottomSheetDialog historyAndDownload;
    private FloatingActionButton btnSearch;
    private FloatingActionButton btnHome;
    private FloatingActionButton btnMenu;

    private TopSitesPresenter topSitesPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initViews();

        SafeIntent intent = new SafeIntent(getIntent());

        if (savedInstanceState == null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                final String url = intent.getDataString();

                BrowsingSession.getInstance().loadCustomTabConfig(this, intent);

                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    pendingUrl = url;
                    showFirstrun();
                } else {
                    showBrowserScreen(url);
                }
            } else {
                if (Settings.getInstance(this).shouldShowFirstrun()) {
                    showFirstrun();
                } else {
                    showHomeScreen();
                }
            }
        }

        WebViewProvider.preload(this);
    }

    @Override
    public void applyLocale() {
        // We don't care here: all our fragments update themselves as appropriate
    }

    @Override
    protected void onStart() {
        HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(HomeFragment.FRAGMENT_TAG);
        if (homeFragment != null) {
            getTopSitesPresenter().setView(homeFragment);
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        TelemetryWrapper.startSession();
    }

    @Override
    protected void onPause() {
        super.onPause();

        TelemetryWrapper.stopSession();
    }

    @Override
    protected void onStop() {
        super.onStop();

        TelemetryWrapper.stopMainActivity();
    }

    @Override
    protected void onNewIntent(Intent unsafeIntent) {
        final SafeIntent intent = new SafeIntent(unsafeIntent);
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // We can't update our fragment right now because we need to wait until the activity is
            // resumed. So just remember this URL and load it in onResumeFragments().
            pendingUrl = intent.getDataString();
        }

        if (ACTION_OPEN.equals(intent.getAction())) {
            TelemetryWrapper.openNotificationActionEvent();
        }

        // We do not care about the previous intent anymore. But let's remember this one.
        setIntent(unsafeIntent);
        BrowsingSession.getInstance().loadCustomTabConfig(this, intent);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (pendingUrl != null && !Settings.getInstance(this).shouldShowFirstrun()) {
            // We have received an URL in onNewIntent(). Let's load it now.
            // Unless we're trying to show the firstrun screen, in which case we leave it pending until
            // firstrun is dismissed.
            showBrowserScreen(pendingUrl);
            pendingUrl = null;
        }
    }

    private void initViews() {
        int visibility = getWindow().getDecorView().getSystemUiVisibility();
        // do not overwrite existing value
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(visibility);

        btnSearch = (FloatingActionButton) findViewById(R.id.btn_search);
        btnHome = (FloatingActionButton) findViewById(R.id.btn_home);
        btnMenu = (FloatingActionButton) findViewById(R.id.btn_menu);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUrlInput(null);
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        setUpMenu();
        setUpHistoryAndDownload();
    }

    private void toggleFloatingButtonsVisibility(int visibility) {
        btnSearch.setVisibility(visibility);
        btnHome.setVisibility(visibility);
        btnMenu.setVisibility(visibility);
    }

    private void setUpMenu() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_main_menu, null);
        menu = new BottomSheetDialog(this);
        menu.setContentView(sheet);
    }

    private void setUpHistoryAndDownload() {
        final View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_history_download, null);
        historyAndDownload = new BottomSheetDialog(this);
        historyAndDownload.setContentView(sheet);
    }

    private void showMenu() {
        menu.show();
    }

    private void showHistoryAndDownload(boolean isHistory) {
        historyAndDownload.show();
    }

    public void onMenuItemClicked(View v) {
        menu.cancel();
        switch (v.getId()) {
            case R.id.menu_download:
                onHistoryClicked();
                break;
            case R.id.menu_history:
                onDownloadClicked();
                break;
            case R.id.menu_preferences:
                onPreferenceClicked();
                break;
            case R.id.action_back:
            case R.id.action_next:
            case R.id.action_refresh:
                onMenuBrowsingItemClicked(v);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" +
                        " known ids");
        }
    }

    public void onMenuBrowsingItemClicked(View v) {
        final BrowserFragment browserFragment = getBrowserFragment();
        if(browserFragment == null || !browserFragment.isVisible()) {
            return;
        }
        switch (v.getId()) {
            case R.id.action_back:
                onBackClicked(browserFragment);
                break;
            case R.id.action_next:
                onNextClicked(browserFragment);
                break;
            case R.id.action_refresh:
                onRefreshClicked(browserFragment);
                break;
            default:
                throw new RuntimeException("Unknown id in menu, onMenuBrowsingItemClicked() is" +
                        " only for known ids");
        }
    }

    private void onPreferenceClicked() {
        openPreferences();
    }

    private void onHistoryClicked() {
        showHistoryAndDownload(true);
    }

    private void onDownloadClicked() {
        showHistoryAndDownload(false);
    }

    private BrowserFragment getBrowserFragment() {
        return (BrowserFragment) getSupportFragmentManager().findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
    }

    private void onBackClicked(final BrowserFragment browserFragment) {
        browserFragment.goBack();
    }

    private void onNextClicked(final BrowserFragment browserFragment) {
        browserFragment.goForward();
    }

    private void onRefreshClicked(final BrowserFragment browserFragment) {
        browserFragment.reload();
    }

    private void showHomeScreen() {
        toggleFloatingButtonsVisibility(View.VISIBLE);

        // We add the home fragment to the layout if it doesn't exist yet. I tried adding the fragment
        // to the layout directly but then I wasn't able to remove it later. It was still visible but
        // without an activity attached. So let's do it manually.
        final FragmentManager fragmentManager = getSupportFragmentManager();
        final TopSitesPresenter presenter = getTopSitesPresenter();
        final HomeFragment fragment = HomeFragment.create();
        presenter.setView(fragment);
        if (fragmentManager.findFragmentByTag(HomeFragment.FRAGMENT_TAG) == null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.container, fragment, HomeFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    private void showFirstrun() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(FirstrunFragment.FRAGMENT_TAG) == null) {
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.container, FirstrunFragment.create(), FirstrunFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    private void showBrowserScreen(String url) {
        toggleFloatingButtonsVisibility(View.VISIBLE);

        final FragmentManager fragmentMgr = getSupportFragmentManager();

        // Replace all fragments with a fresh browser fragment. This means we either remove the
        // HomeFragment with an UrlInputFragment on top or an old BrowserFragment with an
        // UrlInputFragment.
        final BrowserFragment browserFrg = (BrowserFragment) fragmentMgr
                .findFragmentByTag(BrowserFragment.FRAGMENT_TAG);

        final Fragment urlInputFrg = fragmentMgr.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        final Fragment homeFrg = fragmentMgr.findFragmentByTag(HomeFragment.FRAGMENT_TAG);

        FragmentTransaction trans = fragmentMgr.beginTransaction();

        trans = (urlInputFrg == null) ? trans : trans.remove(urlInputFrg);
        trans = (homeFrg == null) ? trans : trans.remove(homeFrg);

        if (browserFrg != null && browserFrg.isVisible()) {
            // Reuse existing visible fragment - in this case we know the user is already browsing.
            // The fragment might exist if we "erased" a browsing session, hence we need to check
            // for visibility in addition to existence.
            browserFrg.loadUrl(url);
        } else {
            trans.replace(R.id.container, BrowserFragment.create(url), BrowserFragment.FRAGMENT_TAG);
        }

        trans.commit();

        final SafeIntent intent = new SafeIntent(getIntent());

        if (intent.getBooleanExtra(EXTRA_TEXT_SELECTION, false)) {
            TelemetryWrapper.textSelectionIntentEvent();
        } else if (BrowsingSession.getInstance().isCustomTab()) {
            TelemetryWrapper.customTabsIntentEvent(BrowsingSession.getInstance().getCustomTabConfig().getOptionsList());
        } else {
            TelemetryWrapper.browseIntentEvent();
        }
    }

    private void showUrlInput(@Nullable String url) {
        toggleFloatingButtonsVisibility(View.GONE);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment existingFragment = fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (existingFragment != null && existingFragment.isAdded() && !existingFragment.isRemoving()) {
            // We are already showing an URL input fragment. This might have been a double click on the
            // fake URL bar. Just ignore it.
            return;
        }

        final UrlInputPresenter presenter = new UrlInputPresenter(this);
        final UrlInputFragment urlFragment = UrlInputFragment.create(presenter, url);
        presenter.setView(urlFragment);
        fragmentManager.beginTransaction()
                .add(R.id.container, urlFragment, UrlInputFragment.FRAGMENT_TAG)
                .commit();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        if (name.equals(IWebView.class.getName())) {
            View v = WebViewProvider.create(this, attrs);
            return v;
        }

        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onBackPressed() {
        final FragmentManager fragmentManager = getSupportFragmentManager();

        final UrlInputFragment urlInputFragment = (UrlInputFragment) fragmentManager.findFragmentByTag(UrlInputFragment.FRAGMENT_TAG);
        if (urlInputFragment != null &&
                urlInputFragment.isVisible() &&
                urlInputFragment.onBackPressed()) {
            // The URL input fragment has handled the back press. It does its own animations so
            // we do not try to remove it from outside.
            return;
        }

        final BrowserFragment browserFragment = (BrowserFragment) fragmentManager.findFragmentByTag(BrowserFragment.FRAGMENT_TAG);
        if (browserFragment != null &&
                browserFragment.isVisible() &&
                browserFragment.onBackPressed()) {
            // The Browser fragment handles back presses on its own because it might just go back
            // in the browsing history.
            return;
        }

        super.onBackPressed();
    }

    public void firstrunFinished() {
        if (pendingUrl != null) {
            // We have received an URL in onNewIntent(). Let's load it now.
            showBrowserScreen(pendingUrl);
            pendingUrl = null;
        } else {
            showHomeScreen();
        }
    }

    private void onFragmentDismiss(@NonNull Fragment from, @Nullable Object payload) {
        final FragmentTransaction t = getSupportFragmentManager().beginTransaction().remove(from);

        if ((payload != null) && (payload instanceof Boolean) && (((Boolean) payload)).booleanValue()) {
            t.commitAllowingStateLoss();
        } else {
            t.commit();
        }

        // TODO: dismissing UrlInputFragment, so we display FAB. This method is not good, need
        // a better way to deal with it. Maybe better Fragments stack management.
        final int visibility = (from instanceof UrlInputFragment) ? View.VISIBLE : View.GONE;
        toggleFloatingButtonsVisibility(visibility);
    }

    @Override
    public void onNotified(@NonNull Fragment from, @NonNull TYPE type, @Nullable Object payload) {
        switch (type) {
            case OPEN_URL:
                if ((payload != null) && (payload instanceof String)) {
                    showBrowserScreen(payload.toString());
                }
                break;
            case OPEN_PREFERENCE:
                openPreferences();
                break;
            case SHOW_HOME:
                showHomeScreen();
                break;
            case SHOW_URL_INPUT:
                final String url = (payload != null) ? payload.toString() : null;
                showUrlInput(url);
                break;
            case DISMISS:
                onFragmentDismiss(from, payload);
                break;
        }
    }

    private TopSitesPresenter getTopSitesPresenter() {
        if (topSitesPresenter == null){
            topSitesPresenter = new TopSitesPresenter();
        }
        return topSitesPresenter;
    }
}
