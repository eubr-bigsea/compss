/*
 *  Copyright 2002-2015 Barcelona Supercomputing Center (www.bsc.es)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package integratedtoolkit.connectors.utils;

import integratedtoolkit.connectors.VM;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.resources.CloudMethodWorker;
import integratedtoolkit.types.resources.ShutdownListener;
import integratedtoolkit.types.resources.description.CloudMethodResourceDescription;
import integratedtoolkit.util.ResourceManager;

import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

public class DeletionThread extends Thread {

    private final Operations operations;
    private final CloudMethodWorker worker;
    private final CloudMethodResourceDescription reduction;
    private VM vm;
    private static Integer count = 0;

    private static final Logger resourcesLogger = Logger.getLogger(Loggers.CONNECTORS);
    private static final Logger runtimeLogger = Logger.getLogger(Loggers.RM_COMP);
    private static final boolean debug = resourcesLogger.isDebugEnabled();

    public DeletionThread(Operations connector, CloudMethodWorker worker, CloudMethodResourceDescription reduction) {
        this.setName("DeletionThread " + worker.getName());
        this.operations = connector;
        synchronized (count) {
            count++;
        }
        this.worker = worker;
        this.reduction = reduction;
        this.vm = null;
    }

    public DeletionThread(Operations connector, VM vm) {
        this.setName("DeletionThread " + vm.getName());
        this.operations = connector;
        synchronized (count) {
            count++;
        }
        this.worker = null;
        this.reduction = null;
        this.vm = vm;
    }

    public static int getCount() {
        return count;
    }

    public void run() {
        if (reduction != null) {
            Semaphore sem = ResourceManager.reduceCloudWorker(worker, reduction);
            try {
                if (sem != null) {
                    if (debug) {
                        runtimeLogger.debug("Waiting until all tasks finishes for resource " + worker.getName() + "...");
                    }
                    sem.acquire();
                }
            } catch (InterruptedException e) {
                // We will never reach this point
            }
            if (debug) {
                runtimeLogger.debug("All tasks finished for resource " + worker.getName() + ". Pausing worker...");
            }
            this.vm = this.operations.pause(worker);
        }
        if (vm != null) {
            CloudMethodWorker worker = vm.getWorker();
            if (worker.shouldBeStopped()) {
                Semaphore sem = new Semaphore(0);
                ShutdownListener sl = new ShutdownListener(sem);
                runtimeLogger.info("Stopping worker " + worker.getName() + "...");
                worker.stop(true, sl);

                sl.enable();
                try {
                    sem.acquire();
                } catch (Exception e) {
                    resourcesLogger.error("ERROR: Exception raised on worker shutdown");
                }
                if (debug) {
                    runtimeLogger.debug("Stopping worker " + worker.getName() + "...");
                }
            } else {
                if (debug) {
                    runtimeLogger.debug("Worker " + worker.getName() + " should not be stopped.");
                }
            }
            if (debug) {
                runtimeLogger.debug("Worker " + worker.getName() + " stopped. Powering of the VM");
            }
            try {
                this.operations.poweroff(vm);
            } catch (Exception e) {
                resourcesLogger.error("Error powering off the resource", e);
            }

        }

        synchronized (count) {
            count--;
            if (debug) {
                runtimeLogger.debug("Number of current VMs deletions decreased (" + count + ").");
            }
        }

    }
}
