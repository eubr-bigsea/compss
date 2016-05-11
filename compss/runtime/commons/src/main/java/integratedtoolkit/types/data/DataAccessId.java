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
package integratedtoolkit.types.data;

import java.io.Serializable;


public abstract class DataAccessId implements Serializable {
	
	/**
	 * Serializable objects Version UID are 1L in all Runtime
	 */
	private static final long serialVersionUID = 1L;

    public abstract int getDataId();

    // Read access
    public static class RAccessId extends DataAccessId {
    	/**
    	 * Serializable objects Version UID are 1L in all Runtime
    	 */
    	private static final long serialVersionUID = 1L;
    	
		// File version read
        private DataInstanceId readDataInstance;
        // Source data preservation flag
        private boolean preserveSourceData = true;

        public RAccessId() {
        }

        public RAccessId(int dataId, int rVersionId) {
            this.readDataInstance = new DataInstanceId(dataId, rVersionId);
        }

        public RAccessId(DataInstanceId rdi) {
            this.readDataInstance = rdi;
        }

        public int getDataId() {
            return readDataInstance.getDataId();
        }

        public int getRVersionId() {
            return readDataInstance.getVersionId();
        }

        public DataInstanceId getReadDataInstance() {
            return readDataInstance;
        }
        
        public boolean isPreserveSourceData() {
        	return preserveSourceData;
        }

        public String toString() {
            return "Read data: " + readDataInstance
            		+ (preserveSourceData ? ", Preserved" : ", Erased");
        }

    }

    // Write access
    public static class WAccessId extends DataAccessId {
    	/**
    	 * Serializable objects Version UID are 1L in all Runtime
    	 */
    	private static final long serialVersionUID = 1L;

        // File version written
        private DataInstanceId writtenDataInstance;

        public WAccessId() {
        }

        public WAccessId(int dataId, int wVersionId) {
            this.writtenDataInstance = new DataInstanceId(dataId, wVersionId);
        }

        public WAccessId(DataInstanceId wdi) {
            this.writtenDataInstance = wdi;
        }

        public int getDataId() {
            return writtenDataInstance.getDataId();
        }

        public int getWVersionId() {
            return writtenDataInstance.getVersionId();
        }

        public DataInstanceId getWrittenDataInstance() {
            return writtenDataInstance;
        }

        public String toString() {
            return "Written data: " + writtenDataInstance;
        }

    }

    // Read-Write access
    public static class RWAccessId extends DataAccessId {
    	/**
    	 * Serializable objects Version UID are 1L in all Runtime
    	 */
    	private static final long serialVersionUID = 1L;

        // File version read
        private DataInstanceId readDataInstance;
        // File version written
        private DataInstanceId writtenDataInstance;
        // Source data preservation flag
        private boolean preserveSourceData = false;

        public RWAccessId() {
        }

        public RWAccessId(DataInstanceId rdi, DataInstanceId wdi, boolean preserveSourceData) {
            this.readDataInstance = rdi;
            this.writtenDataInstance = wdi;
            this.preserveSourceData = preserveSourceData;
        }

        public int getDataId() {
            return readDataInstance.getDataId();
        }

        public int getRVersionId() {
            return readDataInstance.getVersionId();
        }

        public int getWVersionId() {
            return writtenDataInstance.getVersionId();
        }

        public DataInstanceId getReadDataInstance() {
            return readDataInstance;
        }

        public DataInstanceId getWrittenDataInstance() {
            return writtenDataInstance;
        }
        
        public boolean isPreserveSourceData() {
        	return preserveSourceData;
        }

        public String toString() {
            return "Read data: " + readDataInstance 
            		+ ", Written data: " + writtenDataInstance 
            		+ (preserveSourceData ? ", Preserved" : ", Erased");
        }
        
    }

}
