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
package integratedtoolkit.types.parameter;

import integratedtoolkit.api.ITExecution.*;


public class BasicTypeParameter extends Parameter {
    /* Basic type parameter can be:
     * - boolean
     * - char
     * - String
     * - byte
     * - short
     * - int
     * - long
     * - float
     * - double
     */
	
	/**
	 * Serializable objects Version UID are 1L in all Runtime
	 */
	private static final long serialVersionUID = 1L;
	
    private Object value;

    public BasicTypeParameter(ParamType type, ParamDirection direction, Object value) {
        super(type, direction);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toString() {
        return value + " " + getType() + " " + getDirection();
    }
    
}
