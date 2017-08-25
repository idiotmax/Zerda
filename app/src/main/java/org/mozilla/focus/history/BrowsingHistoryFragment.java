package org.mozilla.focus.history;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragment;


public class BrowsingHistoryFragment extends PanelFragment implements View.OnClickListener, HistoryItemAdapter.HistoryListener {

    private RecyclerView mRecyclerView;
    private ViewGroup mContainerEmptyView, mContainerRecyclerView;
    private HistoryItemAdapter mAdapter;

    public static BrowsingHistoryFragment newInstance() {
        return new BrowsingHistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browsing_history, container, false);
        v.findViewById(R.id.browsing_history_btn_clear).setOnClickListener(this);

        mContainerRecyclerView = (ViewGroup) v.findViewById(R.id.browsing_history_recycler_view_container);
        mContainerEmptyView = (ViewGroup) v.findViewById(R.id.browsing_history_empty_view_container);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.browsing_history_recycler_view);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new HistoryItemAdapter(mRecyclerView, getActivity(), this, layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                mAdapter.clear();
                break;
            default:
                break;
        }
    }

    @Override
    public void onEmpty(boolean flag) {
        if(flag) {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.VISIBLE);
        } else{
            mContainerRecyclerView.setVisibility(View.VISIBLE);
            mContainerEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClicked() {
        closePanel();
    }
}
