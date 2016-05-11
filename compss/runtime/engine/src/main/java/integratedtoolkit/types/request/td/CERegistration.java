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
package integratedtoolkit.types.request.td;

import integratedtoolkit.components.impl.JobManager;
import integratedtoolkit.components.impl.TaskScheduler;
import integratedtoolkit.types.resources.MethodResourceDescription;
import integratedtoolkit.util.CoreManager;
import java.util.concurrent.Semaphore;

import integratedtoolkit.types.MethodImplementation;
import integratedtoolkit.types.Implementation;
import integratedtoolkit.util.ResourceManager;
import java.util.LinkedList;


public class CERegistration extends TDRequest {

    private Semaphore sem;
    private final String signature;
    private final String declaringClass;
    private final MethodResourceDescription mrd;
    
    public CERegistration(String signature, String declaringClass, MethodResourceDescription mrd, Semaphore sem) {
        this.signature = signature;
        this.declaringClass = declaringClass;
        this.mrd = mrd;
        this.sem = sem;
    }

    /**
     * Returns the semaphore where to synchronize until the operation is done
     *
     * @return Semaphore where to synchronize until the operation is done
     */
    public Semaphore getSemaphore() {
        return sem;
    }

    /**
     * Sets the semaphore where to synchronize until the operation is done
     *
     * @param sem Semaphore where to synchronize until the operation is done
     */
    public void setSemaphore(Semaphore sem) {
        this.sem = sem;
    }

    @Override
    public void process(TaskScheduler ts, JobManager jm) {
        int coreId = CoreManager.getCoreId(new String[]{signature});

        int implementationId = 0; // python can just have 1 implementation due to lack of interfaces
        
        MethodImplementation me = new MethodImplementation(declaringClass, coreId, implementationId, mrd);
        Implementation<?>[] impls = new Implementation[]{me};
        
        CoreManager.registerImplementations (coreId, impls);
        
        LinkedList<Integer> newCores = new LinkedList<Integer>();
        newCores.add(coreId);
        
        ResourceManager.coreElementUpdates(newCores);
        
        ts.resizeDataStructures();
        
        logger.debug("Data structures resized and CE-resources links updated");
        sem.release();
    }

    @Override
    public TDRequestType getRequestType() {
        return TDRequestType.UPDATE_LOCAL_CEI;
    }

}
