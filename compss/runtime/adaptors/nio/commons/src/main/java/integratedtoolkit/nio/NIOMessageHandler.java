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

import es.bsc.comm.CommException;
import es.bsc.comm.Connection;
import es.bsc.comm.MessageHandler;
import es.bsc.comm.nio.NIOException;
import es.bsc.comm.stage.Transfer;
import integratedtoolkit.log.Loggers;
import integratedtoolkit.nio.commands.Command;

import org.apache.log4j.Logger;


public class NIOMessageHandler implements MessageHandler {

    protected static final Logger LOGGER = Logger.getLogger(Loggers.COMM);

    private final NIOAgent agent;

    public NIOMessageHandler(NIOAgent agent) {
        this.agent = agent;
    }

    @Override
    public void init() throws CommException {
    	// The class has all its paramters
    	// The server has been initialized by the TransferManager
    	// Nothing to do
    }

    @Override
    public void errorHandler(Connection c, Transfer t, CommException ce) {
    	LOGGER.debug("Received error type: " + ((NIOException)ce).getError());
        String errorText = "NIO Error: " + ce.getMessage() + " processing " + ((t == null)? "null":t.hashCode()) + "\n";
        LOGGER.error(errorText, ce);

    	agent.receivedRequestedDataNotAvailableError(c, t);
    	
        // Handle FINISH_CONNECTION and CLOSED CONNECTION errors  
        /*if (((NIOException)ce).getError() == ErrorType.FINISHING_CONNECTION || ((NIOException)ce).getError() == ErrorType.CLOSED_CONNECTION) {
        	c.finishConnection();
        }
        */
    }

    @Override
    public void dataReceived(Connection c, Transfer t) {
        LOGGER.debug("Received data " + (t.isFile() ? t.getFileName() : t.getObject()) + " through connection " + c.hashCode());
        agent.receivedData(c, t);
    }

    @Override
    public void commandReceived(Connection c, Transfer t) {
        try {
            Command cmd = (Command) t.getObject();
            LOGGER.debug("Received Command " + cmd + " through connection " + c.hashCode());
            cmd.agent = agent;
            cmd.handle(c);
        } catch (Exception e) {
            LOGGER.error("Error receving command. Finishing connection.", e);
            c.finishConnection();
        }
    }

    @Override
    public void writeFinished(Connection c, Transfer t) {
        LOGGER.debug("Finished sending " + (t.isFile() ? t.getFileName() : t.getObject()) + " through connection " + c.hashCode());
        agent.releaseSendSlot(c);
    }

    @Override
    public void connectionFinished(Connection c) {
        LOGGER.debug("Connection " + c.hashCode() + " finished");
    }

    @Override
    public void shutdown() {

    }

}
