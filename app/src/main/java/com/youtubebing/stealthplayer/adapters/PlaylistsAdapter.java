package com.youtubebing.stealthplayer.adapters;

/**
 * Created by smedic on 6.2.17..
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.youtubebing.stealthplayer.R;
import com.youtubebing.stealthplayer.interfaces.ItemEventsListener;
import com.youtubebing.stealthplayer.model.VideoItem;
import com.youtubebing.stealthplayer.model.YouTubePlaylist;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Custom array adapter class
 */
public class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>
        implements View.OnClickListener {

    private Context context;
    private List<VideoItem> playlists;
    private ItemEventsListener<VideoItem> itemEventsListener;

    public PlaylistsAdapter(Context context, List<VideoItem> playlists) {
        super();
        this.context = context;
        this.playlists = playlists;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, null);
        v.setOnClickListener(this);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final VideoItem playlist = playlists.get(position);
        Picasso.with(context).load(playlist.getThumbnailUrl()).into(holder.thumbnail);
        holder.title.setText(playlist.getTitle());
        String videosNumberText = context.getString(R.string.number_of_videos) + String.valueOf(playlist.getViewCount());
        holder.videosNumber.setText(videosNumberText);

        holder.garbageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemEventsListener != null) {
                    itemEventsListener.onGarbageClicked(playlist);
                }
            }
        });
        holder.itemView.setTag(playlist);
    }

    @Override
    public int getItemCount() {
        return (null != playlists ? playlists.size() : 0);
    }

    @Override
    public void onClick(View v) {
        if (itemEventsListener != null) {
            VideoItem item = (VideoItem) v.getTag();
            itemEventsListener.onItemClick(item);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView videosNumber;
        TextView privacy;
        ImageView garbageButton;

        public ViewHolder(View itemView) {
            super(itemView);
            thumbnail = (ImageView) itemView.findViewById(R.id.video_thumbnail);
            title = (TextView) itemView.findViewById(R.id.playlist_title);
            videosNumber = (TextView) itemView.findViewById(R.id.videos_number);
            garbageButton = (ImageView) itemView.findViewById(R.id.garbage_button);
        }
    }

    public void setOnItemEventsListener(ItemEventsListener<VideoItem> listener) {
        itemEventsListener = listener;
    }
}