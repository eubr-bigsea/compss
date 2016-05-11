#
#  Copyright 2002-2015 Barcelona Supercomputing Center (www.bsc.es)
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
'''
@author: etejedor
'''

### HELPER FUNCTIONS ###

def initialize_variables():
    import numpy as np
    for matrix in [A,B,C]:
        for i in range(MSIZE):
            matrix.append([])
            for j in range(MSIZE):
                if matrix == C:
                    block = np.array(np.zeros((BSIZE, BSIZE)), dtype=np.double, copy=False)
                else:
                    #aux = np.zeros((BSIZE, BSIZE))
                    #aux.fill(2.0)
                    #block = np.array(aux, dtype=np.double, copy=False)
                    block = np.array(np.random.random((BSIZE, BSIZE)), dtype=np.double, copy=False)
                mb = np.matrix(block, dtype=np.double, copy=False)
                matrix[i].append(mb)




### TASK SELECTION ###

from pycompss.api.task import task
from pycompss.api.parameter import *

@task(c = INOUT)
def multiply(a, b, c):
    #import time
    #start = time.time()

    import numpy as np
    #np.show_config()

    c += a*b

    #end = time.time()
    #tm = end - start
    #print "TIME: " + str(tm*1000) + " msec"



### MAIN PROGRAM ###

if __name__ == "__main__":
    
    import sys
    import numpy as np
#    from pycompss.api.api import compss_wait_on

    np.show_config()
    
    args = sys.argv[1:]
    
    MSIZE = int(args[0])
    BSIZE = int(args[1])
    
    A = []
    B = []
    C = []
    
    initialize_variables()
   
    for i in range(MSIZE):
        for j in range(MSIZE):
            for k in range(MSIZE):
                multiply(A[i][k], B[k][j], C[i][j])

#    for i in range(MSIZE):
#        for j in range(MSIZE):
#           print "C" + str(i) + str(j) + "=" + str(compss_wait_on(C[i][j]))
user@bsccloud:~/matmul_numpy$ 
user@bsccloud:~/matmul_numpy$ 
user@bsccloud:~/matmul_numpy$ cat src/matmul_numpy.py
'''
@author: etejedor
'''

### HELPER FUNCTIONS ###

def initialize_variables():
    import numpy as np
    for matrix in [A,B,C]:
        for i in range(MSIZE):
            matrix.append([])
            for j in range(MSIZE):
                if matrix == C:
                    block = np.array(np.zeros((BSIZE, BSIZE)), dtype=np.double, copy=False)
                else:
                    #aux = np.zeros((BSIZE, BSIZE))
                    #aux.fill(2.0)
                    #block = np.array(aux, dtype=np.double, copy=False)
                    block = np.array(np.random.random((BSIZE, BSIZE)), dtype=np.double, copy=False)
                mb = np.matrix(block, dtype=np.double, copy=False)
                matrix[i].append(mb)




### TASK SELECTION ###

from pycompss.api.task import task
from pycompss.api.parameter import *

@task(c = INOUT)
def multiply(a, b, c):
    #import time
    #start = time.time()

    import numpy as np
    #np.show_config()

    c += a*b

    #end = time.time()
    #tm = end - start
    #print "TIME: " + str(tm*1000) + " msec"



### MAIN PROGRAM ###

if __name__ == "__main__":
    
    import sys
    import numpy as np
#    from pycompss.api.api import compss_wait_on

    np.show_config()
    
    args = sys.argv[1:]
    
    MSIZE = int(args[0])
    BSIZE = int(args[1])
    
    A = []
    B = []
    C = []
    
    initialize_variables()
   
    for i in range(MSIZE):
        for j in range(MSIZE):
            for k in range(MSIZE):
                multiply(A[i][k], B[k][j], C[i][j])

#    for i in range(MSIZE):
#        for j in range(MSIZE):
#           print "C" + str(i) + str(j) + "=" + str(compss_wait_on(C[i][j]))

