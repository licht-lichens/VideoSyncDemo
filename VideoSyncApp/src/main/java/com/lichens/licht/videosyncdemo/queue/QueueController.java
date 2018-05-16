package com.lichens.licht.videosyncdemo.queue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.SeekBar;

import com.lichens.licht.videosyncdemo.R;
import com.lichens.licht.videosyncdemo.utils.DemoUtil;
import com.liulishuo.okdownload.DownloadContext;
import com.liulishuo.okdownload.DownloadContextListener;
import com.liulishuo.okdownload.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueueController {
    private static final String TAG = "QueueController";
    private List<DownloadTask> taskList = new ArrayList<>();
    private DownloadContext context;
    private final QueueListener listener = new QueueListener();

    private File queueDir;

    public void initTasks(@NonNull Context context, @NonNull DownloadContextListener listener) {
        final DownloadContext.QueueSet set = new DownloadContext.QueueSet();
        final File parentFile = new File(DemoUtil.getParentFile(context), "queue");
        this.queueDir = parentFile;

        set.setParentPathFile(parentFile);
        set.setMinIntervalMillisCallbackProcess(200);

        final DownloadContext.Builder builder = set.commit();

        String url = "http://192.168.3.116:8080/DataServer/notad.mp4";
        DownloadTask boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "1. WeChat");

        url = "http://192.168.3.116:8080/DataServer/15.mp4";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "2. LiuLiShuo");

        url = "http://192.168.3.116:8080/DataServer/4K.mp4";
        boundTask = builder.bind(url);
        TagUtil.saveTaskName(boundTask, "3. Alipay");

        builder.setListener(listener);

        this.context = builder.build();
        this.taskList = Arrays.asList(this.context.getTasks());
    }

    public void deleteFiles() {
        if (queueDir != null) {
            String[] children = queueDir.list();
            if (children != null) {
                for (String child : children) {
                    new File(queueDir, child).delete();
                }
            }

            queueDir.delete();
        }

        for (DownloadTask task : taskList) {
            TagUtil.clearProceedTask(task);
        }
    }

    public void setPriority(DownloadTask task, int priority) {
        final DownloadTask newTask = task.toBuilder().setPriority(priority).build();
        this.context = context.toBuilder()
                .bindSetTask(newTask)
                .build();
        newTask.setTags(task);
        TagUtil.savePriority(newTask, priority);
        this.taskList = Arrays.asList(this.context.getTasks());
    }

    public void start(boolean isSerial) {
        this.context.start(listener, isSerial);
    }

    public void stop() {
        if (this.context.isStarted()) {
            this.context.stop();
        }
    }

    void bind(final QueueRecyclerAdapter.QueueViewHolder holder, int position) {
        final DownloadTask task = taskList.get(position);
        Log.d(TAG, "bind " + position + " for " + task.getUrl());

        listener.bind(task, holder);
        listener.resetInfo(task, holder);

        // priority
        final int priority = TagUtil.getPriority(task);
        holder.priorityTv
                .setText(holder.priorityTv.getContext().getString(R.string.priority, priority));
        holder.prioritySb.setProgress(priority);
        if (this.context.isStarted()) {
            holder.prioritySb.setEnabled(false);
        } else {
            holder.prioritySb.setEnabled(true);
            holder.prioritySb.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        boolean isFromUser;

                        @Override public void onProgressChanged(SeekBar seekBar, int progress,
                                                                boolean fromUser) {
                            isFromUser = fromUser;
                        }

                        @Override public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override public void onStopTrackingTouch(SeekBar seekBar) {
                            if (isFromUser) {
                                final int priority = seekBar.getProgress();
                                setPriority(task, priority);
                                holder.priorityTv
                                        .setText(seekBar.getContext()
                                                .getString(R.string.priority, priority));
                            }
                        }
                    });
        }
    }

    int size() {
        return taskList.size();
    }
}