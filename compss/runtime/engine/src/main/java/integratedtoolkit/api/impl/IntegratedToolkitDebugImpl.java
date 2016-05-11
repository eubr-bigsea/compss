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
package integratedtoolkit.api.impl;

import integratedtoolkit.ITConstants;
import integratedtoolkit.api.ITDebug;
import integratedtoolkit.components.impl.RuntimeMonitor;
import integratedtoolkit.components.impl.debug.TaskDispatcherDebug;
import integratedtoolkit.components.impl.debug.AccessProcessorDebug;
import integratedtoolkit.util.ErrorManager;
import integratedtoolkit.util.Tracer;


public class IntegratedToolkitDebugImpl extends IntegratedToolkitImpl implements ITDebug {

    // Components
    AccessProcessorDebug ap;

    public IntegratedToolkitDebugImpl() {
        super();
    }

    // Integrated Toolkit user interface implementation
    public void startIT() {
        if (tracing){
            Tracer.masterEventFinish();
            Tracer.masterEventStart(Tracer.Event.START.getId());
        }
        
        // Console Log
        Thread.currentThread().setName("APPLICATION");
        if (COMPSs_VERSION == null) {
            logger.info("Starting COMPSs Runtime");
        } else if (COMPSs_BUILDNUMBER == null) {
        	logger.info("Starting COMPSs Runtime v" + COMPSs_VERSION);
        } else if (COMPSs_BUILDNUMBER.endsWith("rnull")) {
        	COMPSs_BUILDNUMBER = COMPSs_BUILDNUMBER.substring(0, COMPSs_BUILDNUMBER.length() - 6);
        	logger.info("Starting COMPSs Runtime v" + COMPSs_VERSION + " (build " + COMPSs_BUILDNUMBER + ")");
        } else {
        	logger.info("Starting COMPSs Runtime v" + COMPSs_VERSION + " (build " + COMPSs_BUILDNUMBER + ")");
        }

        // Init Runtime
        if (!initialized) {
        	// Application
            logger.debug("Initializing components");
            td = new TaskDispatcherDebug();
            ap = new AccessProcessorDebug();
            IntegratedToolkitImpl.ap = ap;
            if (RuntimeMonitor.isEnabled()) {
                monitor = new RuntimeMonitor(ap, td, Long.parseLong(System.getProperty(ITConstants.IT_MONITOR)));
            }
            ap.setTD(td);
            td.setTP(ap);
            initialized = true;
            logger.debug("Ready to process tasks");
        } else {
        	// Service
            String className = Thread.currentThread().getStackTrace()[2].getClassName();
            logger.debug("Initializing " + className + "Itf");
            try {
                td.addInterface(Class.forName(className + "Itf"));
            } catch (Exception e) {
            	ErrorManager.fatal("Error adding interface " + className + "Itf");
            }
        }
        
        if (tracing){
            Tracer.masterEventFinish();
        }
    }

}
