package com.theah64.soundclouddownloader.ui.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.theah64.soundclouddownloader.R;
import com.theah64.soundclouddownloader.adapters.ImageTitleSubtitleAdapter;
import com.theah64.soundclouddownloader.database.Tracks;
import com.theah64.soundclouddownloader.models.ITSNode;
import com.theah64.soundclouddownloader.models.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class TracksFragment extends Fragment implements ImageTitleSubtitleAdapter.TracksCallback {


    public TracksFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View row = inflater.inflate(R.layout.fragment_tracks, container, false);

        final RecyclerView rvTracks = (RecyclerView) row.findViewById(R.id.rvTracks);
        rvTracks.setLayoutManager(new LinearLayoutManager(getContext()));
        final List<Track> trackList = Tracks.getInstance(getContext()).getAll();

        if (trackList != null) {
            //Converting to ITS node
            final List<ITSNode> itsNodes = new ArrayList<>(trackList.size());
            for (final Track track : trackList) {
                itsNodes.add(new ITSNode(track.getArtWorkUrl(), track.getTitle(), getDownloadStatus(track.getDownloadId())));
            }

            final ImageTitleSubtitleAdapter itsAdapter = new ImageTitleSubtitleAdapter(itsNodes, this);
            rvTracks.setAdapter(itsAdapter);
        } else {
            //showing no tracks downloaded text view.
            row.findViewById(R.id.tvNoTracksDownloaded).setVisibility(View.VISIBLE);
        }


        return row;
    }

    private String getDownloadStatus(String downloadId) {
        //TODO:
        return "NONE";
    }

    @Override
    public void onRowClicked(int position) {

    }
}