/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package com.viifo.tgvideocompress;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.viifo.tgvideocompress.utils.Utilities;

import java.io.File;
import java.util.ArrayList;

public class MediaController {

    public final static String VIDEO_MIME_TYPE = "video/avc";
    public final static String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private final Object videoConvertSync = new Object();
    private static volatile MediaController Instance;
    private ArrayList<VideoConvertMessage> videoConvertQueue = new ArrayList<>();
    private OnProgressListener listener = null;

    public static MediaController getInstance() {
        MediaController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MediaController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MediaController();
                }
            }
        }
        return localInstance;
    }

    public void scheduleVideoConvert(VideoEditedInfo info) {
        scheduleVideoConvert(info, false);
    }

    public boolean scheduleVideoConvert(VideoEditedInfo info, boolean isEmpty) {
        if (info == null) {
            return false;
        }
        if (isEmpty && !videoConvertQueue.isEmpty()) {
            return false;
        } else if (isEmpty) {
            new File(info.resultPath).delete();
        }
        VideoConvertMessage videoConvertMessage = new VideoConvertMessage(info, false, false);
        videoConvertQueue.add(videoConvertMessage);
        if (videoConvertQueue.size() == 1) {
            // 第一个视频，直接开始压缩
            startVideoConvertFromQueue();
        }
        return true;
    }

    public void cancelVideoConvert(VideoEditedInfo info) {
        if (info != null) {
            if (!videoConvertQueue.isEmpty()) {
                for (int a = 0; a < videoConvertQueue.size(); a++) {
                    VideoConvertMessage videoConvertMessage = videoConvertQueue.get(a);
                    VideoEditedInfo object = videoConvertMessage.videoEditedInfo;
                    if (object.equals(info) && object.originalPath != null && object.originalPath.equals(info.originalPath)) {
                        if (a == 0) {
                            synchronized (videoConvertSync) {
                                videoConvertMessage.videoEditedInfo.canceled = true;
                            }
                        } else {
                            videoConvertQueue.remove(a);
                        }
                        break;
                    }
                }
            }
        }
    }

    private boolean startVideoConvertFromQueue() {
        if (!videoConvertQueue.isEmpty()) {
            VideoConvertMessage videoConvertMessage = videoConvertQueue.get(0);
            VideoEditedInfo videoEditedInfo = videoConvertMessage.videoEditedInfo;
            synchronized (videoConvertSync) {
                if (videoEditedInfo != null) {
                    videoEditedInfo.canceled = false;
                }
            }
            VideoConvertRunnable.runConversion(videoConvertMessage);
            return true;
        }
        return false;
    }

    private static class VideoConvertRunnable implements Runnable {

        private VideoConvertMessage convertMessage;

        private VideoConvertRunnable(VideoConvertMessage message) {
            convertMessage = message;
        }

        @Override
        public void run() {
            MediaController.getInstance().convertVideo(convertMessage);
        }

        public static void runConversion(final VideoConvertMessage obj) {
            new Thread(() -> {
                try {
                    VideoConvertRunnable wrapper = new VideoConvertRunnable(obj);
                    Thread th = new Thread(wrapper, "VideoConvertRunnable");
                    th.start();
                    th.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public boolean convertVideo(final VideoConvertMessage convertMessage) {
        VideoEditedInfo info = convertMessage.videoEditedInfo;
        if (/*messageObject == null ||*/ info == null) {
            return false;
        }
        String videoPath = info.originalPath;
        long startTime = info.startTime;
        long avatarStartTime = info.avatarStartTime;
        long endTime = info.endTime;
        int resultWidth = info.resultWidth;
        int resultHeight = info.resultHeight;
        int rotationValue = info.rotationValue;
        int originalWidth = info.originalWidth;
        int originalHeight = info.originalHeight;
        int framerate = info.framerate;
        int bitrate = info.bitrate;
        int originalBitrate = info.originalBitrate;
        boolean isSecret = info.forceFragmenting;
        // 压缩目标路径
        final File cacheFile = new File(info.resultPath);
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        if (videoPath == null) {
            videoPath = "";
        }

        long duration;
        if (startTime > 0 && endTime > 0) {
            duration = endTime - startTime;
        } else if (endTime > 0) {
            duration = endTime;
        } else if (startTime > 0) {
            duration = info.originalDuration - startTime;
        } else {
            duration = info.originalDuration;
        }

        if (framerate == 0) {
            framerate = 25;
        } else if (framerate > 59) {
            framerate = 59;
        }

        if (rotationValue == 90 || rotationValue == 270) {
            int temp = resultHeight;
            resultHeight = resultWidth;
            resultWidth = temp;
        }
        rotationValue = 0;

        if (!info.shouldLimitFps && framerate > 40 && (Math.min(resultHeight, resultWidth) <= 480)) {
            framerate = 30;
        }

        boolean needCompress = resultWidth != originalWidth || resultHeight != originalHeight;
        VideoConvertorListener callback = new VideoConvertorListener() {

            @Override
            public boolean checkConversionCanceled() {
                return info.canceled;
            }

            @Override
            public void didWriteData(long availableSize, float progress) {
                if (info.canceled) {
                    return;
                }
                if (listener != null) {
                    Utilities.runOnUIThread(() -> listener.progress(info, progress));
                }
            }
        };

        MediaCodecVideoConvertor videoConvertor = new MediaCodecVideoConvertor();
        MediaCodecVideoConvertor.ConvertVideoParams convertVideoParams = MediaCodecVideoConvertor.ConvertVideoParams.of(videoPath, cacheFile,
                rotationValue, isSecret,
                originalWidth, originalHeight,
                resultWidth, resultHeight,
                framerate, bitrate, originalBitrate,
                startTime, endTime, avatarStartTime,
                needCompress, duration,
                callback,
                info);
        convertVideoParams.soundInfos.addAll(info.mixedSoundInfos);
        boolean error = videoConvertor.convertVideo(convertVideoParams);

        boolean canceled = info.canceled;
        if (!canceled) {
            synchronized (videoConvertSync) {
                canceled = info.canceled;
            }
        }

        didWriteData(convertMessage, true, error || canceled);

        return true;
    }

    private void didWriteData(final VideoConvertMessage message, final boolean last, final boolean error) {
       Utilities.runOnUIThread(() -> {
            if (error || last) {
                // 开始下一个任务
                synchronized (videoConvertSync) {
                    message.videoEditedInfo.canceled = false;
                }
                videoConvertQueue.remove(message);
                startVideoConvertFromQueue();
            }
            if (error && listener != null) {
                listener.error(message.videoEditedInfo);
            } else if (listener != null) {
                listener.success(message.videoEditedInfo);
            }
        });
    }

    /**
     * 从视频文件中查找第一个视频轨道或者音频轨道
     * @param extractor - MediaExtractor
     * @param audio - 是否选择音轨
     * @return 轨道索引
     */
    public static int findTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio/")) {
                    return i;
                }
            } else {
                if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -5;
    }

    public interface VideoConvertorListener {
        boolean checkConversionCanceled();
        void didWriteData(long availableSize, float progress);
    }

    public static class VideoConvertMessage {
        public VideoEditedInfo videoEditedInfo;
        public boolean foreground;
        public boolean foregroundConversion;

        public VideoConvertMessage(VideoEditedInfo info, boolean foreground, boolean conversion) {
            videoEditedInfo = info;
            this.foreground = foreground;
            this.foregroundConversion = conversion;
        }
    }

    public static int makeVideoBitrate(int originalHeight, int originalWidth, int originalBitrate, int height, int width) {
        float compressFactor;
        float minCompressFactor;
        int maxBitrate;
        if (Math.min(height, width) >= 1080) {
            maxBitrate = 6800_000;
            compressFactor = 1f;
            minCompressFactor = 1f;
        } else if (Math.min(height, width) >= 720) {
            maxBitrate = 2600_000;
            compressFactor = 1f;
            minCompressFactor = 1f;
        } else if (Math.min(height, width) >= 480) {
            maxBitrate = 1000_000;
            compressFactor = 0.75f;
            minCompressFactor = 0.9f;
        } else {
            maxBitrate = 750_000;
            compressFactor = 0.6f;
            minCompressFactor = 0.7f;
        }
        int remeasuredBitrate = (int) (originalBitrate / (Math.min(originalHeight / (float) (height), originalWidth / (float) (width))));
        remeasuredBitrate = (int) (remeasuredBitrate * compressFactor);
        int minBitrate = (int) (getVideoBitrateWithFactor(minCompressFactor) / (1280f * 720f / (width * height)));
        if (originalBitrate < minBitrate) {
            return remeasuredBitrate;
        }
        if (remeasuredBitrate > maxBitrate) {
            return maxBitrate;
        }
        return Math.max(remeasuredBitrate, minBitrate);
    }

    private static int getVideoBitrateWithFactor(float f) {
        return (int) (f * 2000f * 1000f * 1.13f);
    }

    public static VideoEditedInfo makeTgVideoEditedInfo(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        VideoEditedInfo info = new VideoEditedInfo();
        try {

            retriever.setDataSource(path);
            int bitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)); // Bbps / 1024 = kbps
            int videoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int videoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            int rotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

            float maxSize = (float) Math.max(videoWidth, videoHeight);
            int compressionsCount;
            if (maxSize > 1280f) {
                compressionsCount = 4;
            } else if (maxSize > 854f) {
                compressionsCount = 3;
            } else if (maxSize > 640f) {
                compressionsCount = 2;
            } else {
                compressionsCount = 1;
            }

            switch (compressionsCount) {
                case 1 : {
                    maxSize = 432.0f;
                } break;
                case 2 : {
                    maxSize = 640.0f;
                } break;
                case 3 : {
                    maxSize = 848.0f;
                } break;
                default: {
                    maxSize = 1280.0f;
                }
            }

            info.startTime = -1;
            info.endTime = -1;
            info.rotationValue = rotation;
            info.originalPath = path;
            info.resultPath = path.substring(0, path.length() - 4) + "_compress.mp4";
            info.originalWidth = videoWidth;
            info.originalHeight = videoHeight;

            float scale = 1f;
            if (info.originalWidth > info.originalHeight) {
                scale = maxSize / info.originalWidth;
            } else {
                scale = maxSize / info.originalHeight;
            }
            info.resultWidth = Math.round(info.originalWidth * scale / 2) * 2;
            info.resultHeight = Math.round(info.originalHeight * scale / 2) * 2;

            info.originalBitrate = bitrate;
            info.originalDuration = duration;
            info.bitrate = MediaController.makeVideoBitrate(
                    info.originalHeight, info.originalWidth,
                    info.originalBitrate,
                    info.resultHeight, info.resultWidth
            );
        } catch (Exception e) {
            info = null;
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return info;
    }

    public void setListener(OnProgressListener listener) {
        this.listener = listener;
    }
}