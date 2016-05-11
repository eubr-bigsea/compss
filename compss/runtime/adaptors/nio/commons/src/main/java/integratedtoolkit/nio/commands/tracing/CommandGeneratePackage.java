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
package integratedtoolkit.nio.commands.tracing;

import integratedtoolkit.nio.commands.Command;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import es.bsc.comm.Connection;


public class CommandGeneratePackage extends Command implements Externalizable {

	private String host;
	private String installDir;
	private String workingDir;
	private String name;

	public CommandGeneratePackage() {
		super();
	}
	
	public CommandGeneratePackage(String host, String installDir,
			String workingDir, String name) {
		super();
		this.host=host;
		this.installDir=installDir;
		this.workingDir=workingDir;
		this.name=name;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		host = (String)in.readObject();
		installDir = (String)in.readObject();
		workingDir = (String)in.readObject();
		name = (String)in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(host);
		out.writeObject(installDir);
		out.writeObject(workingDir);
		out.writeObject(name);
		
	}

	@Override
	public CommandType getType() {
		return CommandType.GEN_TRACE_PACKAGE;
	}

	@Override
	public void handle(Connection c) {
		agent.generatePackage(c, host, installDir, workingDir, name);
		
	}

}
