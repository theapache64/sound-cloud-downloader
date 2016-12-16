package com.theah64.soundclouddownloader.ui.fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.theah64.soundclouddownloader.R;
import com.theah64.soundclouddownloader.adapters.ITSAdapter;
import com.theah64.soundclouddownloader.database.Playlists;
import com.theah64.soundclouddownloader.database.Tracks;
import com.theah64.soundclouddownloader.interfaces.TrackListener;
import com.theah64.soundclouddownloader.models.Playlist;
import com.theah64.soundclouddownloader.models.Track;
import com.theah64.soundclouddownloader.services.DownloaderService;
import com.theah64.soundclouddownloader.ui.activities.PlaylistTracksActivity;
import com.theah64.soundclouddownloader.utils.App;
import com.theah64.soundclouddownloader.widgets.ThemedSnackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlaylistsFragment extends BaseMusicFragment implements ITSAdapter.TracksCallback, PopupMenu.OnMenuItemClickListener, TrackListener {


    private static final String X = PlaylistsFragment.class.getSimpleName();
    private List<Playlist> playlists;
    private Playlist currentPlaylist;
    private Playlists playlistsTable;
    private Tracks tracksTable;
    private ITSAdapter itsAdapter;
    private int currentPosition;
    private View layout;

    public PlaylistsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        layout = inflater.inflate(R.layout.fragment_playlists, container, false);
        playlistsTable = Playlists.getInstance(getContext());
        tracksTable = Tracks.getInstance(getContext());

        playlists = playlistsTable.getAll();

        if (playlists != null) {
            RecyclerView rvPlaylists = (RecyclerView) layout.findViewById(R.id.rvPlaylists);
            rvPlaylists.setLayoutManager(new LinearLayoutManager(getActivity()));
            itsAdapter = new ITSAdapter(playlists, this);
            rvPlaylists.setAdapter(itsAdapter);

            layout.findViewById(R.id.llNoPlaylistsFound).setVisibility(View.GONE);
        } else {

            //showing no tracks downloaded text view.
            layout.findViewById(R.id.llNoPlaylistsFound).setVisibility(View.VISIBLE);
            layout.findViewById(R.id.bOpenSoundCloud).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openSoundCloud();
                }
            });
        }


        return layout;
    }


    @Override
    public void onRowClicked(int position, View popUpAnchor) {
        final Playlist playlist = playlists.get(position);
        if (playlist.getTotalTracks() > 0) {
            final Intent playlistTracksIntent = new Intent(getActivity(), PlaylistTracksActivity.class);
            playlistTracksIntent.putExtra(Playlist.KEY, playlist);
            startActivity(playlistTracksIntent);
        } else {
            Toast.makeText(getActivity(), R.string.Empty_playlist, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPopUpMenuClicked(View anchor, int position) {

        currentPlaylist = playlists.get(position);
        currentPosition = position;

        final PopupMenu playlistMenu = new PopupMenu(getActivity(), anchor);
        playlistMenu.getMenuInflater().inflate(currentPlaylist.isDownloaded() ? R.menu.menu_playlist_downloaded : R.menu.menu_playlist_not_downloaded, playlistMenu.getMenu());
        playlistMenu.setOnMenuItemClickListener(this);

        playlistMenu.show();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.miShareSavedTracks:
            case R.id.miShareTracks:

                List<Track> trackList = currentPlaylist.getTracks();

                if (trackList == null) {
                    trackList = tracksTable.getAll(currentPlaylist.getId());
                    currentPlaylist.setTracks(trackList);
                }


                final ArrayList<Uri> existingTracks = new ArrayList<>();
                for (final Track track : trackList) {
                    if (track.getFile() != null && track.getFile().exists()) {
                        existingTracks.add(Uri.fromFile(track.getFile()));
                    }
                }

                final Intent shareTracksIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                shareTracksIntent.putExtra(Intent.EXTRA_TEXT, String.format("Downloaded via SoundCloud Downloader (%s)  Playlist: %s ", App.STORE_URL, currentPlaylist.getTitle())); //TOOD: Modify sub
                shareTracksIntent.setType("audio/*");
                shareTracksIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, existingTracks);
                startActivity(shareTracksIntent);

                Toast.makeText(getActivity(), getResources().getQuantityString(R.plurals.sharing_d_tracks, existingTracks.size(), existingTracks.size()), Toast.LENGTH_SHORT).show();

                return true;

            case R.id.miSharePlaylistURL:

                final Intent sharePlaylistUrlIntent = new Intent(Intent.ACTION_SEND);
                sharePlaylistUrlIntent.setType("text/plain");
                sharePlaylistUrlIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.Download_track_using_soundcloud_downloader_s, App.STORE_URL, currentPlaylist.getTitle(), currentPlaylist.getSoundCloudUrl()));
                startActivity(Intent.createChooser(sharePlaylistUrlIntent, getString(R.string.Share_using)));

                return true;


            case R.id.miDownloadPlaylist:

                //Launching downloader service
                final Intent downloadIntent = new Intent(getActivity(), DownloaderService.class);
                downloadIntent.putExtra(Tracks.COLUMN_SOUNDCLOUD_URL, currentPlaylist.getSoundCloudUrl());

                getActivity().startService(downloadIntent);

                Toast.makeText(getActivity(), R.string.initializing_download, Toast.LENGTH_SHORT).show();


                return true;


            case R.id.miRemovePlaylist:

                //Showing confirmation
                ThemedSnackbar.make(getActivity(), getActivity().findViewById(android.R.id.content), R.string.Tracks_under_this, Snackbar.LENGTH_LONG)
                        .setAction(R.string.CONTINUE, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Deleting currentPlaylist
                                playlistsTable.delete(Playlists.COLUMN_ID, currentPlaylist.getId());
                                playlists.remove(currentPosition);
                                itsAdapter.notifyItemRemoved(currentPosition);

                                if (playlists.isEmpty()) {
                                    //showing no tracks downloaded text view.
                                    layout.findViewById(R.id.llNoPlaylistsFound).setVisibility(View.VISIBLE);
                                }

                            }
                        })
                        .show();

                return true;

            default:
                return false;
        }
    }

    @Override
    public void onTrackDownloaded(Track track) {

        if (track.getPlaylistId() == null) {
            Log.w(X, "Track is not from a currentPlaylist");
            return;
        }

        //Finding the updated track
        for (int i = 0; i < playlists.size(); i++) {

            final Playlist playlist = playlists.get(i);

            if (playlist.getId().equals(track.getPlaylistId())) {
                Log.d(X, "Found updated currentPlaylist : " + playlist);

                playlists.remove(i);
                playlists.add(i, playlistsTable.get(Playlists.COLUMN_ID, playlist.getId()));
                itsAdapter.notifyItemChanged(i);
                return;
            }
        }


        throw new IllegalArgumentException("Couldn't find the currentPlaylist");

    }
}
