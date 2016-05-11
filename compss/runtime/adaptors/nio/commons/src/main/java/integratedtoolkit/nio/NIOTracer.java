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
package integratedtoolkit.nio;

import integratedtoolkit.ITConstants;
import integratedtoolkit.util.Tracer;
import integratedtoolkit.util.StreamGobbler;
import es.bsc.cepbatools.extrae.Wrapper;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.abs;


public class NIOTracer extends Tracer {
    
    private static String scriptDir = "";
    private static String workingDir = "";
    private static String nodeName = "master";  // while no worker sets the Tracer info we assume we are on master
    private static final int ID = 121;
    
    public static final String TRANSFER_END = "0";
    
    
    public static void startTracing(String workerName, String workerUser, String workerHost, Integer numThreads) {
        if (numThreads <= 0) {
            if (debug) {
                logger.debug("Resource " + workerName + " has 0 slots, it won't appear in the trace");
            }
            return;
        }
        
        if (debug) {
            logger.debug("NIO uri File: " + "any:///" + System.getProperty(ITConstants.IT_APP_LOG_DIR) + traceOutRelativePath);
            logger.debug("any:///" + System.getProperty(ITConstants.IT_APP_LOG_DIR) + traceOutRelativePath);
        }
    }

    public static void setWorkerInfo(String scriptDir, String nodeName, String workingDir, int hostID){
        NIOTracer.scriptDir = scriptDir;
        NIOTracer.workingDir = workingDir;
        NIOTracer.nodeName = nodeName;
        
        synchronized(Tracer.class){
            Wrapper.SetTaskID(hostID);
            Wrapper.SetNumTasks(hostID+1);
        }

        if (debug) { 
            logger.debug("Tracer worker for host " + hostID + " and: " + NIOTracer.scriptDir + ", " +NIOTracer.workingDir + ", " + NIOTracer.nodeName);
        }
    }

    public static void emitDataTransferEvent(String data){
        boolean dataTransfer = !(data.startsWith("worker")) && !(data.startsWith("tracing"));
        
        int transferID = (data.equals(TRANSFER_END)) ? 0 : abs(data.hashCode());
        
        if (dataTransfer){
            emitEvent(transferID, DATA_TRANSFERS);
        }

        if (debug) {
            logger.debug( (dataTransfer ? "E" : "Not E") + "mitting synchronized data transfer event [name, id] = [" + data + " , " + transferID + "]");
        }
    }
    
    public static void emitEventAndCounters(int taskId, int eventType){
        synchronized(Tracer.class){
            Wrapper.Eventandcounters(eventType, taskId);
        }
        
        if (debug){
            logger.debug("Emitting synchronized event with HW counters [type, taskId] = [" + eventType + " , " + taskId + "]");
        }
        
    }
    public static void emitCommEvent(boolean send, int partnerID, int tag){

        int size = 0;
        synchronized(Tracer.class){
            Wrapper.Comm(send, tag, size, partnerID, ID);
        }

        if (debug) {
            logger.debug("Emitting communication event [" + (send ? "SEND" : "REC") + "] " + tag + ", " + size + ", " + partnerID + ", " + ID + "]");
        }
    }
    
    public static void generatePackage() {
        masterEventStart(Event.STOP.getId());
        if (debug){
            logger.debug("Generating package of "+ nodeName + ", with " + scriptDir);
        }
        masterEventFinish();
        
        synchronized(Tracer.class){
            Wrapper.SetOptions (
                Wrapper.EXTRAE_ENABLE_ALL_OPTIONS &
                ~Wrapper.EXTRAE_PTHREAD_OPTION);
        
            // End wrapper
            Wrapper.Fini();
        }
        // Generate package
        ProcessBuilder pb = new ProcessBuilder(scriptDir + File.separator + TRACE_SCRIPT, "package", workingDir, nodeName);
        pb.environment().remove("LD_PRELOAD");
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            logger.error("Error generating " + nodeName + " package", e);
            return;
        }
        
    	// Only capture output/error if debug level (means 2 more threads)
        if (debug) {
            StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), System.out);
            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), System.err);
            outputGobbler.start();
            errorGobbler.start();
            logger.debug("Created globbers");
        }
        
        try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
                logger.error("Error generating " + nodeName + " package, exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            logger.error("Error generating " + nodeName + " package (interruptedException) : " + e.getMessage());
        }
        if (debug){
            logger.debug("Finish generating");
        }
    }
    
}
