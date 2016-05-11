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
package integratedtoolkit.connectors.vmm;

import java.util.HashMap;

import integratedtoolkit.ITConstants;
import integratedtoolkit.connectors.AbstractSSHConnector;
import integratedtoolkit.connectors.ConnectorException;
import integratedtoolkit.types.resources.description.CloudMethodResourceDescription;


public class VMMConnector extends AbstractSSHConnector {
	
	private static final String ENDPOINT_PROP = "Server";
    private static final String ACTIVE = "ACTIVE";
    private static final String ERROR = "ERROR";
    private static final long POLLING_INTERVAL = 5;
    private static final int TIMEOUT = 1800;
    private VMMClient client;
    
    public VMMConnector(String providerName, HashMap<String, String> props) {
		super(providerName, props);
		this.client = new VMMClient(props.get(ENDPOINT_PROP));
	}
    
	@Override
	public void destroy(Object vm) throws ConnectorException {
		String vmId = (String) vm;
        try{
			client.deleteVM(vmId);
        }catch(Exception e){
        	logger.error("Exception waiting for VM Creation");
            throw new ConnectorException(e);

        }
	}

	@Override
	public Object create(String name, CloudMethodResourceDescription requested)
			throws ConnectorException {
		try {
			logger.debug("Image password:" + requested.getImage().getProperties().get(ITConstants.PASSWORD));
			String id =  client.createVM(name, requested.getImage().getName(), 
   				 requested.getProcessorCoreCount(), 
   				 (int)(requested.getMemoryPhysicalSize()*1000), 
   				 (int)requested.getStorageElemSize(), 
   				 System.getProperty(ITConstants.IT_APP_NAME));
		logger.debug("Machine "+ id + " created");
   		return id; 
       } catch (Exception e) {
           logger.error("Exception submitting vm creation", e);           
           throw new ConnectorException(e);
       }
	}

	@Override
	public CloudMethodResourceDescription waitUntilCreation(Object vm,
			CloudMethodResourceDescription requested) throws ConnectorException {
		CloudMethodResourceDescription granted = new CloudMethodResourceDescription();
        String vmId = (String) vm;
        try {
			VMDescription vmd = client.getVMDescription(vmId);
			logger.info("VM State is "+vmd.getState());
			int tries = 0;
			while (vmd.getState() == null || !vmd.getState().equals(ACTIVE)) {
				if (vmd.getState().equals(ERROR)) {
					logger.error("Error waiting for VM Creation. Middleware has return an error state");
					throw new ConnectorException("Error waiting for VM Creation. Middleware has return an error state");
				}
				if (tries * POLLING_INTERVAL > TIMEOUT) {
					throw new ConnectorException("Maximum VM creation time reached.");
				}

				tries++;

				try {
					Thread.sleep(POLLING_INTERVAL * 1000);
				} catch (InterruptedException e) {
					// Ignore
				}
				vmd = client.getVMDescription(vmId);
			}

			granted.setName(vmd.getIpAddress());

			granted.setType(requested.getType());
			granted.setProcessorCPUCount(requested.getProcessorCPUCount());
			granted.setProcessorCoreCount(vmd.getCpus());
			granted.setProcessorArchitecture(requested.getProcessorArchitecture());
			granted.setProcessorSpeed(requested.getProcessorSpeed());
			granted.setMemoryPhysicalSize(requested.getMemoryPhysicalSize());
			granted.setMemoryAccessTime(requested.getMemoryAccessTime());
			granted.setMemorySTR(requested.getMemorySTR());
			granted.setMemoryVirtualSize(requested.getMemoryVirtualSize());
			granted.setStorageElemSize(vmd.getDiskGb());
			granted.setStorageElemAccessTime(requested.getStorageElemAccessTime());
			granted.setStorageElemSTR(requested.getStorageElemSTR());

			granted.setOperatingSystemType("Linux");
			granted.setSlots(requested.getSlots());

			granted.getAppSoftware().addAll(requested.getAppSoftware());
			granted.setImage(requested.getImage());
			granted.setValue(requested.getValue());
			granted.setValue(getMachineCostPerTimeSlot(granted));
			return granted;
        } catch(Exception e) {
        	logger.error("Exception waiting for VM Creation");
            throw new ConnectorException(e);
        }
			
	}

	@Override
	public float getMachineCostPerTimeSlot(CloudMethodResourceDescription rd) {
		return rd.getValue();
	}

	@Override
	public long getTimeSlot() {
		return 0;
	}
	
	@Override
	protected void close(){
	    	//Nothing to do;
	}

}
