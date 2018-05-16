package com.lichens.licht.videosyncdemo.base;

import java.util.List;

/**
 * Created by licht on 2018/5/15.
 */

public class mqttData {

    /**
     * server_video : {"video_list":[{"file_name":"xxxx.mp4"},{"file_name":"xxxx.mp4"}]}
     */

    public ServerVideoBean server_video;

    public static class ServerVideoBean {
        public List<VideoListBean> video_list;

        public static class VideoListBean {
            /**
             * file_name : xxxx.mp4
             */

            public String file_name;
        }
    }
}
