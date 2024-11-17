package com.viifo.tgvideocompress;

public interface OnProgressListener {

    void progress(VideoEditedInfo info, float progress);

    void success(VideoEditedInfo info);

    /**
     * 处理出错或被取消
     */
    void error(VideoEditedInfo info);
}
