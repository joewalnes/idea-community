package com.intellij.unscramble;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yole
 */
public class ThreadState {
  private final String myName;
  private final String myState;
  private String myStackTrace;
  private boolean myEmptyStackTrace;
  private String myJavaThreadState;
  private String myThreadStateDetail;
  private String myExtraState;
  private final Set<ThreadState> myThreadsWaitingForMyLock = new HashSet<ThreadState>();
  private final Set<ThreadState> myDeadlockedThreads = new HashSet<ThreadState>();

  @Nullable
  private ThreadOperation myOperation;

  public ThreadState(final String name, final String state) {
    myName = name;
    myState = state.trim();
  }

  public String getName() {
    return myName;
  }

  public String getState() {
    return myState;
  }

  public String getStackTrace() {
    return myStackTrace;
  }

  public void setStackTrace(final String stackTrace, boolean isEmpty) {
    myStackTrace = stackTrace;
    myEmptyStackTrace = isEmpty;
  }

  public Collection<ThreadState> getAwaitingThreads() {
    return Collections.unmodifiableSet(myThreadsWaitingForMyLock);
  }

  public String toString() {
    return myName;
  }

  public void setJavaThreadState(final String javaThreadState) {
    myJavaThreadState = javaThreadState;
  }

  public void setThreadStateDetail(@NonNls final String threadStateDetail) {
    myThreadStateDetail = threadStateDetail;
  }

  public String getJavaThreadState() {
    return myJavaThreadState;
  }

  public String getThreadStateDetail() {
    if (myOperation != null) {
      return myOperation.toString();
    }
    return myThreadStateDetail;
  }

  public boolean isEmptyStackTrace() {
    return myEmptyStackTrace;
  }

  public String getExtraState() {
    return myExtraState;
  }

  public void setExtraState(final String extraState) {
    myExtraState = extraState;
  }

  public boolean isSleeping() {
    return "sleeping".equals(getThreadStateDetail()) ||
           (("parking".equals(getThreadStateDetail()) || "waiting on condition".equals(myState)) && isThreadPoolExecutor());
  }

  private boolean isThreadPoolExecutor() {
    return myStackTrace.contains("java.util.concurrent.ScheduledThreadPoolExecutor$DelayedWorkQueue.take") ||
           myStackTrace.contains("java.util.concurrent.ThreadPoolExecutor.getTask");
  }

  public boolean isAwaitedBy(ThreadState thread) {
    return myThreadsWaitingForMyLock.contains(thread);
  }

  public void addWaitingThread(ThreadState thread) {
    myThreadsWaitingForMyLock.add(thread);
  }

  public boolean isDeadlocked() {
    return !myDeadlockedThreads.isEmpty();
  }

  public void addDeadlockedThread(ThreadState thread) {
    myDeadlockedThreads.add(thread);
  }

  @Nullable
  public ThreadOperation getOperation() {
    return myOperation;
  }

  public void setOperation(@Nullable final ThreadOperation operation) {
    myOperation = operation;
  }

  public boolean isWaiting() {
    return "on object monitor".equals(myThreadStateDetail) || 
           myState.startsWith("waiting") ||
           ("parking".equals(myThreadStateDetail) && !isSleeping());
  }

  public boolean isEDT() {
    return getName().startsWith("AWT-EventQueue");
  }
}