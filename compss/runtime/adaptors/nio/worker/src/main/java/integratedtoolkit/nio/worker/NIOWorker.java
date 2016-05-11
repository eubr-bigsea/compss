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
package integratedtoolkit.nio.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import es.bsc.comm.CommException;
import es.bsc.comm.Connection;
import es.bsc.comm.nio.NIONode;
import es.bsc.comm.stage.Transfer;
import es.bsc.comm.stage.Transfer.Destination;
import integratedtoolkit.ITConstants;
import integratedtoolkit.api.ITExecution.ParamType;
import integratedtoolkit.nio.NIOAgent;
import integratedtoolkit.nio.NIOParam;
import integratedtoolkit.nio.NIOTask;
import integratedtoolkit.nio.NIOURI;
import integratedtoolkit.nio.commands.CommandDataReceived;
import integratedtoolkit.nio.commands.Data;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.nio.NIOMessageHandler;
import integratedtoolkit.nio.commands.CommandShutdownACK;
import integratedtoolkit.nio.commands.CommandTaskDone;
import integratedtoolkit.util.ErrorManager;
import integratedtoolkit.nio.commands.workerFiles.CommandWorkerDebugFilesDone;
import integratedtoolkit.nio.exceptions.SerializedObjectException;
import integratedtoolkit.util.RequestQueue;
import integratedtoolkit.util.Serializer;
import integratedtoolkit.util.ThreadPool;
import integratedtoolkit.nio.NIOTracer;


public class NIOWorker extends NIOAgent {

    public static boolean workerDebug;
    public static String workingDir;

    public static int jobThreads;
    public static String POOL_NAME = "NIO_JOBS";
    protected static final String THREAD_POOL_ERR = "Error starting pool of threads";

    protected static final Logger wLogger = Logger.getLogger(Loggers.WORKER);
	private static final int MAX_RETRIES = 5;

    protected static ThreadPool pool;

    public static RequestQueue<NIOTask> jobQueue;

    public static ThreadPrintStream out;
    public static ThreadPrintStream err;

    private final ObjectCache objectCache;

    private final String host;

    private String deploymentId;

    static {
        try {
            out = new ThreadPrintStream(".out", System.out);
            err = new ThreadPrintStream(".err", System.err);
            System.setErr(err);
            System.setOut(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public NIOWorker(String uuid, int snd, int rcv, int masterPort, String hostName) {
        super(snd, rcv, masterPort);

        // Log worker creation
        wLogger.info("NIO Worker init");

        this.deploymentId = uuid;
        objectCache = new ObjectCache();
        /*tasks = new HashMap<Integer, NIOTask>();
         jobsPending = new HashMap<Integer, LinkedList<String>>();
         transferToJobs = new HashMap<String, LinkedList<Integer>>();*/

        masterNode = null;

        // Start pool of workers
        jobQueue = new RequestQueue<NIOTask>();
        if (tracing_level == NIOTracer.BASIC_MODE){
            NIOTracer.enablePThreads();
        }

        pool = new ThreadPool(jobThreads, POOL_NAME, new JobLauncher(jobQueue, this, NIOWorker.wLogger, NIOWorker.workerDebug));
        try {
            pool.startThreads();
        } catch (Exception e) {
        	ErrorManager.error(THREAD_POOL_ERR, e);
        }
        if (tracing_level == NIOTracer.BASIC_MODE){
            NIOTracer.disablePThreads();
        }
        this.host = hostName;
    }

    @Override
    public void setWorkerIsReady(String nodeName){
        // implemented on NIOAdaptor to notify that the worker is up and ready
    }


    @Override
    public void setMaster(NIONode master){
        if (masterNode == null) {
            masterNode = new NIONode(master.ip, masterPort);
        }
    }

    @Override
    public boolean isMyUuid(String uuid){
        return uuid.equals(this.deploymentId);
    }


    @Override
    public void receivedNewTask(NIONode master, NIOTask task, LinkedList<String> obsoleteFiles) {
        wLogger.info("Received Job " + task);

        // Remove obsolete
        if (obsoleteFiles != null) {
            removeObsolete(obsoleteFiles);
        }

        // Demand files
        wLogger.info("Checking parameters");
        TransferringTask tt = new TransferringTask(task);
        int i = 0;
        for (NIOParam param : task.getParams()) {
            i++;
            if (param.getData() != null) {
            	// Parameter has associated data
            	wLogger.debug("- Checking transfers for data of parameter " + (String) param.getValue());

                switch (param.getType()) {
                	/* OBJECTS */
                    case OBJECT_T:
                    	wLogger.debug("   - " + (String) param.getValue() + " registered as object.");

                    	boolean catched = false;
                    	boolean locationsInCache = false;
                    	boolean existInHost = false;
                    	boolean askTransfer = false;

                    	// Try if parameter is in cache
                    	wLogger.debug("   - Checking if " + (String) param.getValue() + " is in cache.");
                    	catched = objectCache.checkPresence((String) param.getValue());
                    	if (!catched) {
                    		// Try if any of the object locations is in cache
                    		wLogger.debug("   - Checking if " + (String) param.getValue() + " locations are catched");
                    	    for (NIOURI loc : param.getData().getSources()) {
                    	    	if (objectCache.checkPresence(loc.getPath())) {
                    	    		// Object found
                    	    		wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") location found in cache.");
                    	    		try {
	                    	    		if (param.isPreserveSourceData()) {
	                    	    			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") preserves sources. CACHE-COPYING");
	                    	    			Object o = Serializer.deserialize(loc.getPath());
	    	    							storeInCache((String) param.getValue(), o);
	                    	    		} else {
	                    	    			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") erases sources. CACHE-MOVING");
	                    	    			Object o = objectCache.get(loc.getPath());
	                    	    			objectCache.remove(loc.getPath());
	    	    							storeInCache((String) param.getValue(), o);
	                    	    		}
	                    	    		locationsInCache = true;
                    	    		} catch (IOException ioe) {
                    	    			// If exception is raised, locationsInCache remains false. We log the exception and try host files
    	    							wLogger.error(ioe);
            	                	} catch (ClassNotFoundException e) {
            	                		// If exception is raised, locationsInCache remains false. We log the exception and try host files
    	    							wLogger.error(e);
    	    						}
                    	    		// Stop looking for locations
                    	    		break;
                    	    	}
                    	    }

                    		if (!locationsInCache) {
                    			// Try if any of the object locations is in the host
                    			wLogger.debug("   - Checking if " + (String) param.getValue() + " locations are in host");
                    			NIOURI loc = param.getData().getURIinHost(host);
            	                if (loc != null) {
            	                	wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") found at host.");
            	                	try {
            	                		File source = new File(workingDir + File.separator + loc.getPath());
            	                		File target = new File(workingDir + File.separator + param.getValue().toString());
            	                		if (param.isPreserveSourceData()) {
            	                			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") preserves sources. COPYING");
            	                			wLogger.debug("         Source: " + source);
            	                			wLogger.debug("         Target: " + target);
            	                			Files.copy(source.toPath(), target.toPath());
            	                		} else {
            	                			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") erases sources. MOVING");
            	                			wLogger.debug("         Source: " + source);
            	                			wLogger.debug("         Target: " + target);
            	                			Files.move(source.toPath(), target.toPath());
            	                		}
            	                		// Move object to cache
    	    							Object o = Serializer.deserialize((String) param.getValue());
    	    							storeInCache((String) param.getValue(), o);
    	    							existInHost = true;
            	                	} catch (IOException ioe) {
            	                		// If exception is raised, existInHost remains false. We log the exception and try transfer
            	                		wLogger.error(ioe);
            	                	} catch (ClassNotFoundException e) {
            	                		// If exception is raised, existInHost remains false. We log the exception and try transfer
    	    							wLogger.error(e);
    	    						}
            	                }

                    			if (!existInHost) {
                    				// We must transfer the file
                    				askTransfer = true;
                    			}
                    		}
                    	}

                    	// Request the transfer if needed
                    	if (askTransfer) {
                    		wLogger.info("- Parameter " + i + "(" + (String) param.getValue() + ") does not exist, requesting data transfer");
                            DataRequest dr = new WorkerDataRequest(tt, param.getType(), param.getData(), (String) param.getValue());
                            addTransferRequest(dr);
                    	} else {
                    		// If no transfer, decrese the parameter counter (we already have it)
                    		 wLogger.info("- Parameter " + i + "(" + (String) param.getValue() + ") already exists.");
                    		--tt.params;
                    	}
                        break;

                    /* FILES */
                    case FILE_T:
                    	wLogger.debug("   - " + (String) param.getValue() + " registered as file.");

                        boolean exists = false;
                    	boolean locationsInHost = false;
                    	askTransfer = false;

                    	// Try if parameter is in the host
                    	wLogger.debug("   - Checking if file "+ (String) param.getValue() + " exists.");
                    	File f = new File(param.getValue().toString());
                        exists = f.exists();
                    	if (!exists) {
                    		// Try if any of the locations is in the same host
                    		wLogger.debug("   - Checking if "+ (String) param.getValue() + " exists in worker");
        	                NIOURI loc = param.getData().getURIinHost(host);
        	                if (loc != null) {
        	                	// Data is already present at host
        	                	wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") found at host.");
        	                	try {
        	                		File source = new File(workingDir + File.separator + loc.getPath());
        	                		File target = new File(workingDir + File.separator + param.getValue().toString());
        	                		if (param.isPreserveSourceData()) {
        	                			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") preserves sources. COPYING");
        	                			wLogger.debug("         Source: " + source);
        	                			wLogger.debug("         Target: " + target);
        	                			Files.copy(source.toPath(), target.toPath());
        	                		} else {
        	                			wLogger.debug("   - Parameter " + i + "(" + (String) param.getValue() + ") erases sources. MOVING");
        	                			wLogger.debug("         Source: " + source);
        	                			wLogger.debug("         Target: " + target);
        	                			Files.move(source.toPath(), target.toPath());
        	                		}
        	                		locationsInHost = true;
        	                	} catch (IOException ioe) {
        	                		wLogger.error(ioe);
        	                	}
        	                }

        	                if (!locationsInHost) {
        	                	// We must transfer the file
                				askTransfer = true;
        	                }
                    	}

                    	// Request the transfer if needed
                    	if (askTransfer) {
                    		wLogger.info("- Parameter " + i + "(" + (String) param.getValue() + ") does not exist, requesting data transfer");
                            DataRequest dr = new WorkerDataRequest(tt, param.getType(), param.getData(), (String) param.getValue());
                            addTransferRequest(dr);
                    	} else {
                    		// If no transfer, decrease the parameter counter (we already have it)
                    		 wLogger.info("- Parameter " + i + "(" + (String) param.getValue() + ") already exists.");
                    		--tt.params;
                    	}
                        break;

                    /* OTHERS: Strings or basic types */
                    default:
                    	// The master should have erased all these parameters so we are never entering this section
                    	// In any case, there is nothing to do for these type of parameters
                    	break;
                }
            } else {
            	// OUT parameter. Has no associated data. Decrease the parameter counter (we already have it)
            	--tt.params;
            }
        }

        // Request the transfers
        if (tracing) {
            NIOTracer.emitEvent(tt.task.getTaskId(), NIOTracer.getTaskTransfersType());
        }
        requestTransfers();
        if (tracing) {
            NIOTracer.emitEvent(NIOTracer.EVENT_END, NIOTracer.getTaskTransfersType());
        }


        if (tt.params == 0) {
            executeTask(tt.task);
        }

    }

    @Override
    protected void handleDataToSendNotAvailable(Connection c, Data d) {
        ErrorManager.warn("Data " + d.getName() + "in this worker " + this.getHostName() +
    					  " could not be sent to master.");
        c.finishConnection();
    }

    //This is called when the master couldn't send a data to the worker.
    //The master abruptly finishes the connection. The NIOMessageHandler handles this as an error,
    //which treats with its function handleError, and notifies the worker in this case.
    public void handleRequestedDataNotAvailableError(LinkedList<DataRequest> failedRequests, String dataId) {

        for (DataRequest dr : failedRequests) { //For every task pending on this request, flag it as an error
            WorkerDataRequest wdr = (WorkerDataRequest) dr;
            wdr.task.params--;

            //mark as an error task. When all the params've been consumed, sendTaskDone unsuccessful
            wdr.task.error = true;
            if (wdr.task.params == 0) {
            	sendTaskDone(wdr.task.task, false);
            }


            //Create job*_[NEW|RESUBMITTED|RESCHEDULED].[out|err]
            //If we don't create this when the task fails to retrieve a value,
            //the master will try to get the out of this job, and it will get blocked.
            //Same for the worker when sending, throwing an error when trying to read the job out, which wouldnt exist

            String baseJobPath = workingDir + "/jobs/job" + wdr.task.task.getJobId() + "_" + wdr.task.task.getHist();
        	File fout = new File(baseJobPath + ".out");
        	File ferr = new File(baseJobPath + ".err");
            if(!fout.exists() || !ferr.exists()) {
	        	FileOutputStream fos = null;
	            try {
	            	String errorMessage = "Worker closed because the data " + dataId + " couldn't be retrieved.";
					fos = new FileOutputStream(fout);
					fos.write(errorMessage.getBytes());
					fos.close();
					fos = new FileOutputStream(ferr);
					fos.write(errorMessage.getBytes());
					fos.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
	            finally {
	            	if(fos != null) {
	            		try {
							fos.close();
						} catch (IOException e) {
							e.printStackTrace();
				} } } }
        }

    }

    @Override
    public void receivedValue(Destination type, String dataId, Object object, LinkedList<DataRequest> achievedRequests) {
        if (type == Transfer.Destination.OBJECT) {
        	wLogger.info("Received data " + dataId + " with associated object " + object);
            storeInCache(dataId, object);
        } else {
        	wLogger.info("Received data " + dataId);
        }
        for (DataRequest dr : achievedRequests) {
            WorkerDataRequest wdr = (WorkerDataRequest) dr;
            wdr.task.params--;
            if(tracing){
                NIOTracer.emitDataTransferEvent(NIOTracer.TRANSFER_END);
            }
            if (wdr.task.params == 0) {
            	if(!wdr.task.error) {
            		executeTask(wdr.task.task);
            	} else {
                	sendTaskDone(wdr.task.task, false);
            	}
            }
        }
    }

    public void sendTaskDone(NIOTask nt, boolean successful){
        int taskID = nt.getJobId();

        // Notify task done
        int retries = 0;
        Connection c = null;
        while (retries< MAX_RETRIES){
        	try{
        		c = tm.startConnection(masterNode);
        		break;
        	}catch (Exception e){
        		if (retries >= MAX_RETRIES){
        			wLogger.error("Exception sending Task notification", e);
        			return;
        		}else{
        			try {
        				Thread.sleep(10);
        			} catch (InterruptedException e1) {
        				// Nothing to do
        			}
        			retries++;
        		}
        	}
        }
        CommandTaskDone cmd = new CommandTaskDone(this, taskID, successful);
        c.sendCommand(cmd);
        if (workerDebug) {
            c.sendDataFile(workingDir + "/jobs/job" + nt.getJobId() + "_" + nt.getHist() + ".out");
            c.sendDataFile(workingDir + "/jobs/job" + nt.getJobId() + "_" + nt.getHist() + ".err");
        } else {
            if (!successful) {
                c.sendDataFile(workingDir + "/jobs/job" + nt.getJobId() + "_" + nt.getHist() + ".out");
                c.sendDataFile(workingDir + "/jobs/job" + nt.getJobId() + "_" + nt.getHist() + ".err");
            }
        }
        c.finishConnection();
    }

    // Check if this task is ready to execute
    private void executeTask(NIOTask task) {
        if(workerDebug){
        	wLogger.debug("Enqueueing job "+ task.getJobId()+" for execution.");
        }

        // Execute the job
        jobQueue.enqueue(task);

        // Notify the master that the data has been transfered
        // The message is sent after the task enqueue because the connection can have N pending task transfer and will wait until they
        // are finished to send all the answers (blocking the task execution)
        if(workerDebug){
        	wLogger.debug("Notifying presence of all data for job " + task.getJobId()+".");
        }

        CommandDataReceived cdr = new CommandDataReceived(this, task.getTransferGroupId());
        for (int retries = 0; retries < MAX_RETRIES; retries++){
            if (tryNofiyDataReceived(cdr)) {
                return;
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Nothing to do
                }
            }
        }
    }

    private boolean tryNofiyDataReceived(CommandDataReceived cdr) {
        Connection c = tm.startConnection(masterNode);
        c.sendCommand(cdr);
        c.finishConnection();

        return true;
    }

	// Remove obsolete files and objects
    public void removeObsolete(LinkedList<String> obsolete) {
        try {
            for (String name : obsolete) {
                if (name.startsWith(File.separator)) {
                    File f = new File(name);
                    f.delete();
                } else {
                    removeFromCache(name);
                }
            }
        } catch (Exception e) {
        	wLogger.error(e);
        }
    }

    public void receivedUpdateSources(Connection c) {

    }

    // Shutdown the worker, at this point there are no active transfers
    public void shutdown(Connection closingConnection) {
        
    	wLogger.debug("Entering shutdown method on worker");
        try {
            // Stop the job threads
            pool.stopThreads();

            // Finish the main thread
            if (closingConnection != null) {
            closingConnection.sendCommand(new CommandShutdownACK());
            closingConnection.finishConnection();
            }
            tm.shutdown(closingConnection);
        } catch (Exception e) {
        	wLogger.error(e);
        }
        wLogger.debug("Finish shutdown method on worker");
        
    }

    public Object getObject(String s) throws SerializedObjectException {
        String realName = s.substring(s.lastIndexOf('/') + 1);
        return objectCache.get(realName);
    }

    public String getObjectAsFile(String s) {
    	// This method should never be called in the worker side
    	wLogger.warn("getObjectAsFile has been called in the worker side!");

    	return null;
    }

    public void storeInCache(String name, Object value) {
        objectCache.store(name, value);
    }

    public void removeFromCache(String name) {
        objectCache.remove(name);
    }

    public String getWorkingDir() {
        return workingDir + File.separator;
    }

    // args: debug workingDir numThreads maxSnd maxRcv
    public static void main(String[] args) {
               
        // Get args
        workerDebug = Boolean.valueOf(args[0]);
        workingDir = args[1];
        jobThreads = new Integer(args[2]);
        int maxSnd = new Integer(args[3]);
        int maxRcv = new Integer(args[4]);
        String workerIP = args[5];
        int wPort = new Integer(args[6]);
        int mPort = new Integer(args[7]);
        String trace = args[8];
        String host = args[9];
        String installDir = args[10];
        String appUuid = args[11];


        // Configure tracing
        System.setProperty(ITConstants.IT_TRACING, trace);
        tracing = Integer.parseInt(trace) > 0;
        tracing_level = Integer.parseInt(trace);

        /* Init tracing system */
        if (tracing){
            NIOTracer.emitEvent(NIOTracer.Event.START.getId(), NIOTracer.Event.START.getType());

            try {
                tracingID = Integer.parseInt(host);
                NIOTracer.setWorkerInfo(installDir , workerIP, workingDir, tracingID);
            } catch (Exception e) {
                logger.error("No valid hostID provided to the tracing system. Provided ID: " + host);
            }
        }

        // Configure worker logger since it doesn't receive the it-log4j file
        ConsoleAppender console = new ConsoleAppender();
        Logger.getRootLogger().setLevel(workerDebug ? Level.DEBUG : Level.OFF);
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        // Log args
        if (workerDebug) {
        	wLogger.debug("WorkingDir:" + workingDir);
        	wLogger.debug("jobThreads: " + String.valueOf(jobThreads));
        	wLogger.debug("maxSnd: " + String.valueOf(maxSnd));
        	wLogger.debug("maxRcv: " + String.valueOf(maxRcv));
        	wLogger.debug("WorkerName: " + workerIP);
        	wLogger.debug("WorkerPort: " + String.valueOf(wPort));
        	wLogger.debug("MasterPort: " + String.valueOf(mPort));
        	wLogger.debug("Tracing: " + trace);
        	wLogger.debug("Host: " + host);
        	wLogger.debug("Install Dir: " + installDir);
        	wLogger.debug("App uuid: " + appUuid);
        }

        NIOWorker nw = new NIOWorker(appUuid, maxSnd, maxRcv, mPort, workerIP);
        NIOMessageHandler mh = new NIOMessageHandler(nw);


        // Init the Transfer Manager
        wLogger.debug("  Initializing the TransferManager structures...");
        try {
            tm.init(NIOEventManagerClass, null, mh);
        } catch (CommException ce) {
            wLogger.error("Error initializing Transfer Manager on worker " + nw.getHostName(), ce);
            // Shutdown the Worker since the error it is not recoverable
            nw.shutdown(null);
            return;
        }

        // Start the Transfer Manager thread (starts the EventManager)
        wLogger.debug("  Starting TransferManager Thread");
        tm.start();
        try {
            tm.startServer(new NIONode(null, wPort));
        } catch (CommException ce) {
            wLogger.error("Error starting TransferManager Server at Worker" + nw.getHostName(), ce);
            nw.shutdown(null);
            return;
        }
        
        if (tracing){
            NIOTracer.emitEvent(NIOTracer.EVENT_END, NIOTracer.Event.START.getType());
        }

        // Wait for the Transfer Manager thread to finish (the shutdown is received on that thread)
        try {
            tm.join();
        } catch (InterruptedException ie) {
            wLogger.warn("TransferManager interrupted", ie);
        }
    }

    public static void registerOutputs(String path) {
        err.registerThread(path);
        out.registerThread(path);
    }

    public static void unregisterOutputs() {
        err.unregisterThread();
        out.unregisterThread();
    }

    @Override
    public void receivedTaskDone(Connection c, int jobID, boolean successful) {
        //Should not receive this call
    }

    @Override
    public void copiedData(int transfergroupID) {
        //Should not receive this call
    }

    @Override
    public void shutdownNotification(Connection c) {
        //Never orders the shutdown of a worker peer
    }

    public String getHostName(){
        return this.host;
    }

	@Override
	public void waitUntilTracingPackageGenerated() {
		// Nothing to do

	}

	@Override
	public void notifyTracingPackageGeneration() {
		// Nothing to do
	}

	@Override
	public void waitUntilWorkersDebugInfoGenerated() {
		// Nothing to do

	}

	@Override
	public void notifyWorkersDebugInfoGeneration() {
		// Nothing to do
	}

	@Override
    public void generateWorkersDebugInfo(Connection c) {
		// Freeze output
		String outSource = workingDir + File.separator + "log" + File.separator + "worker_" + host + ".out" ;
		String outTarget = workingDir + File.separator + "log" + File.separator + "static_" + "worker_" + host + ".out";
		if (new File(outSource).exists()) {
			try {
				Files.copy(new File(outSource).toPath(), new File(outTarget).toPath());
			} catch (Exception e) {
				logger.error(e);
			}
		} else {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(outTarget);
				fos.write("Empty file".getBytes());
				fos.close();
			} catch (Exception e) {
				logger.error(e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}
		}

		// Freeze error
		String errSource = workingDir + File.separator + "log" + File.separator + "worker_" + host + ".err" ;
		String errTarget = workingDir + File.separator + "log" + File.separator + "static_" + "worker_" + host + ".err";
		if (new File(errSource).exists()) {
			try {
				Files.copy(new File(errSource).toPath(), new File(errTarget).toPath());
			} catch (Exception e) {
				logger.error(e);
			}
		} else {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(errTarget);
				fos.write("Empty file".getBytes());
				fos.close();
			} catch (Exception e) {
				logger.error(e);
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) {
						logger.error(e);
					}
				}
			}
		}

		// End
        c.sendCommand(new CommandWorkerDebugFilesDone());
        c.finishConnection();
    }


    private class WorkerDataRequest extends DataRequest {

        private final TransferringTask task;

        public WorkerDataRequest(TransferringTask task, ParamType type, Data source, String target) {
            super(type, source, target);
            this.task = task;
        }

    }

    private static class TransferringTask {

        NIOTask task;
        int params;
        boolean error;

        public TransferringTask(NIOTask task) {
            this.task = task;
            params = task.getParams().size();
        }
    }

}
