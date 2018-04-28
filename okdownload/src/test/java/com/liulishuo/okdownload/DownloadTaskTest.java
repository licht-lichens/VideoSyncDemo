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

import com.liulishuo.okdownload.core.IdentifiedTask;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStore;
import com.liulishuo.okdownload.core.breakpoint.BreakpointStoreOnCache;
import com.liulishuo.okdownload.core.dispatcher.DownloadDispatcher;
import com.liulishuo.okdownload.core.download.DownloadStrategy;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.liulishuo.okdownload.TestUtils.assertFile;
import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class DownloadTaskTest {

    @BeforeClass
    public static void setupClass() throws IOException {
        TestUtils.mockOkDownload();
    }

    @Test
    public void addHeader() throws Exception {
        final String url = "mock url";
        final Uri mockFileUri = mock(Uri.class);
        when(mockFileUri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
        when(mockFileUri.getPath()).thenReturn("mock path");
        DownloadTask.Builder builder = new DownloadTask.Builder(url, mockFileUri);


        final String mockKey1 = "mock key1";
        final String mockKey2 = "mock key2";
        final String mockValue1 = "mock value1";
        final String mockValue2 = "mock value2";

        builder.addHeader(mockKey1, mockValue1);
        builder.addHeader(mockKey1, mockValue2);

        builder.addHeader(mockKey2, mockValue2);

        final Map<String, List<String>> headerMap = builder.build().getHeaderMapFields();
        assertThat(headerMap).isNotNull();

        assertThat(headerMap).containsKey(mockKey1).containsKey(mockKey2);

        final List<String> key1Values = headerMap.get(mockKey1);
        assertThat(key1Values).containsOnly(mockValue1, mockValue2);

        final List<String> key2Values = headerMap.get(mockKey2);
        assertThat(key2Values).containsOnly(mockValue2);
    }

    private final String parentPath = "./p-path/";
    private final String filename = "filename";

    @Before
    public void setup() throws IOException {
        new File(parentPath).mkdir();
        new File(parentPath, filename).createNewFile();
    }

    @After
    public void tearDown() {
        new File(parentPath, filename).delete();
        new File(parentPath).delete();
    }


    @Test
    public void enqueue() {
        final DownloadTask[] tasks = new DownloadTask[2];
        tasks[0] = new DownloadTask.Builder("url1", "path", "filename1").build();
        tasks[1] = new DownloadTask.Builder("url2", "path", "filename1").build();

        final DownloadListener listener = mock(DownloadListener.class);
        DownloadTask.enqueue(tasks, listener);

        assertThat(tasks[0].getListener()).isEqualTo(listener);
        assertThat(tasks[1].getListener()).isEqualTo(listener);

        verify(OkDownload.with().downloadDispatcher()).enqueue(eq(tasks));
    }

    @Test
    public void equal() throws IOException {
        // for id
        when(OkDownload.with().breakpointStore()).thenReturn(spy(new BreakpointStoreOnCache()));

        final Uri uri = mock(Uri.class);
        when(uri.getPath()).thenReturn(parentPath);
        when(uri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);

        // origin is:
        // 1. uri is directory
        // 2. filename is provided
        DownloadTask task = new DownloadTask
                .Builder("url", uri)
                .setFilename(filename)
                .build();

        // compare to:
        // 1. uri is not directory
        // 2. filename is provided by uri.
        final Uri anotherUri = mock(Uri.class);
        when(anotherUri.getPath()).thenReturn(parentPath + filename);
        when(anotherUri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
        DownloadTask anotherTask = new DownloadTask
                .Builder("url", anotherUri)
                .build();
        assertThat(task.equals(anotherTask)).isTrue();

        // compare to:
        // 1. uri is directory
        // 2. filename is not provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .build();
        // expect: not same
        assertThat(task.equals(anotherTask)).isFalse();


        // compare to:
        // 1. uri is directory
        // 2. filename is provided and different
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("another-filename")
                .build();
        // expect: not same
        assertThat(task.equals(anotherTask)).isFalse();

        // origin is:
        // 1. uri is directory
        // 2. filename is not provided
        DownloadTask noFilenameTask = new DownloadTask
                .Builder("url", uri)
                .build();

        // filename is enabled by response
        final BreakpointInfo info = mock(BreakpointInfo.class);
        when(info.getFilenameHolder()).thenReturn(mock(DownloadStrategy.FilenameHolder.class));
        new DownloadStrategy().validFilenameFromResponse("response-filename",
                noFilenameTask, info);

        // compare to:
        // 1. uri is directory
        // 2. filename is provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("another-filename")
                .build();
        assertThat(noFilenameTask.equals(anotherTask)).isFalse();

        // compare to:
        // 1. uri is directory
        // 2. filename is not provided
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .build();

        assertThat(noFilenameTask.equals(anotherTask)).isTrue();

        // compare to:
        // 1. uri is directory
        // 2. filename is provided and the same to the response-filename
        anotherTask = new DownloadTask
                .Builder("url", uri)
                .setFilename("response-filename")
                .build();
        assertThat(noFilenameTask.equals(anotherTask)).isTrue();
    }

    @Test
    public void toBuilder() {
        // filename is provided specially => set filename
        final Uri uri = mock(Uri.class);
        when(uri.getPath()).thenReturn(parentPath);
        when(uri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);

        DownloadTask task = new DownloadTask
                .Builder("url", uri)
                .setFilename("filename1")
                .build();

        DownloadTask buildTask = task.toBuilder().build();
        assertThat(buildTask.getUrl()).isEqualTo("url");
        assertThat(buildTask.getUri()).isEqualTo(uri);
        assertThat(buildTask.getFilename()).isEqualTo("filename1");

        // another uri is file, use new filename
        final Uri anotherUri = mock(Uri.class);
        when(anotherUri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
        when(anotherUri.getPath()).thenReturn(parentPath + filename);

        buildTask = task.toBuilder("anotherUrl", anotherUri).build();
        assertThat(buildTask.getUrl()).isEqualTo("anotherUrl");
        assertThat(buildTask.getUri()).isEqualTo(anotherUri);
        assertThat(buildTask.getFilename()).isEqualTo(filename);

        // same uri provided filename => same file
        when(uri.getPath()).thenReturn(parentPath + filename);
        task = new DownloadTask
                .Builder("url", uri)
                .build();
        buildTask = task.toBuilder("anotherUrl", uri).build();
        assertFile(buildTask.getFile()).isEqualTo(task.getFile());
    }

    @Test
    public void profile() {
        final String url = "url";
        final Uri uri = mock(Uri.class);
        when(uri.getPath()).thenReturn("~/path");
        when(uri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);

        // basic profile
        DownloadTask task = new DownloadTask.Builder(url, uri)
                .setReadBufferSize(1)
                .setFlushBufferSize(2)
                .setSyncBufferSize(3)
                .setSyncBufferIntervalMillis(4)
                .setMinIntervalMillisCallbackProcess(5)
                .setAutoCallbackToUIThread(true)
                .setWifiRequired(true)
                .build();
        assertThat(task.getReadBufferSize()).isEqualTo(1);
        assertThat(task.getFlushBufferSize()).isEqualTo(2);
        assertThat(task.getSyncBufferSize()).isEqualTo(3);
        assertThat(task.getSyncBufferIntervalMills()).isEqualTo(4);
        assertThat(task.getMinIntervalMillisCallbackProcess()).isEqualTo(5);
        assertThat(task.isAutoCallbackToUIThread()).isTrue();
        assertThat(task.isWifiRequired()).isTrue();

        // setTag
        task.setTag("tag");
        assertThat(task.getTag()).isEqualTo("tag");
        task.removeTag();
        assertThat(task.getTag()).isNull();

        // addTag
        task.addTag(1, "tag1");
        task.addTag(2, "tag2");
        assertThat(task.getTag(1)).isEqualTo("tag1");
        assertThat(task.getTag(2)).isEqualTo("tag2");
        task.removeTag(1);
        assertThat(task.getTag(1)).isNull();

        // callback process timestamp
        task.setLastCallbackProcessTs(1L);
        assertThat(task.getLastCallbackProcessTs()).isEqualTo(1L);

        // setTags
        DownloadTask oldTask = new DownloadTask.Builder(url, uri).build();
        DownloadTask newTask = new DownloadTask.Builder(url, uri).build();
        oldTask.setTag("tag");
        oldTask.addTag(0, "tag0");
        newTask.setTags(oldTask);
        assertThat(newTask.getTag()).isEqualTo("tag");
        assertThat(newTask.getTag(0)).isEqualTo("tag0");
    }

    @Test
    public void operation() {
        final DownloadDispatcher dispatcher = OkDownload.with().downloadDispatcher();

        final String url = "url";
        final Uri uri = mock(Uri.class);
        when(uri.getScheme()).thenReturn(ContentResolver.SCHEME_FILE);
        when(uri.getPath()).thenReturn("~/path");
        DownloadTask task = new DownloadTask.Builder(url, uri).build();

        // enqueue
        final DownloadListener listener = mock(DownloadListener.class);
        task.enqueue(listener);
        assertThat(task.getListener()).isEqualTo(listener);
        verify(dispatcher).enqueue(eq(task));

        // cancel
        task.cancel();
        verify(dispatcher).cancel(eq(task));

        // execute
        task.execute(listener);
        assertThat(task.getListener()).isEqualTo(listener);
        verify(dispatcher).execute(eq(task));
    }

    @Test
    public void taskBuilder_constructWithFile() {
        final String url = "https://jacksgong.com";
        final File noExistFile = new File(parentPath, "no-exist");
        DownloadTask task = new DownloadTask.Builder(url, noExistFile).build();
        assertThat(task.getFilename()).isEqualTo(noExistFile.getName());
        assertThat(task.getFile().getAbsolutePath()).isEqualTo(noExistFile.getAbsolutePath());

        final File existFile = new File(parentPath, filename);
        task = new DownloadTask.Builder(url, existFile).build();
        assertThat(task.getFilename()).isEqualTo(existFile.getName());
        assertThat(task.getFile().getAbsolutePath()).isEqualTo(existFile.getAbsolutePath());

        final File existParentFile = new File(parentPath);
        task = new DownloadTask.Builder(url, existParentFile).build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(existParentFile);

        final File onlyFile = new File("/path");
        task = new DownloadTask.Builder(url, onlyFile).build();
        assertThat(task.getFilename()).isEqualTo("path");
        assertFile(task.getFile()).isEqualTo(onlyFile);
        assertFile(task.getParentFile()).isEqualTo(new File("/"));
    }

    @Test
    public void taskHideWrapper() {
        final DownloadTask task = mock(DownloadTask.class);

        DownloadTask.TaskHideWrapper.setLastCallbackProcessTs(task, 10L);
        verify(task).setLastCallbackProcessTs(eq(10L));

        DownloadTask.TaskHideWrapper.getLastCallbackProcessTs(task);
        verify(task).getLastCallbackProcessTs();

        final BreakpointInfo info = mock(BreakpointInfo.class);
        DownloadTask.TaskHideWrapper.setBreakpointInfo(task, info);
        verify(task).setBreakpointInfo(eq(info));
    }

    @Test
    public void getInfo() throws IOException {
        mockOkDownload();

        final BreakpointInfo info = mock(BreakpointInfo.class);
        final BreakpointStore store = OkDownload.with().breakpointStore();

        when(store.get(1)).thenReturn(info);
        when(store.findOrCreateId(any(DownloadTask.class))).thenReturn(1);

        final DownloadTask task = new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath))
                .build();

        assertThat(task.getInfo()).isEqualTo(info);
    }

    @Test
    public void mockTaskForCompare() {
        DownloadTask task = new DownloadTask
                .Builder("https://jacksgong.com", parentPath, filename)
                .build();

        IdentifiedTask identifiedTask = task.mock(0);
        assertThat(identifiedTask.compareIgnoreId(task)).isTrue();

        task = new DownloadTask
                .Builder("https://www.jacksgong.com", new File(parentPath))
                .build();
        identifiedTask = task.mock(0);
        assertThat(identifiedTask.compareIgnoreId(task)).isTrue();

        task = new DownloadTask
                .Builder("https://jacksgong.com", "non-exist-parent", "non-exist")
                .build();
        identifiedTask = task.mock(0);
        assertThat(identifiedTask.compareIgnoreId(task)).isTrue();
    }

    @Test
    public void constructor_path() {
        // exist[filename and parent path]
        DownloadTask task = new DownloadTask
                .Builder("https://jacksgong.com", parentPath, filename)
                .build();
        assertThat(task.getFilename()).isEqualTo(filename);
        assertThat(task.getFile().getAbsolutePath())
                .isEqualTo(new File(parentPath, filename).getAbsolutePath());

        // exist[filename and parent path] but force filename from response
        task = new DownloadTask
                .Builder("https://jacksgong.com", parentPath, filename)
                .setFilenameFromResponse(true)
                .build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(new File(parentPath));

        // exist[parent path] and provide[filename through setFilename]
        task = new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath))
                .setFilename(filename)
                .build();
        assertThat(task.getFilename()).isEqualTo(filename);
        assertFile(task.getFile()).isEqualTo(new File(parentPath, filename));
        assertFile(task.getParentFile()).isEqualTo(new File(parentPath));

        // exist[parent path] but not provide[filename]
        task = new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath))
                .build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(new File(parentPath));

        // unknown filename or parent path
        task = new DownloadTask
                .Builder("https://jacksgong.com", new File("/not-exist"))
                .build();
        assertThat(task.getFilename()).isEqualTo("not-exist");
        assertFile(task.getFile()).isEqualTo(new File("/not-exist"));
        assertFile(task.getParentFile()).isEqualTo(new File("/"));

        // unknown filename or parent path but set filename from response
        task = new DownloadTask
                .Builder("https://jacksgong.com", new File("not-exist"))
                .setFilenameFromResponse(true)
                .build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));

        // there is filename and parent path but all not exist
        task = new DownloadTask
                .Builder("https://jacksgong.com", new File("not-exist/filename"))
                .build();
        assertThat(task.getFilename()).isEqualTo("filename");
        assertFile(task.getFile()).isEqualTo(new File("not-exist/filename"));
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));

        task = new DownloadTask
                .Builder("https://jacksgong.com", "not-exist", "filename")
                .build();
        assertThat(task.getFilename()).isEqualTo("filename");
        assertFile(task.getFile()).isEqualTo(new File("not-exist", "filename"));
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));

        // there is filename and parent path but all not exist and set filename from response
        task = new DownloadTask
                .Builder("https://jacksgong.com", "not-exist", "filename")
                .setFilenameFromResponse(true)
                .build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));

        // there is filename and parent path but all not exist and set filename not from response
        task = new DownloadTask
                .Builder("https://jacksgong.com", "not-exist", "filename")
                .setFilenameFromResponse(false)
                .build();
        assertThat(task.getFilename()).isEqualTo("filename");
        assertFile(task.getFile()).isEqualTo(new File("not-exist/filename"));
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));


        task = new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath, "unknown-filename"))
                .setFilenameFromResponse(false)
                .build();
        assertThat(task.getFilename()).isEqualTo("unknown-filename");
        assertFile(task.getFile()).isEqualTo(new File(parentPath, "unknown-filename"));
        assertFile(task.getParentFile()).isEqualTo(new File(parentPath));

        // provide null filename.
        task = new DownloadTask
                .Builder("https://jacksgong.com", "not-exist", null)
                .build();
        assertThat(task.getFilename()).isNull();
        assertThat(task.getFile()).isNull();
        assertFile(task.getParentFile()).isEqualTo(new File("not-exist"));

        // provide is not directory but force filename from response.
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("If you want filename from response please make sure you "
                + "provide path is directory " + new File(parentPath, filename).getAbsolutePath());
        new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath, filename))
                .setFilenameFromResponse(true)
                .build();

        // no valid filename but force filename not from response
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("If you don't want filename from response please make sure"
                + " you have already provided valid filename or not directory path "
                + new File(parentPath).getAbsolutePath());
        new DownloadTask
                .Builder("https://jacksgong.com", new File(parentPath))
                .setFilenameFromResponse(false)
                .build();
    }

    @Test
    public void taskToString() {
        DownloadTask task = new DownloadTask.Builder("https://jacksgong.com",
                new File(parentPath, filename)).build();
        assertThat(task.toString())
                .endsWith("@" + task.getId() + "@https://jacksgong.com@"
                        + new File(parentPath, filename).getAbsolutePath());
    }

    @Test
    public void mockTaskForCompare_justId() {
        final IdentifiedTask task = DownloadTask.mockTaskForCompare(1);
        assertThat(task.getId()).isEqualTo(1);
        assertThat(task.getUrl()).isEqualTo(IdentifiedTask.EMPTY_URL);
        assertThat(task.getFilename()).isNull();
        assertThat(task.getParentFile()).isEqualTo(IdentifiedTask.EMPTY_FILE);
    }

    @Rule public ExpectedException thrown = ExpectedException.none();
}