package com.cleanyourtv.launcher;

/** A video from YouTube's most-popular chart. */
final class YouTubeVideo {
    final String id;
    final String title;
    final String channel;
    final String thumbnail;

    YouTubeVideo(String id, String title, String channel, String thumbnail) {
        this.id = id;
        this.title = title;
        this.channel = channel;
        this.thumbnail = thumbnail;
    }
}
