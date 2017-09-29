package com.youtubebing.stealthplayer.model;

import java.io.Serializable;


/**
 * Created by dell on 7/17/2017.
 */

public class VideoItem implements Serializable {
    private String id;
    private String title;
    private String thumbnailUrl;
    private String description;
    private String duration;
    private String viewCount;
    private String videoType;


    public VideoItem(){
        id="";
        title = "";
        thumbnailUrl = "";
        description = "";
        duration = "";
        viewCount = "";
        videoType = "";
    }

    public VideoItem(VideoItem newItem){
        id = newItem.id;
        title = newItem.title;
        thumbnailUrl = newItem.thumbnailUrl;
        description = newItem.description;
        duration = newItem.duration;
        viewCount = newItem.viewCount;
        videoType = newItem.videoType;
    }


    public VideoItem(String id, String title, String thumbnailURL, String duration, String viewCount, String description, String type){
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailURL;
        this.description = description;
        this.duration = duration;
        this.viewCount = viewCount;
        this.videoType = type;
    }

    public String getId(){
        return this.id;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getTitle(){
        return this.title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getDescription(){
        return this.description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getThumbnailUrl(){
        return this.thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl){
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDuration(){
        return this.duration;
    }

    public void setDuration(String duration){
        this.duration = duration;
    }

    public String getViewCount(){
        return this.viewCount;
    }

    public void setViewCount(String viewCount){
        this.viewCount = viewCount;
    }

    public String getVideoType(){
        return this.videoType;
    }

    public void setVideoType(String type){
        this.videoType = type;
    }
}
