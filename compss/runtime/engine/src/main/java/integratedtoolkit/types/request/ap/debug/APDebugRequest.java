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
package integratedtoolkit.types.request.ap.debug;

import integratedtoolkit.types.request.ap.APRequest;


/**
 * The TPRequest class represents any interaction with the TaskProcessor
 * component.
 */
public abstract class APDebugRequest extends APRequest {

    /**
     * Contains the different types of request that the Task Processor can
     * response.
     */
    public enum DebugRequestType {

    }

    @Override
    public APRequestType getRequestType() {
        return APRequestType.DEBUG;
    }

    public abstract DebugRequestType getDebugRequestType();
}
