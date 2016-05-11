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
package integratedtoolkit.nio.master.handlers;

import integratedtoolkit.nio.master.NIOWorkerNode;
import integratedtoolkit.nio.master.WorkerStarter;


public class Ender extends Thread {
	
    private NIOWorkerNode node;
    private int pid;

    public Ender(NIOWorkerNode node, int pid) {
        this.node = node;
        this.pid = pid;
    }

    public void run() {
    	WorkerStarter.ender(node, pid);
    }
    
}
