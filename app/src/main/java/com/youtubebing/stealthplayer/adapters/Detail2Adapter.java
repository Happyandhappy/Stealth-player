package com.youtubebing.stealthplayer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.youtubebing.stealthplayer.R;
import com.youtubebing.stealthplayer.interfaces.DetailItemEventListener;
import com.youtubebing.stealthplayer.model.VideoItem;

import java.util.List;

/**
 * Created by dell on 7/29/2017.
 */

public class Detail2Adapter extends RecyclerView.Adapter<Detail2Adapter.CustomHolder>
        implements View.OnClickListener{

    private List<VideoItem> detailList;
    private Context context;
    private DetailItemEventListener<VideoItem> itemEventsListener;

    public Detail2Adapter(Context context, List<VideoItem> list){
        this.context = context;
        detailList = list;
    }

    @Override
    public CustomHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_item, null);
        view.setOnClickListener(this);
        CustomHolder customHolder = new CustomHolder(view);
        return customHolder;
    }

    @Override
    public void onBindViewHolder(CustomHolder holder, int position) {
        VideoItem item = detailList.get(position);
        if (!TextUtils.isEmpty(item.getThumbnailUrl())) {
            Picasso.with(context).load(item.getThumbnailUrl())
                    .error(R.drawable.ic_action_playlist)
                    .placeholder(R.drawable.ic_action_playlist)
                    .into(holder.thumbnailView);
        }

        holder.titleView.setText(item.getTitle());
        holder.itemView.setTag(item);

    }

    @Override
    public int getItemCount() {
        return (null != detailList ? detailList.size() : 0);
    }

    @Override
    public void onClick(View v) {
        if (itemEventsListener != null) {
            VideoItem item = (VideoItem) v.getTag();
            itemEventsListener.onItemClick2(item);
        }
    }

    public class CustomHolder extends RecyclerView.ViewHolder{

        private ImageView thumbnailView;
        private TextView titleView;

        public CustomHolder(View itemView) {
            super(itemView);
            thumbnailView = (ImageView)itemView.findViewById(R.id.detail_thumbnail);
            titleView = (TextView)itemView.findViewById(R.id.detail_title);
        }
    }

    public void setOnItemEventsListener(DetailItemEventListener<VideoItem> listener) {
        itemEventsListener = listener;
    }

}
