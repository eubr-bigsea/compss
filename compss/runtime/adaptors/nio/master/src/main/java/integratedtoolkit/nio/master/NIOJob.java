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
package integratedtoolkit.nio.master;

import integratedtoolkit.ITConstants;

import java.util.LinkedList;

import integratedtoolkit.api.ITExecution.ParamType;
import integratedtoolkit.nio.NIOParam;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.commands.Data;
import integratedtoolkit.types.Implementation;
import integratedtoolkit.types.parameter.BasicTypeParameter;
import integratedtoolkit.types.parameter.DependencyParameter;
import integratedtoolkit.types.MethodImplementation;
import integratedtoolkit.types.parameter.Parameter;
import integratedtoolkit.types.TaskParams;
import integratedtoolkit.types.data.DataAccessId;
import integratedtoolkit.types.data.DataAccessId.RAccessId;
import integratedtoolkit.types.data.DataAccessId.RWAccessId;
import integratedtoolkit.types.job.Job.JobListener.JobEndStatus;
import integratedtoolkit.types.resources.Resource;


public class NIOJob extends integratedtoolkit.types.job.Job<NIOWorkerNode> {

    private static final String workerClasspath = (System.getProperty(ITConstants.IT_WORKER_CP) != null
            && System.getProperty(ITConstants.IT_WORKER_CP).compareTo("") != 0)
            ? System.getProperty(ITConstants.IT_WORKER_CP) : "\"\"";

    public NIOJob(int taskId, TaskParams taskParams, Implementation<?> impl, Resource res, JobListener listener) {
        super(taskId, taskParams, impl, res, listener);
    }

    @Override
    public String getHostName() {
        return worker.getName();
    }

    @Override
    public String toString() {
        MethodImplementation method = (MethodImplementation) this.impl;
        
        String className = method.getDeclaringClass();
        String methodName = taskParams.getName();
        
        return "NIOJob JobId" + this.jobId + " for method " + methodName + " at class " + className;
    }

    @Override
    public void submit() throws Exception {
        // Prepare the job
        logger.info("Submit NIOJob with ID " + jobId);

        NIOAdaptor.submitTask(this);

    }

    public NIOTask prepareJob() {
        MethodImplementation method = (MethodImplementation) this.impl;
        
        String className = method.getDeclaringClass();
        String methodName = taskParams.getName();
        boolean hasTarget = taskParams.hasTargetObject();

        LinkedList<NIOParam> params = addParams();

        int numParams = params.size();
        if (taskParams.hasReturnValue()) {
            numParams--;
        }

        NIOTask nt = new NIOTask(lang, getResourceNode().getInstallDir(), getResourceNode().getLibPath(), getResourceNode().getAppDir(), workerClasspath, workerDebug, className, methodName, hasTarget, params, numParams, taskId, this.taskParams.getId(), jobId, history, transferId);
        
        return nt;
    }

    private LinkedList<NIOParam> addParams() {
        LinkedList<NIOParam> params = new LinkedList<NIOParam>();
        for (Parameter param : taskParams.getParameters()) {
            ParamType type = param.getType();
            NIOParam np;
            if (type == ParamType.FILE_T || type == ParamType.OBJECT_T) {
                DependencyParameter dPar = (DependencyParameter) param;
                DataAccessId dAccId = dPar.getDataAccessId();
                Object value = dPar.getDataTarget();
                boolean preserveSourceData = true;
                if (dAccId instanceof RAccessId) {
                	// Parameter is a R, has sources
                	preserveSourceData = ((RAccessId) dAccId).isPreserveSourceData();
                } else if (dAccId instanceof RWAccessId) {
                	// Parameter is a RW, has sources
                	preserveSourceData = ((RWAccessId) dAccId).isPreserveSourceData();
                } else {
                	// Parameter is a W, it has no sources
                	preserveSourceData = false;
                }
                
                boolean writeFinalValue = !(dAccId instanceof RAccessId);	// Only store W and RW
                
                np = new NIOParam(type, preserveSourceData, writeFinalValue, value, (Data) dPar.getDataSource());
            } else {
                BasicTypeParameter btParB = (BasicTypeParameter) param;
                Object value = btParB.getValue();
                boolean preserveSourceData = false;	// Basic parameters are not preserved on Worker
                boolean writeFinalValue = false;	// Basic parameters are not stored on Worker
                
                np = new NIOParam(type, preserveSourceData, writeFinalValue, value, null);
            }

            params.add(np);
        }
        return params;
    }

    public JobKind getKind() {
        return JobKind.METHOD;
    }

    public void taskFinished(boolean successful) {
        if (successful) {
            listener.jobCompleted(this);
        } else {
            listener.jobFailed(this, JobEndStatus.EXECUTION_FAILED);
        }
    }

    @Override
    public void stop() throws Exception {
        //Do nothing
    }
}
