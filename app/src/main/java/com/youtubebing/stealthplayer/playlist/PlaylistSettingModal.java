package com.youtubebing.stealthplayer.playlist;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.youtubebing.stealthplayer.R;
import com.youtubebing.stealthplayer.database.YouTubeSqlDb;
import com.youtubebing.stealthplayer.model.VideoItem;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSettingModal extends DialogFragment  implements AdapterView.OnItemSelectedListener {

    private Button okBtn;
    private Button cancelBtn;
    private VideoItem videoitem;
    private int playlistId;
    private String playlistName;
    private Spinner spinner;

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        playlistName = spinner.getItemAtPosition(position).toString();
        playlistId = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    public interface PlaylistDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    PlaylistDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (PlaylistDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PlaylistDialogListener");
        }
    }

    // Empty constructor required for DialogFragment
    public PlaylistSettingModal() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.playlist_modal, container);

        playlistId = 0;

        spinner = (Spinner) view.findViewById(R.id.playlist_spinner);

        List<String> categories = new ArrayList<String>();

        categories.addAll(YouTubeSqlDb.getInstance().getSpinnerLists().readAll());

        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Spinner Drop down elements



        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        okBtn = (Button)view.findViewById(R.id.playlist_ok_btn);
        cancelBtn = (Button)view.findViewById(R.id.playlist_cancel_btn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDialogPositiveClick(PlaylistSettingModal.this);
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDialogNegativeClick(PlaylistSettingModal.this);
            }
        });

        // set this instance as callback for editor action
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        getDialog().setTitle(R.string.playlist_modal_title);

        return view;
    }

    public int getPlaylistId(){
        return playlistId;
    }

    public String getPlaylistName(){
        return playlistName;
    }


    public VideoItem getVideoitem(){
        return videoitem;
    }

    public void setVideoitem(VideoItem videoitem){
        this.videoitem = videoitem;
    }

}