/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.vcs.changes;

import com.intellij.lifecycle.AtomicSectionsAware;
import com.intellij.lifecycle.PeriodicalTasksCloser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NotNull;

/**
 * for restarted single threaded executor
 */
public class ExecutorWrapper {
  private final Semaphore mySemaphore;
  private volatile boolean myDisposeStarted;
  private final Object myLock;
  private boolean myInProgress;
  private Thread myCurrentWorker;
  private final AtomicSectionsAware myAtomicSectionsAware;

  protected ExecutorWrapper(@NotNull Project project, @NotNull String name) {
    myLock = new Object();
    mySemaphore = new Semaphore();

    Runnable stopper = new Runnable() {
      public void run() {
        synchronized (myLock) {
          myDisposeStarted = true;
          if (myCurrentWorker != null) {
            myCurrentWorker.interrupt();
          }
          taskFinished();
        }
      }
    };

    myAtomicSectionsAware = new AtomicSectionsAware() {
      public void checkShouldExit() throws ProcessCanceledException {
        if (myDisposeStarted) {
          throw new ProcessCanceledException();
        }
      }
      public void enter() {
        // we won't kill the thread, so we can ignore information
      }
      public void exit() {
        //the same
      }
      public boolean shouldExitAsap() {
        return myDisposeStarted;
      }
    };
    myDisposeStarted = ! PeriodicalTasksCloser.getInstance().register(project, name, stopper);
  }

  private void taskFinished() {
    synchronized (myLock) {
      myCurrentWorker = null;
      if (myInProgress) {
        myInProgress = false;
        mySemaphore.up();
      }
    }
  }

  /**
   * executed on separate thread, that can be interrupted immediately, while "the main thread" gets control back
   * and can continue to serve other requests
   */
  public void submit(final Consumer<AtomicSectionsAware> runnable) {
    try {
      synchronized (myLock) {
        if (myDisposeStarted) return;
        assert ! myInProgress;
        myInProgress = true;
        mySemaphore.down();
      }
      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        public void run() {
          synchronized (myLock) {
            myCurrentWorker = Thread.currentThread();
          }
          try {
            runnable.consume(myAtomicSectionsAware);
          } finally {
            taskFinished();
          }
        }
      });
      mySemaphore.waitFor();
    } finally {
      taskFinished();
    }
  }
}
