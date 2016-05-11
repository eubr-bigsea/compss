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
package integratedtoolkit.nio.commands;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import integratedtoolkit.nio.NIOAgent;
import es.bsc.comm.Connection;

public class CommandDataDemand extends Command implements Externalizable {

    private Data d;
    private int id;
    
    public CommandDataDemand() {
        super();
    }

    public CommandDataDemand(NIOAgent ng, Data d, int receiverID) {
        super(ng);
        this.d = d;
        this.id = receiverID;
    }

    @Override
    public CommandType getType() {
        return CommandType.DATA_DEMAND;
    }

    @Override
    public void handle(Connection c) {
        boolean slot = agent.tryAcquireSendSlot(c);
        if (!slot) {// There are no slots available
            //TODO: ENABLE DATA NEGATE COMMANDS
            agent.sendData(c, d, id);
            //agent.sendDataNegate(c, d, true);
        } else {        // There is a slot and the data exists
            agent.sendData(c, d, id);
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        d = (Data) in.readObject();
        id = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(d);
        out.writeInt(id);
    }

    public String toString() {
        return "Request for sending data " + d;
    }
}
