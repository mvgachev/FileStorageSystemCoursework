# FileStorageSystemCoursework
In this coursework you will build a distributed storage system. This will involve knowledge
of Java, networking and distributed systems. The system has one Controller and N Data
Stores (Dstores). It supports multiple concurrent clients sending store, load, list, remove
requests. Each file is replicated R times over different Dstores. Files are stored by
the Dstores, the Controller orchestrates client requests and maintains an index with the
allocation of files to Dstores, as well as the size of each stored file. The client actually
gets the files directly from Dstores – which makes it very scalable. For simplicity all these
processes will be on the same machine but the principles are similar to a system
distributed over several servers. Files in the distributed storage are not organised in
folders and sub-folders. Filenames do not contain spaces.
The Controller is started first, with R as an argument. It waits for Dstores to join the
datastore (see Rebalance operation). The Controller does not serve any client request
until at least R Dstores have joined the system.
As Dstores may fail and be restarted, and new Dstores can join the datastore at runtime,
rebalance operations are required to make sure each file is replicated R times and files
are distributed evenly over the Dstores.

More information is available in the instructions.pdf file.
