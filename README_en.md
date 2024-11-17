# TGVideoCompress

[中文](https://github.com/viifo/TGVideoCompress/blob/master/README.md) | [English](https://github.com/viifo/TGVideoCompress/blob/master/README_en.md)


A video compression library based on Telegram for Android 11.1.3 (5244), extracted from the [**Telegram for Android**](https://github.com/DrKLO/Telegram) [![Telegram for Android](https://raw.githubusercontent.com/lalongooo/VideoCompressor/master/images/ic_launcher.png)](https://github.com/DrKLO/Telegram) source code.


![](./screenshots/preview.gif)


## Usage

```java
// TG default compression strategy
VideoEditedInfo info = MediaController.makeTgVideoEditedInfo(path);
// Start the task, multiple tasks will be queued and executed in sequence
MediaController.getInstance().scheduleVideoConvert(info);
// Progress callback
MediaController.getInstance().setListener(new OnProgressListener() {
    @Override
    public void progress(VideoEditedInfo info, float progress) {

    }

    @Override
    public void success(VideoEditedInfo info) {

    }

    @Override
    public void error(VideoEditedInfo info) {
        // Error or task cancelled
    }
});

// Cancel a task
MediaController.getInstance().cancelVideoConvert(info);
```


## License

```
Copyright 2024 viifo

This program is free software; you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
```

