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
package integratedtoolkit.nio.worker.executors;

import integratedtoolkit.ITConstants;
import org.apache.log4j.Logger;

import integratedtoolkit.log.Loggers;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.NIOTracer;
import integratedtoolkit.nio.worker.NIOWorker;


public abstract class Executor {
	
    protected static final Logger logger = Logger.getLogger(Loggers.WORKER);
       
    // Tracing
    protected static final boolean tracing = System.getProperty(ITConstants.IT_TRACING) != null
            && Integer.parseInt(System.getProperty(ITConstants.IT_TRACING)) > 0;
    
    public final boolean execute(NIOTask nt, NIOWorker nw) {

        if (tracing){
            NIOTracer.emitEvent(NIOTracer.Event.TASK_RUNNING.getId() , NIOTracer.Event.TASK_RUNNING.getType());
        }

        
        NIOWorker.registerOutputs(NIOWorker.workingDir + "/jobs/job" + nt.getJobId() + "_" + nt.getHist());
        String sandBox;
        try {
            logger.debug("Creating sandbox for job "+nt.getJobId());
            sandBox = createSandBox();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            NIOWorker.unregisterOutputs();
            if (tracing) {
            	NIOTracer.emitEvent(NIOTracer.EVENT_END, NIOTracer.Event.TASK_RUNNING.getType());
            }
            return false;
        }

        try {
            executeTask(sandBox, nt, nw);
        } catch (Exception e) {
        	logger.error(e.getMessage(), e);
            return false;
        } finally {
            try {
            	logger.debug("Removing sandbox for job " + nt.getJobId());
            	removeSandBox(sandBox);
            } catch (Exception e1) {
            	logger.error(e1.getMessage(), e1);
                return false;
            } finally {
                NIOWorker.unregisterOutputs();
                if (tracing) {
                    NIOTracer.emitEvent(NIOTracer.EVENT_END, NIOTracer.Event.TASK_RUNNING.getType());
                }
            }
        }

        return true;
    }

    abstract String createSandBox() throws Exception;

    abstract void executeTask(String sandBox, NIOTask nt, NIOWorker nw) throws Exception;

    abstract void removeSandBox(String sandBox) throws Exception;
}
