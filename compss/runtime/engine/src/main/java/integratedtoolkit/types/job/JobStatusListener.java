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
package integratedtoolkit.types.job;

import integratedtoolkit.components.impl.JobManager;
import integratedtoolkit.types.Task;
import integratedtoolkit.types.job.Job.JobListener;
import integratedtoolkit.types.resources.Worker;

public class JobStatusListener implements JobListener {

    private final Worker<?> worker;
    private final Task task;
    private final JobManager jm;

    public JobStatusListener(Worker<?> worker, Task t, JobManager jm) {
        this.worker = worker;
        this.task = t;
        this.jm = jm;
    }

    @Override
    public void jobCompleted(Job<?> job) {
        jm.completedJob(job, task, worker);
    }

    @Override
    public void jobFailed(Job<?> job, JobEndStatus endStatus) {
        jm.failedJob(job, task, endStatus, worker);
    }

}
