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

import es.bsc.comm.CommException;
import es.bsc.comm.Connection;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import es.bsc.comm.nio.NIONode;
import es.bsc.comm.stage.Transfer.Destination;
import integratedtoolkit.ITConstants;
import integratedtoolkit.api.ITExecution.ParamType;
import integratedtoolkit.comm.Comm;
import integratedtoolkit.comm.CommAdaptor;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.types.AdaptorDescription;
import integratedtoolkit.types.job.Job;
import integratedtoolkit.types.data.location.URI;
import integratedtoolkit.nio.NIOAgent;
import integratedtoolkit.nio.NIOAgent.DataRequest.MasterDataRequest;
import integratedtoolkit.nio.NIOMessageHandler;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.NIOTracer;
import integratedtoolkit.nio.NIOURI;
import integratedtoolkit.nio.commands.CommandNewTask;
import integratedtoolkit.nio.commands.Data;
import integratedtoolkit.nio.commands.workerFiles.CommandWorkerDebugFilesDone;
import integratedtoolkit.nio.exceptions.SerializedObjectException;
import integratedtoolkit.types.data.LogicalData;
import integratedtoolkit.types.data.location.DataLocation;
import integratedtoolkit.types.data.operation.DataOperation;
import integratedtoolkit.types.data.operation.Copy;
import integratedtoolkit.types.data.operation.DataOperation.EventListener;
import integratedtoolkit.types.job.Job.JobHistory;
import integratedtoolkit.types.resources.Resource;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import integratedtoolkit.types.resources.ShutdownListener;
import integratedtoolkit.util.ErrorManager;

import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;


public class NIOAdaptor extends NIOAgent implements CommAdaptor {

    public static final int MAX_SEND = 1000;
    public static final int MAX_RECEIVE = 1000;

    public static final int MAX_SEND_WORKER = 5;
    public static final int MAX_RECEIVE_WORKER = 5;

	private static final int MAX_RETRIES = 5;

    /*
     *  The master port can be:
     *  1. Given by the IT_MASTER_PORT property
     *  2. A BASE_MASTER_PORT plus a random number
     */
	public static final String DEPLOYMENT_ID = System.getProperty(ITConstants.IT_DEPLOYMENT_ID);
    private static final int BASE_MASTER_PORT = 43000;
    private static final int MAX_RANDOM_VALUE = 1000;
    private static final int RANDOM_VALUE = new Random().nextInt(MAX_RANDOM_VALUE);
    private static final int MASTER_PORT_CALCULATED = BASE_MASTER_PORT + RANDOM_VALUE;
    private static final String MASTER_PORT_PROPERTY = System.getProperty(ITConstants.IT_MASTER_PORT);
    public static final int MASTER_PORT = (MASTER_PORT_PROPERTY != null && !MASTER_PORT_PROPERTY.isEmpty()) ?
    		Integer.valueOf(MASTER_PORT_PROPERTY) : MASTER_PORT_CALCULATED;

    // Final jobs log directory
    private static final String JOBS_DIR = System.getProperty(ITConstants.IT_APP_LOG_DIR) + "jobs" + File.separator;

    private static final String TERM_ERR = "Error terminating";
    //private static final String SER_RCV_ERR = "Error serializing received object";

    private static final HashSet<NIOWorkerNode> nodes = new HashSet<NIOWorkerNode>();

    private static final ConcurrentHashMap<Integer, NIOJob> runningJobs = new ConcurrentHashMap<Integer, NIOJob>();

    private static final HashMap<Integer, LinkedList<Copy>> groupToCopy = new HashMap<Integer, LinkedList<Copy>>();

    private static final HashMap<Connection, ClosingWorker> stoppingNodes = new HashMap<Connection, ClosingWorker>();

    private Semaphore tracingGeneration = new Semaphore(0);
    private Semaphore workersDebugInfo = new Semaphore(0);

    public static boolean workerDebug = Logger.getLogger(Loggers.WORKER).isDebugEnabled();


    public NIOAdaptor() {
        super(MAX_SEND, MAX_RECEIVE, MASTER_PORT);
        File file = new File(JOBS_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public void init() {
        logger.info("Initializing NIO Adaptor...");
        masterNode = new NIONode(null, MASTER_PORT);

        // Instantiate the NIO Message Handler
        final NIOMessageHandler mhm = new NIOMessageHandler(this);

        // Init the Transfer Manager
        logger.debug("  Initializing the TransferManager structures...");
        try {
        	tm.init(NIOEventManagerClass, null, mhm);
        } catch (CommException ce) {
        	String errMsg = "Error initializing the TransferManager";
        	ErrorManager.error(errMsg, ce);
        }

        /* Init tracing values */
        tracing = System.getProperty(ITConstants.IT_TRACING) != null
               && Integer.parseInt(System.getProperty(ITConstants.IT_TRACING)) > 0;
        tracing_level = Integer.parseInt(System.getProperty(ITConstants.IT_TRACING));


        // Start the server
        logger.debug("  Starting transfer server...");
        try {
        	tm.startServer(masterNode);
        } catch (CommException ce) {
        	String errMsg = "Error starting transfer server";
        	ErrorManager.error(errMsg, ce);
        }

        // Start the Transfer Manager thread (starts the EventManager)
        logger.debug("  Starting TransferManager Thread");
        tm.start();
    }

    public NIOWorkerNode initWorker(String name, HashMap<String, String> properties, TreeMap<String, AdaptorDescription> adaptorsDesc) throws Exception {
        logger.debug("Init NIO Worker Node");
        NIOWorkerNode worker = null;
        worker = new NIOWorkerNode(name, properties, this);
        NIONode n = null;
        try {
        	n = new WorkerStarter(worker, adaptorsDesc).startWorker();
        }
        catch(Exception e) {
            ErrorManager.warn("There was an error when initiating worker " + worker.getName() + ".", e);
            throw e;
        }
        worker.setNode(n);
        nodes.add(worker);

        return worker;
    }

    public void removedNode(NIOWorkerNode worker) {
        logger.debug("Remove worker " + worker.getName());
        nodes.remove(worker);
    }

    public void stop() {
        logger.debug("NIO Adaptor stoping workers...");
        HashSet<NIOWorkerNode> workers = new HashSet<NIOWorkerNode>();
        workers.addAll(nodes);

        Semaphore sem = new Semaphore(0);
        ShutdownListener sl = new ShutdownListener(sem);
        for (NIOWorkerNode worker : workers) {
            logger.debug("- Stopping worker" + worker.getName());
            worker.stop(sl);
        }

        logger.debug("- Waiting for workers to shutdown...");
        sl.enable();
        try {
            sem.acquire();
        } catch (Exception e) {
            logger.error("ERROR: Exception raised on worker shutdown");
        }
        logger.debug("- Workers stoped");

        logger.debug("- Shutting down TM...");
        tm.shutdown(null);
        logger.debug("NIO Adaptor stop completed!");
    }

    protected static void submitTask(NIOJob job) throws Exception {
        logger.debug("NIO submitting new job " + job.getJobId());
        Resource res = job.getResource();
        NIOWorkerNode node = (NIOWorkerNode) res.getNode();
        NIONode hostNode = node.getNode();
        LinkedList<String> obsolete = res.clearObsoletes();

        runningJobs.put(job.getJobId(), job);
        NIOTask t = job.prepareJob();
        CommandNewTask cmd = new CommandNewTask(t, obsolete);
        for (int retries=0;retries<MAX_RETRIES;retries++){
        	if(trySubmitTask(hostNode, cmd)){
        		return;
        	}else{
        		try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Nothing to do
				}
        	}
        }
        //Submission has failed
        throw new Exception("Submit Job"+ job.getJobId()+ " failed.");
    }

    private static boolean trySubmitTask(NIONode hostNode, CommandNewTask cmd) {
    	Connection c = tm.startConnection(hostNode);
		c.sendCommand(cmd);
		c.finishConnection();

		return true;
	}

    @Override
    public void receivedNewTask(NIONode master, NIOTask t, LinkedList<String> obsoleteFiles) {
        //Can not run any task. Do nothing
    }

    @Override
    public void setMaster(NIONode master) {
        // this is called on NIOWorker
        // Setting Master on Adaptor --> Nothig to be done
    }

    @Override
    public boolean isMyUuid(String uuid) {
        // this is used on NIOWorker to check sent UUID against worker UUID
        return false;
    }

    @Override
    public void setWorkerIsReady(String nodeName) {
        logger.info("Notifying that worker is ready " + nodeName);
        WorkerStarter ws = WorkerStarter.getWorkerStarter(nodeName);
        ws.setWorkerIsReady();
    }

    public void receivedTaskDone(Connection c, int jobId, boolean successful) {

        NIOJob nj = runningJobs.remove(jobId);

        //Check if all the FILE outs have been generated
       // nj.getCore().getParameters()

        JobHistory h = nj.getHistory();
        nj.taskFinished(successful);
        if (workerDebug) {
            c.receiveDataFile(JOBS_DIR + "job" + nj.getJobId() + "_" + h + ".out");
            c.receiveDataFile(JOBS_DIR + "job" + nj.getJobId() + "_" + h + ".err");
        } else {
            if (!successful) {
                c.receiveDataFile(JOBS_DIR + "job" + nj.getJobId() + "_" + nj.getHistory() + ".out");
                c.receiveDataFile(JOBS_DIR + "job" + nj.getJobId() + "_" + nj.getHistory() + ".err");
            }
        }
        c.finishConnection();
    }

    public void registerCopy(Copy c) {
        for (EventListener el : c.getEventListeners()) {
            Integer groupId = el.getId();
            LinkedList<Copy> copies = groupToCopy.get(groupId);
            if (copies == null) {
                copies = new LinkedList<Copy>();
                groupToCopy.put(groupId, copies);
            }
            copies.add(c);
        }
    }

    @Override
    protected void handleDataToSendNotAvailable(Connection c, Data d) {
    	//Finish the connection asap. The comm library will notify this error upwards as a ClosedChannelError.
    	c.finishConnection();
    }

    @Override
    public void handleRequestedDataNotAvailableError(LinkedList<DataRequest> failedRequests, String dataId) {
        for (DataRequest dr : failedRequests) {
            MasterDataRequest mdr = (MasterDataRequest) dr;
            Copy c = (Copy) mdr.getOperation();
            c.getSourceData().finishedCopy(c);
            c.end(DataOperation.OpEndState.OP_FAILED); //Notify the copy has failed
        }
    }

    @Override
    public void receivedValue(Destination type, String dataId, Object object, LinkedList<DataRequest> achievedRequests) {
        for (DataRequest dr : achievedRequests) {
            MasterDataRequest mdr = (MasterDataRequest) dr;
            Copy c = (Copy) mdr.getOperation();
            DataLocation actualLocation = c.getSourceData().finishedCopy(c);
            LogicalData tgtData = c.getTargetData();
            if (tgtData != null) {
                tgtData.addLocation(actualLocation);
                if (object != null) {
                    tgtData.setValue(object);
                }
            }
            c.end(DataOperation.OpEndState.OP_OK);
        }
        if(tracing){
            NIOTracer.emitDataTransferEvent(NIOTracer.TRANSFER_END);
        }
    }

    public void copiedData(int transferGroupId) {
        LinkedList<Copy> copies = groupToCopy.remove(transferGroupId);
        if (copies == null) {
            return;
        }
        for (Copy c : copies) {
            if (!c.isRegistered()) {
                continue;
            }
            DataLocation actualLocation = c.getSourceData().finishedCopy(c);
            LogicalData tgtData = c.getTargetData();
            if (tgtData != null) {
                tgtData.addLocation(actualLocation);
            }
        }
    }

    // Return the data that a worker should be obtaining
    // and has not yet confirmed
    public LinkedList<DataOperation> getPending() {
        return new LinkedList<DataOperation>();
    }

    public boolean checkData(Data d) {
        boolean data = false;
        /*for (Entry<String, LogicalData> e : Comm.DC.nameToLogicalData.entrySet()) {
         if (d.getSourceName().equals(e.getValue().getName())) {
         data = true;
         break;
         }
         }*/
        return data;
    }

    public Object getObject(String name) throws SerializedObjectException {
    	LogicalData ld = Comm.getData(name);
    	Object o = ld.getValue();

    	// Check if the object has been serialized meanwhile
    	if (o == null) {
    		for (URI loc : ld.getURIs()) {
    			if (loc.getHost().equals(Comm.appHost.getName())) {
    				// The object is null because it has been serialized by the master, raise exception
    				throw new SerializedObjectException(name);
    			}
    		}
    	}

    	// If we arrive to this return means:
    	//    1- The object has been found		or
    	//    2- The object is really null (no exception thown)
    	return o;
    }

    public String getObjectAsFile(String name) {
    	LogicalData ld = Comm.getData(name);

    	// Get a Master location
		for (URI loc : ld.getURIs()) {
			if (loc.getHost().equals(Comm.appHost.getName())) {
				return loc.getPath();
			}
		}

		// No location found in master
    	return null;
    }

    public String getWorkingDir() {
        return "";
    }

    public void stopSubmittedJobs() {

        synchronized (runningJobs) {
            for (Job<?> job : runningJobs.values()) {
                try {
                    job.stop();
                } catch (Exception e) {
                    logger.error(TERM_ERR, e);
                }
            }
        }
    }

    @Override
    public void completeMasterURI(URI u) {
        u.setInternalURI(ID, new NIOURI(masterNode, u.getPath()));
    }

    public void requestData(Copy c, ParamType paramType, Data d, String path) {
        DataRequest dr = new MasterDataRequest(c, paramType, d, path);
        addTransferRequest(dr);
        requestTransfers();

    }

    public void shuttingDown(NIOWorkerNode worker, Connection c, ShutdownListener listener) {
        stoppingNodes.put(c, new ClosingWorker(worker, listener));
    }

    @Override
    public void shutdownNotification(Connection c) {
        ClosingWorker closing = stoppingNodes.remove(c);
        NIOWorkerNode worker = closing.worker;
        removedNode(worker);
        ShutdownListener listener = closing.listener;
        listener.notifyEnd();
    }

    @Override
    public void shutdown(Connection closingConnection) {
    	// Master side, nothing to do
    }

    @Override
    public void waitUntilTracingPackageGenerated() {
        try {
            tracingGeneration.acquire();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for package generation");
        }

    }

    @Override
    public void notifyTracingPackageGeneration() {
        tracingGeneration.release();
    }

    @Override
    public void waitUntilWorkersDebugInfoGenerated() {
        try {
            workersDebugInfo.acquire();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for package generation");
        }

    }

    @Override
    public void notifyWorkersDebugInfoGeneration() {
    	workersDebugInfo.release();
    }

	@Override
    public void generateWorkersDebugInfo(Connection c) {
        c.sendCommand(new CommandWorkerDebugFilesDone());
        c.finishConnection();
    }

    private class ClosingWorker {

        private final NIOWorkerNode worker;
        private final ShutdownListener listener;

        public ClosingWorker(NIOWorkerNode w, ShutdownListener l) {
            worker = w;
            listener = l;
        }
    }
}
