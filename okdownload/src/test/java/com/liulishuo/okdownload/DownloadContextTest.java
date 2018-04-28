/*
 * Copyright (c) 2017 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liulishuo.okdownload;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;

import com.liulishuo.okdownload.core.Util;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.listener.DownloadListenerBunch;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadContextTest {

    @Mock private DownloadListener listener;
    @Mock private DownloadContextListener queueListener;

    private DownloadContext context;
    private DownloadTask[] tasks;

    private DownloadContext.Builder builder;
    private DownloadContext.QueueSet queueSet;

    private String filePath = "./exist-file";

    @BeforeClass
    public static void setupClass() {
        Util.setLogger(mock(Util.Logger.class));
    }

    @Before
    public void setup() {
        initMocks(this);

        tasks = new DownloadTask[3];
        tasks[0] = mock(DownloadTask.class);
        tasks[1] = mock(DownloadTask.class);
        tasks[2] = mock(DownloadTask.class);

        queueSet = new DownloadContext.QueueSet();
        context = spy(new DownloadContext(tasks, queueListener, queueSet));
        builder = spy(new DownloadContext.Builder(queueSet));
    }

    @After
    public void tearDown() {
        final File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void startOnSerial() {
        doNothing().when(context).start(eq(listener), anyBoolean());
        context.startOnSerial(listener);
        verify(context).start(eq(listener), eq(true));
    }

    @Test
    public void startOnParallel() {
        doNothing().when(context).start(eq(listener), anyBoolean());
        context.startOnParallel(listener);
        verify(context).start(eq(listener), eq(false));
    }

    @Test
    public void start_serialNonStartedCallbackToUI() throws IOException {
        mockOkDownload();

        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = spy(new DownloadTask.Builder("url1", "path", "filename1").build());
        tasks[1] = spy(new DownloadTask.Builder("url2", "path", "filename1").build());
        DownloadTask task = tasks[0];
        when(task.isAutoCallbackToUIThread()).thenReturn(true);
        task = tasks[1];
        when(task.isAutoCallbackToUIThread()).thenReturn(false);

        final Handler handler = mock(Handler.class);
        when(handler.post(any(Runnable.class))).thenAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        });

        context = spy(new DownloadContext(tasks, queueListener, queueSet, handler));
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(context).executeOnSerialExecutor(any(Runnable.class));
        doNothing().when(tasks[0]).execute(any(DownloadListener.class));
        doNothing().when(tasks[1]).execute(any(DownloadListener.class));
        when(context.isStarted()).thenReturn(false);
        context.start(listener, true);

        verify(context).executeOnSerialExecutor(any(Runnable.class));
        verify(tasks[0], never()).execute(any(DownloadListener.class));
        verify(tasks[1], never()).execute(any(DownloadListener.class));
        verify(handler).post(any(Runnable.class));
        verify(queueListener).queueEnd(eq(context));
    }

    @Test
    public void start_serialNonStartedCallbackDirectly() throws IOException {
        mockOkDownload();

        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = spy(new DownloadTask.Builder("url1", "path", "filename1").build());
        tasks[1] = spy(new DownloadTask.Builder("url2", "path", "filename1").build());
        DownloadTask task = tasks[0];
        when(task.isAutoCallbackToUIThread()).thenReturn(true);
        task = tasks[1];
        when(task.isAutoCallbackToUIThread()).thenReturn(false);

        final Handler handler = mock(Handler.class);
        when(handler.post(any(Runnable.class))).thenAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        });

        context = spy(new DownloadContext(tasks, queueListener, queueSet, handler));
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(context).executeOnSerialExecutor(any(Runnable.class));
        doNothing().when(tasks[0]).execute(any(DownloadListener.class));
        doNothing().when(tasks[1]).execute(any(DownloadListener.class));

        when(context.isStarted()).thenReturn(true, false);
        context.start(listener, true);
        verify(context).executeOnSerialExecutor(any(Runnable.class));
        verify(tasks[0]).execute(any(DownloadListener.class));
        verify(tasks[1], never()).execute(any(DownloadListener.class));
        verify(handler, never()).post(any(Runnable.class));
        verify(queueListener).queueEnd(eq(context));
    }

    @Test
    public void start_withoutQueueListener() throws IOException {
        mockOkDownload();

        // without queue listener
        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = spy(new DownloadTask.Builder("url1", "path", "filename1").build());
        tasks[1] = spy(new DownloadTask.Builder("url2", "path", "filename1").build());

        context = spy(new DownloadContext(tasks, null, queueSet));
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) {
                final Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }
        }).when(context).executeOnSerialExecutor(any(Runnable.class));
        doNothing().when(tasks[0]).execute(any(DownloadListener.class));
        doNothing().when(tasks[1]).execute(any(DownloadListener.class));

        assertThat(context.isStarted()).isFalse();
        context.start(listener, true);
        verify(context).executeOnSerialExecutor(any(Runnable.class));
        verify(tasks[0]).execute(any(DownloadListener.class));
        verify(tasks[1]).execute(any(DownloadListener.class));

        assertThat(context.isStarted()).isTrue();

        context.start(listener, false);

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        verify(dispatcher).enqueue(tasks);

        assertThat(tasks[0].getListener()).isEqualTo(listener);
        assertThat(tasks[1].getListener()).isEqualTo(listener);
    }

    @Test
    public void start_withQueueListener() throws IOException {
        mockOkDownload();

        // with queue listener
        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = new DownloadTask.Builder("url1", "path", "filename1").build();
        tasks[1] = new DownloadTask.Builder("url2", "path", "filename1").build();

        context = spy(new DownloadContext(tasks, queueListener, queueSet));
        doNothing().when(context).executeOnSerialExecutor(any(Runnable.class));
        context.start(listener, false);

        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();
        verify(dispatcher).enqueue(tasks);

        assertThat(tasks[0].getListener()).isEqualTo(tasks[1].getListener());
        final DownloadListener taskListener = tasks[0].getListener();
        assertThat(taskListener).isExactlyInstanceOf(DownloadListenerBunch.class);
        assertThat(((DownloadListenerBunch) taskListener).contain(listener)).isTrue();
        // provided listener must callback before queue-attached-listener.
        assertThat(((DownloadListenerBunch) taskListener).indexOf(listener)).isZero();
    }

    @Test
    public void stop() {
        context.isStarted = true;
        context.stop();
        assertThat(context.isStarted()).isFalse();

        verify(OkDownload.with().downloadDispatcher()).cancel(tasks);
    }

    @Test
    public void builder_bindSetTask() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        builder.bindSetTask(mockTask);
        assertThat(builder.boundTaskList).containsExactly(mockTask);
    }

    @Test
    public void setListener() {
        final DownloadContextListener listener = mock(DownloadContextListener.class);
        builder.setListener(listener);
        final DownloadContext context = builder.build();
        assertThat(context.contextListener).isEqualTo(listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void builder_bind_noSetUri() {
        builder.bind("url");
    }

    @Test
    public void builder_bind() throws IOException {
        mockOkDownload();

        final String url = "url";
        final Uri uri = mock(Uri.class);
        when(uri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
        when(uri.getPath()).thenReturn("");
        queueSet.setParentPathUri(uri);
        assertThat(queueSet.getDirUri()).isEqualTo(uri);
        builder.bind(url);
        final DownloadTask addedTask = builder.boundTaskList.get(0);
        assertThat(addedTask.getUrl()).isEqualTo(url);
        assertThat(addedTask.getUri()).isEqualTo(uri);

        final DownloadTask.Builder taskBuilder = mock(DownloadTask.Builder.class);

        final HashMap headerMap = mock(HashMap.class);
        queueSet.setHeaderMapFields(headerMap);
        builder.bind(taskBuilder);
        verify(taskBuilder).setHeaderMapFields(headerMap);

        final int readBufferSize = 1;
        queueSet.setReadBufferSize(readBufferSize);
        assertThat(queueSet.getReadBufferSize()).isEqualTo(readBufferSize);
        builder.bind(taskBuilder);
        verify(taskBuilder).setReadBufferSize(eq(readBufferSize));

        final int flushBufferSize = 2;
        queueSet.setFlushBufferSize(flushBufferSize);
        assertThat(queueSet.getFlushBufferSize()).isEqualTo(flushBufferSize);
        builder.bind(taskBuilder);
        verify(taskBuilder).setFlushBufferSize(eq(flushBufferSize));

        final int syncBufferSize = 3;
        queueSet.setSyncBufferSize(syncBufferSize);
        assertThat(queueSet.getSyncBufferSize()).isEqualTo(syncBufferSize);
        builder.bind(taskBuilder);
        verify(taskBuilder).setSyncBufferSize(eq(syncBufferSize));

        final int syncBufferIntervalMillis = 4;
        queueSet.setSyncBufferIntervalMillis(syncBufferIntervalMillis);
        assertThat(queueSet.getSyncBufferIntervalMillis()).isEqualTo(syncBufferIntervalMillis);
        builder.bind(taskBuilder);
        verify(taskBuilder).setSyncBufferIntervalMillis(eq(syncBufferIntervalMillis));

        final boolean autoCallbackToUIThread = false;
        queueSet.setAutoCallbackToUIThread(autoCallbackToUIThread);
        assertThat(queueSet.getAutoCallbackToUIThread()).isEqualTo(autoCallbackToUIThread);
        builder.bind(taskBuilder);
        verify(taskBuilder).setAutoCallbackToUIThread(eq(autoCallbackToUIThread));

        final int minIntervalMillisCallbackProgress = 5;
        queueSet.setMinIntervalMillisCallbackProcess(minIntervalMillisCallbackProgress);
        assertThat(queueSet.getMinIntervalMillisCallbackProcess())
                .isEqualTo(minIntervalMillisCallbackProgress);
        builder.bind(taskBuilder);
        verify(taskBuilder)
                .setMinIntervalMillisCallbackProcess(eq(minIntervalMillisCallbackProgress));

        final Object tag = mock(Object.class);
        queueSet.setTag(tag);
        assertThat(queueSet.getTag()).isEqualTo(tag);
        final DownloadTask task = mock(DownloadTask.class);
        doReturn(task).when(taskBuilder).build();
        builder.bind(taskBuilder);
        verify(task).setTag(tag);

        queueSet.setPassIfAlreadyCompleted(true);
        assertThat(queueSet.isPassIfAlreadyCompleted()).isTrue();
        builder.bind(taskBuilder);
        verify(taskBuilder).setPassIfAlreadyCompleted(eq(true));

        queueSet.setWifiRequired(true);
        assertThat(queueSet.isWifiRequired()).isTrue();
        builder.bind(taskBuilder);
        verify(taskBuilder).setWifiRequired(eq(true));

        queueSet.commit();
    }

    @Test(expected = IllegalArgumentException.class)
    public void setParentFile() throws IOException {
        final String parentPath = "./parent";
        final File parentPathFile = new File(parentPath);

        queueSet.setParentPath(parentPath);
        assertThat(queueSet.getDirUri().getPath()).isEqualTo(parentPathFile.getAbsolutePath());

        queueSet.setParentPathFile(parentPathFile);
        assertThat(queueSet.getDirUri().getPath()).isEqualTo(parentPathFile.getAbsolutePath());

        File file = new File(filePath);
        file.createNewFile();
        queueSet.setParentPathFile(file);
    }

    @Test
    public void unbind() {
        final DownloadTask mockTask = mock(DownloadTask.class);
        final int id = 1;
        when(mockTask.getId()).thenReturn(id);
        builder.boundTaskList.add(mockTask);
        builder.unbind(id);
        assertThat(builder.boundTaskList).isEmpty();

        builder.boundTaskList.add(mockTask);
        builder.unbind(mockTask);
        assertThat(builder.boundTaskList).isEmpty();
    }

    @Test
    public void replaceTask() {
        final DownloadTask oldTask = tasks[0];
        final DownloadTask newTask = mock(DownloadTask.class);

        context.alter().replaceTask(oldTask, newTask);

        assertThat(tasks[0]).isEqualTo(newTask);
    }

    @Test
    public void queueAttachListener() {
        final DownloadContextListener contextListener = mock(DownloadContextListener.class);
        final DownloadContext.QueueAttachListener attachListener =
                new DownloadContext.QueueAttachListener(context, contextListener, 2);

        final DownloadTask task1 = mock(DownloadTask.class);
        final DownloadTask task2 = mock(DownloadTask.class);
        attachListener.taskStart(task1);
        attachListener.taskStart(task2);

        final EndCause endCause = mock(EndCause.class);
        final Exception realCause = mock(Exception.class);

        attachListener.taskEnd(task1, endCause, realCause);
        verify(contextListener).taskEnd(eq(context), eq(task1), eq(endCause), eq(realCause), eq(1));
        verify(contextListener, never()).queueEnd(eq(context));

        attachListener.taskEnd(task2, endCause, realCause);
        verify(contextListener).taskEnd(eq(context), eq(task2), eq(endCause), eq(realCause), eq(0));
        verify(contextListener).queueEnd(eq(context));
    }
}