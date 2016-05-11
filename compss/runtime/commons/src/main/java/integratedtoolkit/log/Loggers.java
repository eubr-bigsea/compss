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
package integratedtoolkit.log;

import integratedtoolkit.types.exceptions.NonInstantiableException;


public final class Loggers {

    // Integrated Toolkit
    public static final String IT = "integratedtoolkit";

    // Loader
    public static final String LOADER = IT + ".Loader";
    public static final String LOADER_UTILS = IT + ".LoaderUtils";

    // API
    public static final String API = IT + ".API";

    //Resources
    public static final String RESOURCES = IT + ".Resources";

    // Components
    public static final String ALL_COMP = IT + ".Components";

    public static final String TP_COMP = ALL_COMP + ".TaskProcessor";
    public static final String TD_COMP = ALL_COMP + ".TaskDispatcher";
    public static final String RM_COMP = ALL_COMP + ".ResourceManager";

    public static final String TA_COMP = TP_COMP + ".TaskAnalyser";
    public static final String DIP_COMP = TP_COMP + ".DataInfoProvider";

    public static final String TS_COMP = TD_COMP + ".TaskScheduler";
    public static final String JM_COMP = TD_COMP + ".JobManager";
    public static final String FTM_COMP = TD_COMP + ".FileTransferManager";

    public static final String CONNECTORS = IT + ".Connectors";
    public static final String CONNECTORS_IMPL = IT + ".ConnectorsImpl";
    
    public static final String ERROR_MANAGER = ALL_COMP + ".ErrorManager";

    // Worker
    public static final String WORKER = IT + ".Worker";

    // Communications
    public static final String COMM = IT + ".Communication";

    private Loggers() {
        throw new NonInstantiableException("Loggers should not be instantiated");
    }

}
