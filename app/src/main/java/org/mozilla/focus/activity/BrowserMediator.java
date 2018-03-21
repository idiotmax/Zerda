package org.mozilla.focus.activity;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.BrowserFragment;

public class BrowserMediator {

    private final MainActivity activity;
    private final MainMediator mainMediator;

    BrowserMediator(@NonNull MainActivity activity, @NonNull MainMediator mainMediator) {
        this.activity = activity;
        this.mainMediator = mainMediator;
    }

    // A.k.a. close Home Screen
    void showBrowserScreen() {
        mainMediator.clearAllFragmentImmediate();
    }


    // If openInNewTab is not provided, we decide based on current state of MainMediator
    void showBrowserScreen(final @NonNull String url) {
        showBrowserScreen(url, mainMediator.isHomeFragmentVisible());
    }

    void showBrowserScreen(@NonNull String url, boolean openInNewTab) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadUrl(url, openInNewTab);
        showBrowserScreenPost();
    }

    void showBrowserScreenForRestoreTabs(@NonNull String tabId) {
        final FragmentManager fragmentManager = this.activity.getSupportFragmentManager();
        findBrowserFragment(fragmentManager).loadTab(tabId);
        showBrowserScreenPost();
    }

    private void showBrowserScreenPost() {
        showBrowserScreen();
        this.activity.sendBrowsingTelemetry();
    }


    private BrowserFragment findBrowserFragment(FragmentManager fm) {
        return (BrowserFragment) fm.findFragmentById(R.id.browser);
    }

    boolean isBrowserFragmentAtTop() {
        return mainMediator.getTopFragment() == null;
    }
}
