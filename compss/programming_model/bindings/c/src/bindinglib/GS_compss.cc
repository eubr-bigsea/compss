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


#include "GS_templates.h"


using namespace std;
using namespace boost;

map<void *, Entry> objectMap;

void compss_on(void) {
	GS_On();
}

void compss_off(void) {
	GS_clean();
	GS_Off();
}

void GS_clean() {
	  std::map<void *, Entry>::iterator it;

	  for (std::map<void *,Entry>::iterator it=objectMap.begin(); it!=objectMap.end(); ++it) {
		remove (it->second.filename);
	  }
}

int GS_register(void *ref, datatype type, direction dir, char *classname, char * &filename) {
	 Entry entry;
	 int result = 1;

	 debug_printf("[   BINDING]  -  @GS_register  -  Ref: %p\n", (char *)ref);

	 if (dir == null_dir) {
		 dir = out_dir;
	 }

	 if (dir != in_dir) {

		 entry = objectMap[ref];

		 if (entry.filename == NULL) {
			 debug_printf("[   BINDING]  -  @GS_register  -  ENTRY ADDED\n");

			 entry.type = type;
			 entry.classname = strdup(classname);

			 if ((datatype)entry.type != file_dt) {
			 	entry.filename =  strdup("compss-serialized-obj_XXXXXX");
			 	result = mkstemp(entry.filename);
			 } else {
				entry.filename = strdup(filename);
			 }

			 objectMap[ref] = entry;

			 result = 0;

		 } else {
			 debug_printf("[   BINDING]  -  @GS_register  -  ENTRY FOUND\n");
		 }

		 debug_printf("[   BINDING]  -  @GS_register  -  Entry.type: %d\n", entry.type);
		 debug_printf("[   BINDING]  -  @GS_register  -  Entry.classname: %s\n", entry.classname);
		 debug_printf("[   BINDING]  -  @GS_register  -  Entry.filename: %s\n", entry.filename);

		 filename = strdup(entry.filename);

	 } else {
                 if ((datatype)entry.type != file_dt) {		 
			 filename = strdup("compss-serialized-obj_XXXXXX");
			 result = mkstemp(filename);
		 }

		 result = 0;
	 }

  	 return result;
 }
