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

import integratedtoolkit.log.Loggers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;


public class VMMClient {
	
	private Client client;
    private WebResource resource;
    private static final Logger logger = Logger.getLogger(Loggers.TS_COMP);
    
	/**
	 * 
	 */
	public VMMClient(String url) {
		super();
		this.client = new Client();
	    this.resource = client.resource(url);
	    
	}
	
	public String createVM(String name, String image, int cpus, int ramMb, int diskGb, String applicationId) throws Exception{
		VMRequest vm = new VMRequest(name, image, cpus, ramMb, diskGb, applicationId);
		VMs vms = new VMs();
		vms.getVms().add(vm);
		JSONObject obj = new JSONObject(vms);
		logger.debug("Submitting vm creation ...");
		ClientResponse cr = resource.path("vms").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, obj.toString());
		if (cr.getStatus()== Status.OK.getStatusCode()){
			String s = cr.getEntity(String.class);
			JSONObject res = new JSONObject(s);
			String id = (String)res.getJSONArray("ids").getJSONObject(0).get("id");
			logger.debug("VM submitted with id " + id);
			return id;
		}else{
			logger.error("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase());
			throw(new Exception("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase()));
		}
		
		
	}
	
	public VMDescription getVMDescription(String id) throws Exception{
		logger.debug("Getting vm description ...");
		ClientResponse cr = resource.path("vms").path(id).get(ClientResponse.class);
		if (cr.getStatus()== Status.OK.getStatusCode()){
			String s = cr.getEntity(String.class);
			logger.debug("Obtained description " + s);
			return new VMDescription(new JSONObject(s));
			
		}else{
			logger.error("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase());
			throw(new Exception("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase()));
		}
	}

	/*public void stopVM(String vmId) throws Exception {
		logger.debug("Getting vm description ...");
		JSONObject res = new JSONObject();
		res = res.put("action", "stop");
		ClientResponse cr = resource.path("vms").path(vmId).put(ClientResponse.class, res.toString());
		if (cr.getStatus() != Status.NO_CONTENT.getStatusCode()){
			logger.error("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase());
			throw(new Exception("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase()));
		}
		
	}*/

	public void deleteVM(String vmId) throws Exception {
		logger.debug("Getting vm description ...");
		
		ClientResponse cr = resource.path("vms").path(vmId).delete(ClientResponse.class);
		if (cr.getStatus() != Status.NO_CONTENT.getStatusCode()){
			logger.error("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase());
			throw(new Exception("Incorrect return code: "+cr.getStatus()+"."+cr.getClientResponseStatus().getReasonPhrase()));
		}
	}
		

}
