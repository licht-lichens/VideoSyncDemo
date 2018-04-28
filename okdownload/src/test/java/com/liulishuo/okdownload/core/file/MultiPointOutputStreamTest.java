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

package com.liulishuo.okdownload.core.file;

import android.content.Context;
import android.net.Uri;

import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.core.breakpoint.BlockInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.DownloadStore;
import com.liulishuo.okdownload.core.exception.PreAllocateException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class) // for sparseArray.
@Config(manifest = NONE)
public class MultiPointOutputStreamTest {

    private MultiPointOutputStream multiPointOutputStream;

    private final String parentPath = "./p-path/";
    private final File existFile = new File("./p-path/filename");
    @Mock private BreakpointInfo info;
    @Mock private DownloadTask task;
    @Mock private DownloadStore store;
    @Mock private Runnable syncRunnable;

    @BeforeClass
    public static void setupClass() throws IOException {
        mockOkDownload();
    }

    @Before
    public void setup() {
        when(OkDownload.with().context()).thenReturn(application);
        initMocks(this);
        when(task.getFile()).thenReturn(existFile);
        when(task.getParentFile()).thenReturn(new File(parentPath));
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info, store, syncRunnable));
        doNothing().when(multiPointOutputStream).executeSyncRunnableAsync();
    }

    @After
    public void tearDown() {
        final File file = existFile;
        if (file.exists()) {
            file.delete();
        }
        if (file.getParentFile().exists()) {
            file.getParentFile().delete();
        }
    }

    @Test
    public void write() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(anyInt());
        multiPointOutputStream.syncRunning = true;

        final byte[] bytes = new byte[6];
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong());
        multiPointOutputStream.write(1, bytes, 6);

        verify(multiPointOutputStream).write(1, bytes, 6);

        multiPointOutputStream.noSyncLengthMap.put(2, new AtomicLong());
        multiPointOutputStream.write(2, bytes, 16);
        verify(multiPointOutputStream).write(2, bytes, 16);

        assertThat(multiPointOutputStream.allNoSyncLength.get()).isEqualTo(22);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isEqualTo(6);
        assertThat(multiPointOutputStream.noSyncLengthMap.get(2).get()).isEqualTo(16);
    }

    @Test
    public void runSync() throws IOException {
        final DownloadOutputStream outputStream = mock(DownloadOutputStream.class);
        doReturn(outputStream).when(multiPointOutputStream).outputStream(1);
        when(info.getBlock(1)).thenReturn(mock(BlockInfo.class));
        multiPointOutputStream.syncRunning = true;
        final Thread thread = mock(Thread.class);
        multiPointOutputStream.parkForWaitingSyncThread = thread;

        multiPointOutputStream.allNoSyncLength.addAndGet(10);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));
        multiPointOutputStream.outputStreamMap.put(1, mock(DownloadOutputStream.class));

        final ProcessFileStrategy fileStrategy = OkDownload.with().processFileStrategy();
        final FileLock fileLock = mock(FileLock.class);
        when(fileStrategy.getFileLock()).thenReturn(fileLock);

        multiPointOutputStream.runSync();

        verify(store).onSyncToFilesystemSuccess(info, 1, 10);
        verify(fileLock).decreaseLock(eq(existFile.getAbsolutePath()));
        verify(multiPointOutputStream).unparkThread(eq(thread));
        assertThat(multiPointOutputStream.allNoSyncLength.get()).isZero();
        assertThat(multiPointOutputStream.noSyncLengthMap.get(1).get()).isZero();
        assertThat(multiPointOutputStream.syncRunning).isFalse();
    }

    @Test
    public void ensureSyncComplete() throws IOException {
        multiPointOutputStream.syncRunning = true;
        doNothing().when(multiPointOutputStream).parkThread(anyLong());
        doNothing().when(multiPointOutputStream).runSync();
        when(multiPointOutputStream.isSyncRunning()).thenReturn(true, false);
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));

        multiPointOutputStream.ensureSyncComplete(1, false);

        verify(multiPointOutputStream).parkThread(eq(50L));
        verify(multiPointOutputStream).runSync();
    }

    @Test(expected = IOException.class)
    public void ensureSyncComplete_syncException() throws IOException {
        multiPointOutputStream.syncException = new IOException();
        multiPointOutputStream.ensureSyncComplete(1, false);
    }

    @Test
    public void ensureSyncComplete_async() throws IOException {
        multiPointOutputStream.syncRunning = false;
        multiPointOutputStream.noSyncLengthMap.put(1, new AtomicLong(10));

        final ProcessFileStrategy strategy = OkDownload.with().processFileStrategy();
        final FileLock fileLock = mock(FileLock.class);
        when(strategy.getFileLock()).thenReturn(fileLock);

        multiPointOutputStream.ensureSyncComplete(1, true);

        verify(multiPointOutputStream).executeSyncRunnableAsync();
        verify(fileLock).increaseLock(eq(existFile.getAbsolutePath()));
        assertThat(multiPointOutputStream.syncRunning).isTrue();
    }

    @Test
    public void inspectAndPersist() throws IOException {
        multiPointOutputStream.syncRunning = true;
        multiPointOutputStream.inspectAndPersist();

        verify(multiPointOutputStream, never()).executeSyncRunnableAsync();

        multiPointOutputStream.syncRunning = false;
        when(multiPointOutputStream.isNeedPersist()).thenReturn(false);
        multiPointOutputStream.inspectAndPersist();

        verify(multiPointOutputStream, never()).executeSyncRunnableAsync();

        when(multiPointOutputStream.isNeedPersist()).thenReturn(true);
        multiPointOutputStream.inspectAndPersist();

        verify(multiPointOutputStream).executeSyncRunnableAsync();
        assertThat(multiPointOutputStream.syncRunning).isTrue();
    }

    @Test(expected = IOException.class)
    public void inspectComplete_notFull() throws IOException {
        final BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(1)).thenReturn(blockInfo);

        when(blockInfo.getContentLength()).thenReturn(9L);
        when(blockInfo.getCurrentOffset()).thenReturn(10L);

        multiPointOutputStream.inspectComplete(1);
    }

    @Test(expected = IOException.class)
    public void inspectComplete_syncException() throws IOException {
        multiPointOutputStream.syncException = new IOException();
        multiPointOutputStream.inspectAndPersist();
    }

    @Test
    public void outputStream() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(10L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isNull();
        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);

        assertThat(outputStream).isNotNull();
        assertThat(multiPointOutputStream.outputStreamMap.get(0)).isEqualTo(outputStream);
        verify(outputStream).seek(eq(10L));
        verify(outputStream).setLength(eq(20L));
    }

    @Test
    public void outputStream_rangeLeft0_noSeek() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream).setLength(eq(20L));
    }

    @Test
    public void outputStream_chunked_noPreAllocate() throws IOException {
        prepareOutputStreamEnv();

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.isChunked()).thenReturn(true);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream, never()).setLength(anyLong());
    }

    @Test
    public void outputStream_nonFileScheme() throws IOException {
        prepareOutputStreamEnv();

        final Uri uri = task.getUri();
        when(uri.getScheme()).thenReturn("content");

        BlockInfo blockInfo = mock(BlockInfo.class);
        when(info.getBlock(0)).thenReturn(blockInfo);
        when(blockInfo.getRangeLeft()).thenReturn(0L);
        when(info.getTotalLength()).thenReturn(20L);
        when(info.isChunked()).thenReturn(false);

        final DownloadOutputStream outputStream = multiPointOutputStream.outputStream(0);
        verify(outputStream, never()).seek(anyLong());
        verify(outputStream).setLength(eq(20L));
        verify(multiPointOutputStream, never()).inspectFreeSpace(anyString(), anyLong());
    }

    private void prepareOutputStreamEnv() throws FileNotFoundException, PreAllocateException {
        when(OkDownload.with().outputStreamFactory().supportSeek()).thenReturn(true);
        when(OkDownload.with().processFileStrategy().isPreAllocateLength()).thenReturn(true);
        when(OkDownload.with().outputStreamFactory().create(any(Context.class), any(Uri.class),
                anyInt())).thenReturn(mock(DownloadOutputStream.class));
        // recreate for new values of support-seek and pre-allocate-length.
        multiPointOutputStream = spy(new MultiPointOutputStream(task, info, store));
        doNothing().when(multiPointOutputStream).inspectFreeSpace(anyString(), anyLong());

        final Uri uri = mock(Uri.class);
        when(task.getUri()).thenReturn(uri);
        when(uri.getScheme()).thenReturn("file");
    }
}