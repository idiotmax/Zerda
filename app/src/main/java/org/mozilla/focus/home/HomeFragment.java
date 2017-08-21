/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.BrowsingHistoryManager;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.focus.widget.FragmentListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class HomeFragment extends Fragment implements TopSitesContract.View {

    public static final String FRAGMENT_TAG = "homescreen";
    public static final String TOPSITES_PREF = "topsites_pref";
    public static final int TOP_SITES_QUERY_LIMIT = 8;
    public static final int TOP_SITES_QUERY_MIN_VIEW_COUNT = 1;

    private TopSitesContract.Presenter presenter;
    private RecyclerView recyclerView;
    private View btnMenu;
    private View fakeInput;
    private SiteItemClickListener clickListener = new SiteItemClickListener();
    private TopSiteAdapter topSiteAdapter;
    private JSONArray orginalDefaultSites = null;

    public static HomeFragment create() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.presenter = new TopSitesPresenter();
        this.presenter.setView(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_homescreen, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.main_list);
        this.btnMenu = view.findViewById(R.id.btn_menu);
        this.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity parent = getActivity();
                if (parent instanceof FragmentListener) {
                    ((FragmentListener) parent).onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_MENU,
                            null);
                }
            }
        });

        this.fakeInput = view.findViewById(R.id.home_fragment_fake_input);
        this.fakeInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity parent = getActivity();
                if (parent instanceof FragmentListener) {
                    ((FragmentListener) parent).onNotified(HomeFragment.this,
                            FragmentListener.TYPE.SHOW_URL_INPUT,
                            null);
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedState) {
//        this.presenter.populateSites();
    }

    @Override
    public void onStart() {
        super.onStart();
        initDefaultSites();
        BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STARTED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity parent = getActivity();
        if (parent instanceof FragmentListener) {
            ((FragmentListener) parent).onNotified(this,
                    FragmentListener.TYPE.FRAGMENT_STOPPED,
                    FRAGMENT_TAG);
        }
    }

    @Override
    public void showSites(@NonNull List<Site> sites) {
        if (this.topSiteAdapter == null) {
            this.topSiteAdapter = new TopSiteAdapter(sites, clickListener, clickListener);
            this.recyclerView.setAdapter(topSiteAdapter);
        } else {
            this.recyclerView.setAdapter(topSiteAdapter);
            this.topSiteAdapter.setSites(sites);
        }

    }

    @Override
    public void appendSite(@NonNull Site site) {
        final TopSiteAdapter adapter = (TopSiteAdapter) this.recyclerView.getAdapter();
        adapter.addSite(adapter.getItemCount(), site);
    }

    @Override
    public void removeSite(@NonNull Site site) {
        this.topSiteAdapter.setSites(presenter.getSites());
    }

    @Override
    public void onSiteChanged(@NonNull Site site) {
        throw new NoSuchMethodError("Not implement yet");
    }

    public void setPresenter(TopSitesContract.Presenter presenter) {
        this.presenter = presenter;
    }


    public void toggleFakeUrlInput(boolean visible) {
        final int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        this.fakeInput.setVisibility(visibility);
    }

    private class SiteItemClickListener implements View.OnClickListener, View.OnLongClickListener {

        @Override
        public void onClick(View v) {
            final Site site = (Site) v.getTag();
            final Activity parent = getActivity();
            if ((site != null) && (parent instanceof FragmentListener)) {
                ((FragmentListener) parent).onNotified(HomeFragment.this,
                        FragmentListener.TYPE.OPEN_URL,
                        site.getUrl());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            final Site site = (Site) v.getTag();

            if (site == null) {
                return false;
            }

            v.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu,
                                                View v,
                                                ContextMenu.ContextMenuInfo menuInfo) {

                    final MenuInflater inflater = new MenuInflater(getContext());
                    inflater.inflate(R.menu.menu_top_site_item, menu);
                    menu.findItem(R.id.remove)
                            .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    if (site.getId() < 0) {
                                        HomeFragment.this.presenter.removeSite(site);
                                        HomeFragment.this.removeDefaultSites(site);
                                        restoreDefaultSite();
                                        BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
                                    } else {
                                        BrowsingHistoryManager.getInstance().delete(site.getId(), mTopSiteDeleteListener);
                                    }

                                    return true;
                                }
                            });
                }
            });
            v.showContextMenu();

            return true;
        }
    }

    private BrowsingHistoryManager.AsyncQueryListener mTopSitesQueryListener = new BrowsingHistoryManager.AsyncQueryListener() {
        @Override
        public void onQueryComplete(List sites) {
            List<Site> querySites = new ArrayList<>();
            for (Object site : sites) {
                if (site instanceof Site) {
                    querySites.add((Site)site);
                }
            }

            HomeFragment.this.mergeQueryAndDefaultSites(querySites);
        }
    };

    private BrowsingHistoryManager.AsyncDeleteListener mTopSiteDeleteListener = new BrowsingHistoryManager.AsyncDeleteListener() {
        @Override
        public void onDeleteComplete(int result, long id) {
            BrowsingHistoryManager.getInstance().queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT, mTopSitesQueryListener);
        }
    };

    private void mergeQueryAndDefaultSites(List<Site> querySites) {
        List<Site> topSites = new ArrayList<>(this.presenter.getSites());
        Iterator<Site> sitesIterator = topSites.iterator();
        while (sitesIterator.hasNext()) {
            Site temp = sitesIterator.next();
            if (temp.getId() >= 0) {
                sitesIterator.remove();
            }
        }

        topSites.addAll(querySites);
        TopSideComparator topSideComparator = new TopSideComparator();
        Collections.sort(topSites, topSideComparator);

        if (topSites.size() > TOP_SITES_QUERY_LIMIT) {
            List<Site> removeSites = topSites.subList(TOP_SITES_QUERY_LIMIT, topSites.size());
            removeDefaultSites(removeSites);

            topSites = topSites.subList(0, TOP_SITES_QUERY_LIMIT);
        }

        this.presenter.setSites(topSites);
        this.presenter.populateSites();
    }

    private void initDefaultSites() {
        String obj_sites = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(HomeFragment.TOPSITES_PREF, null);

        //if no default sites data in SharedPreferences, load data from assets.
        if (obj_sites == null) {
            this.orginalDefaultSites = TopSitesUtils.getDefaultSitesJsonArrayFromAssets(getContext());
        } else {
            try {
                this.orginalDefaultSites = new JSONArray(obj_sites);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }

        List<Site> defaultSites = TopSitesUtils.paresJsonToList(getContext(), this.orginalDefaultSites);
        this.presenter.setSites(defaultSites);
    }

    private void restoreDefaultSite() {
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .edit()
                .putString(HomeFragment.TOPSITES_PREF, this.orginalDefaultSites.toString())
                .apply();
    }

    private void removeDefaultSites(List<Site> removeSites) {
        boolean isRemove = false;
        for(int i = 0; i < removeSites.size(); i++) {
            Site rSite = removeSites.get(i);
            if (rSite.getId() < 0) {
                removeDefaultSites(rSite);
                isRemove = true;
            }
        }

        if (isRemove) {
            restoreDefaultSite();
        }
    }

    private void removeDefaultSites(Site removeSite) {
        try {
            if (this.orginalDefaultSites != null) {
                for (int i = 0; i < this.orginalDefaultSites.length(); i++) {
                    long id = ((JSONObject) this.orginalDefaultSites.get(i)).getLong("id");

                    if (id == removeSite.getId()) {
                        this.orginalDefaultSites.remove(i);
                        break;
                    }
                }
            }
        }  catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
