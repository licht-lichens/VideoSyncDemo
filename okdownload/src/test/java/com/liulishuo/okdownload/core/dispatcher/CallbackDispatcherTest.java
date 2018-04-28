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

package com.liulishuo.okdownload.core.dispatcher;

import android.os.Handler;

import com.liulishuo.okdownload.DownloadListener;
import com.liulishuo.okdownload.DownloadMonitor;
import com.liulishuo.okdownload.DownloadTask;
import com.liulishuo.okdownload.OkDownload;
import com.liulishuo.okdownload.TestUtils;
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo;
import com.liulishuo.okdownload.core.cause.EndCause;
import com.liulishuo.okdownload.core.cause.ResumeFailedCause;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.liulishuo.okdownload.TestUtils.mockOkDownload;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public class CallbackDispatcherTest {

    private CallbackDispatcher dispatcher;
    private CallbackDispatcher.DefaultTransmitListener transmit;

    @Mock private Handler handler;

    @Before
    public void setup() {
        initMocks(this);

        when(handler.post(any(Runnable.class))).thenAnswer(
                new Answer() {
                    @Override public Object answer(InvocationOnMock invocation) {
                        final Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    }
                });
        TestUtils.initProvider();
        transmit = spy(new CallbackDispatcher.DefaultTransmitListener(handler));
        dispatcher = spy(new CallbackDispatcher(handler, transmit));
    }

    @Test
    public void dispatch_ui() {
        final DownloadTask task = mock(DownloadTask.class);
        final DownloadListener listener = mock(DownloadListener.class);
        when(task.getListener()).thenReturn(listener);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final Map<String, List<String>> headerFields = mock(Map.class);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);
        final EndCause endCause = mock(EndCause.class);
        final Exception exception = mock(Exception.class);

        when(task.isAutoCallbackToUIThread()).thenReturn(true);

        dispatcher.dispatch().taskStart(task);
        verify(listener).taskStart(eq(task));

        dispatcher.dispatch().connectTrialStart(task, headerFields);
        verify(listener).connectTrialStart(eq(task), eq(headerFields));

        dispatcher.dispatch().connectTrialEnd(task, 200, headerFields);
        verify(listener).connectTrialEnd(eq(task), eq(200), eq(headerFields));

        dispatcher.dispatch().downloadFromBeginning(task, info, resumeFailedCause);
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));

        dispatcher.dispatch().downloadFromBreakpoint(task, info);
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));

        dispatcher.dispatch().connectStart(task, 1, headerFields);
        verify(listener).connectStart(eq(task), eq(1), eq(headerFields));

        dispatcher.dispatch().connectEnd(task, 2, 200, headerFields);
        verify(listener).connectEnd(eq(task), eq(2), eq(200), eq(headerFields));

        dispatcher.dispatch().fetchStart(task, 1, 2L);
        verify(listener).fetchStart(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchProgress(task, 1, 2L);
        verify(listener).fetchProgress(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchEnd(task, 1, 2L);
        verify(listener).fetchEnd(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().taskEnd(task, endCause, exception);
        verify(listener).taskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void dispatch_nonUi() {
        final DownloadTask task = mock(DownloadTask.class);
        final DownloadListener listener = mock(DownloadListener.class);
        when(task.getListener()).thenReturn(listener);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final Map<String, List<String>> headerFields = mock(Map.class);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);
        final EndCause endCause = mock(EndCause.class);
        final Exception exception = mock(Exception.class);

        when(task.isAutoCallbackToUIThread()).thenReturn(false);

        dispatcher.dispatch().taskStart(task);
        verify(listener).taskStart(eq(task));
        verify(transmit).inspectTaskStart(eq(task));

        dispatcher.dispatch().connectTrialStart(task, headerFields);
        verify(listener).connectTrialStart(eq(task), eq(headerFields));

        dispatcher.dispatch().connectTrialEnd(task, 200, headerFields);
        verify(listener).connectTrialEnd(eq(task), eq(200), eq(headerFields));

        dispatcher.dispatch().downloadFromBeginning(task, info, resumeFailedCause);
        verify(listener).downloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));
        verify(transmit).inspectDownloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));

        dispatcher.dispatch().downloadFromBreakpoint(task, info);
        verify(listener).downloadFromBreakpoint(eq(task), eq(info));
        verify(transmit).inspectDownloadFromBreakpoint(eq(task), eq(info));

        dispatcher.dispatch().connectStart(task, 1, headerFields);
        verify(listener).connectStart(eq(task), eq(1), eq(headerFields));

        dispatcher.dispatch().connectEnd(task, 2, 200, headerFields);
        verify(listener).connectEnd(eq(task), eq(2), eq(200), eq(headerFields));

        dispatcher.dispatch().fetchStart(task, 1, 2L);
        verify(listener).fetchStart(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchProgress(task, 1, 2L);
        verify(listener).fetchProgress(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().fetchEnd(task, 1, 2L);
        verify(listener).fetchEnd(eq(task), eq(1), eq(2L));

        dispatcher.dispatch().taskEnd(task, endCause, exception);
        verify(listener).taskEnd(eq(task), eq(endCause), eq(exception));
        verify(transmit).inspectTaskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void monitor_taskStart() throws IOException {
        mockOkDownload();

        final DownloadMonitor monitor = mock(DownloadMonitor.class);
        final OkDownload okDownload = OkDownload.with();
        when(okDownload.getMonitor()).thenReturn(monitor);

        final DownloadTask task = mock(DownloadTask.class);

        transmit.inspectTaskStart(task);
        verify(monitor).taskStart(eq(task));
    }

    @Test
    public void monitor_trialConnectEnd() throws IOException {
        mockOkDownload();

        final DownloadMonitor monitor = mock(DownloadMonitor.class);
        final OkDownload okDownload = OkDownload.with();
        when(okDownload.getMonitor()).thenReturn(monitor);

        final DownloadTask task = mock(DownloadTask.class);
        final BreakpointInfo info = mock(BreakpointInfo.class);
        final ResumeFailedCause resumeFailedCause = mock(ResumeFailedCause.class);

        transmit.inspectDownloadFromBeginning(task, info, resumeFailedCause);
        verify(monitor).taskDownloadFromBeginning(eq(task), eq(info), eq(resumeFailedCause));

        transmit.inspectDownloadFromBreakpoint(task, info);
        verify(monitor).taskDownloadFromBreakpoint(eq(task), eq(info));
    }

    @Test
    public void monitor_taskEnd() throws IOException {
        mockOkDownload();

        final DownloadMonitor monitor = mock(DownloadMonitor.class);
        final OkDownload okDownload = OkDownload.with();
        when(okDownload.getMonitor()).thenReturn(monitor);

        final DownloadTask task = mock(DownloadTask.class);
        final EndCause endCause = mock(EndCause.class);
        final Exception exception = mock(Exception.class);

        transmit.inspectTaskEnd(task, endCause, exception);
        verify(monitor).taskEnd(eq(task), eq(endCause), eq(exception));
    }

    @Test
    public void endTasks() {
        final Collection<DownloadTask> completedTaskCollection = new ArrayList<>();
        final Collection<DownloadTask> sameTaskConflictCollection = new ArrayList<>();
        final Collection<DownloadTask> fileBusyCollection = new ArrayList<>();

        dispatcher
                .endTasks(completedTaskCollection, sameTaskConflictCollection, fileBusyCollection);
        verify(handler, never()).post(any(Runnable.class));

        final DownloadTask autoUiTask = mock(DownloadTask.class);
        final DownloadTask nonUiTask = mock(DownloadTask.class);

        final DownloadListener nonUiListener = mock(DownloadListener.class);
        final DownloadListener autoUiListener = mock(DownloadListener.class);

        when(autoUiTask.getListener()).thenReturn(autoUiListener);
        when(autoUiTask.isAutoCallbackToUIThread()).thenReturn(true);

        when(nonUiTask.getListener()).thenReturn(nonUiListener);
        when(nonUiTask.isAutoCallbackToUIThread()).thenReturn(false);

        completedTaskCollection.add(autoUiTask);
        completedTaskCollection.add(nonUiTask);

        sameTaskConflictCollection.add(autoUiTask);
        sameTaskConflictCollection.add(nonUiTask);

        fileBusyCollection.add(autoUiTask);
        fileBusyCollection.add(nonUiTask);

        dispatcher
                .endTasks(completedTaskCollection, sameTaskConflictCollection, fileBusyCollection);

        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.SAME_TASK_BUSY), nullable(Exception.class));
        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.FILE_BUSY), nullable(Exception.class));

        verify(handler).post(any(Runnable.class));
        verify(autoUiListener)
                .taskEnd(eq(autoUiTask), eq(EndCause.COMPLETED), nullable(Exception.class));
        verify(autoUiListener)
                .taskEnd(eq(autoUiTask), eq(EndCause.SAME_TASK_BUSY), nullable(Exception.class));
        verify(autoUiListener)
                .taskEnd(eq(autoUiTask), eq(EndCause.FILE_BUSY), nullable(Exception.class));
    }

    @Test
    public void endTasksWithError() {
        final Collection<DownloadTask> errorCollection = new ArrayList<>();
        final Exception realCause = mock(Exception.class);

        final DownloadTask autoUiTask = mock(DownloadTask.class);
        final DownloadTask nonUiTask = mock(DownloadTask.class);

        final DownloadListener nonUiListener = mock(DownloadListener.class);
        final DownloadListener autoUiListener = mock(DownloadListener.class);

        when(autoUiTask.getListener()).thenReturn(autoUiListener);
        when(autoUiTask.isAutoCallbackToUIThread()).thenReturn(true);

        when(nonUiTask.getListener()).thenReturn(nonUiListener);
        when(nonUiTask.isAutoCallbackToUIThread()).thenReturn(false);

        errorCollection.add(autoUiTask);
        errorCollection.add(nonUiTask);

        dispatcher.endTasksWithError(errorCollection, realCause);

        verify(nonUiListener).taskEnd(eq(nonUiTask), eq(EndCause.ERROR), eq(realCause));
        verify(handler).post(any(Runnable.class));
        verify(autoUiListener).taskEnd(eq(autoUiTask), eq(EndCause.ERROR), eq(realCause));
    }

    @Test
    public void endTasksWithCanceled() {
        final Collection<DownloadTask> canceledCollection = new ArrayList<>();

        final DownloadTask autoUiTask = mock(DownloadTask.class);
        final DownloadTask nonUiTask = mock(DownloadTask.class);

        final DownloadListener nonUiListener = mock(DownloadListener.class);
        final DownloadListener autoUiListener = mock(DownloadListener.class);

        when(autoUiTask.getListener()).thenReturn(autoUiListener);
        when(autoUiTask.isAutoCallbackToUIThread()).thenReturn(true);

        when(nonUiTask.getListener()).thenReturn(nonUiListener);
        when(nonUiTask.isAutoCallbackToUIThread()).thenReturn(false);

        canceledCollection.add(autoUiTask);
        canceledCollection.add(nonUiTask);

        dispatcher.endTasksWithCanceled(canceledCollection);

        verify(nonUiListener)
                .taskEnd(eq(nonUiTask), eq(EndCause.CANCELED), nullable(Exception.class));
        verify(handler).post(any(Runnable.class));
        verify(autoUiListener)
                .taskEnd(eq(autoUiTask), eq(EndCause.CANCELED), nullable(Exception.class));
    }

    @Test
    public void isFetchProcessMoment_noMinInterval() {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getMinIntervalMillisCallbackProcess()).thenReturn(0);
        assertThat(dispatcher.isFetchProcessMoment(task)).isTrue();
    }

    @Test
    public void isFetchProcessMoment_largeThanMinInterval() {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getMinIntervalMillisCallbackProcess()).thenReturn(1);
        assertThat(dispatcher.isFetchProcessMoment(task)).isTrue();
    }

    @Test
    public void isFetchProcessMoment_lessThanMinInterval() {
        final DownloadTask task = mock(DownloadTask.class);
        when(task.getMinIntervalMillisCallbackProcess()).thenReturn(Integer.MAX_VALUE);
        assertThat(dispatcher.isFetchProcessMoment(task)).isFalse();
    }

    @Test
    public void fetchProgress_setMinInterval() {
        final DownloadTask task = spy(new DownloadTask
                .Builder("https://jacksgong.com", "parentPath", "filename")
                .setMinIntervalMillisCallbackProcess(1)
                .build());
        final DownloadListener listener = mock(DownloadListener.class);
        when(task.getListener()).thenReturn(listener);

        dispatcher.dispatch().fetchProgress(task, 1, 2);
        assertThat(DownloadTask.TaskHideWrapper.getLastCallbackProcessTs(task)).isNotZero();
    }
}