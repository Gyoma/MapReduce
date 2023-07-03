# SLR207

> Project of SLR207 at Télécom Paris - **Artem Golovin**

Implementation of MapReduce concept - Télécom Paris 2022-2023

## Implementation

It's written in Java using Maven manager to configure, compile and create JAR packages.

This is done in the form of an automaton, meaning that each server knows its current state and the state it can go to if it fulfills the necessary conditions.

### Communication

All the communication (between the servers and between the servers and master) is implemented via raw sockets.

### Launch

First you need to start the servers, and then the master program. This is done automatically through the deployment script called ``deploy.sh``

Data to be specified in the script:
1. List of hosts and ports on which to run the servers
2. Host on which to run the master
3. File from which to take data

After altering the script you need just to launch it.

The master connects on a given port to each server via a socket, so the master is a client in this sense.

### Splitting

The master node splits the file almost equally into bytes trying to finish words if they are not completed after the split and sends them to the servers in a round-robin manner.

### Mapping

After receiving the split, each server tokenizes the split to get the words from it and locally counts how many times it has encountered this or that word.

### Shuffling & Reducing

This is the first stage in which the servers interact with each other. Its goal is to collect all the same words (and their frequencies respectively) on one single server. Which word is sent to which server is determined by the following formula:

> index of the server = hash(word) % count of servers

Reduce step is applied automatically due to the HashMap specification.

Note:
I think it's important to describe here the communication protocol of this step.

After a server has sent data to other server it must send an ``END`` command to let the receiver know it is done. The receiver must in turn respond with an ``OK`` command, letting know that it has received all the data. This is done to make the servers consistent before moving on to the next step.

### Sort. Mapping

This is the part where we want to make each server responsible for a specific range of word frequencies.

The servers send their local word frequencies to the master, then the master divides the entire frequency range into subranges assigned to each server and sends the data back to the servers.

### Sort. Shuffling & Reducing

All servers send the data that was generated during ``Shuffling & Reducing`` step to the appropriate servers. The same communication protocol is used here.

### Progress output

To make the program more verbose, the information necessary to understand the process is sent to the logger. This information is displayed in the console and saved to the master.log and server.log files for the master and each server, respectively.

<details>
  <summary>Log example</summary>

  ```
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.470 : -path=/cal/commoncrawl/CC-MAIN-20230320175948-20230320205948-00274.warc.wet -s=tp-3a209-04:10335;tp-3a209-05:10335
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.620 : Starting client program...
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.621 : File path: /cal/commoncrawl/CC-MAIN-20230320175948-20230320205948-00274.warc.wet
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.641 : Processing the following addresses:
tp-3a209-04:10335
tp-3a209-05:10335
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.641 : Starting a handler for tp-3a209-04 on port 10335
1::com.src.server.Server::start::03-07-2023 16:13:29.688 :  : Waiting for the master
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.692 : Starting a handler for tp-3a209-05 on port 10335
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.694 : Starting the master
1::com.src.client.ClientProgram::main::03-07-2023 16:13:29.694 : Starting StopWatcher...
1::com.src.server.Server::start::03-07-2023 16:13:29.697 :  : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:29.727 :  : From master: INITIALIZE
1::com.src.server.Server::start::03-07-2023 16:13:29.728 :  : From master: -a=tp-3a209-04:10335 -s=tp-3a209-05:10335;tp-3a209-04:10335
1::com.src.server.Server::start::03-07-2023 16:13:29.729 :  : From master: INITIALIZE
1::com.src.server.Server::start::03-07-2023 16:13:29.730 :  : From master: -a=tp-3a209-05:10335 -s=tp-3a209-05:10335;tp-3a209-04:10335
1::com.src.server.Server::start::03-07-2023 16:13:29.735 : tp-3a209-04:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:29.736 : tp-3a209-05:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:29.735 : tp-3a209-04:10335 : From master: MAPPING
1::com.src.server.Server::start::03-07-2023 16:13:29.737 : tp-3a209-05:10335 : From master: MAPPING
1::com.src.server.Server::start::03-07-2023 16:13:33.702 : tp-3a209-05:10335 : Bytes to get : 175331217
1::com.src.server.Server::start::03-07-2023 16:13:39.584 : tp-3a209-04:10335 : Bytes to get : 175331216
1::com.src.server.Server::start::03-07-2023 16:13:44.322 : tp-3a209-05:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:44.324 : tp-3a209-05:10335 : From master: SHUFFLING
1::com.src.server.Server::start::03-07-2023 16:13:44.592 : tp-3a209-04:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:44.592 : tp-3a209-04:10335 : From master: SHUFFLING
13::com.src.server.Server::lambda$createHandler$8::03-07-2023 16:13:47.935 : tp-3a209-05:10335 : Reply OK to tp-3a209-04:10335
1::com.src.server.Server::start::03-07-2023 16:13:48.454 : tp-3a209-04:10335 : Waiting for the master
13::com.src.server.Server::lambda$createHandler$8::03-07-2023 16:13:48.454 : tp-3a209-04:10335 : Reply OK to tp-3a209-05:10335
1::com.src.server.Server::start::03-07-2023 16:13:48.454 : tp-3a209-04:10335 : From master: SORT_MAPPING
1::com.src.server.Server::start::03-07-2023 16:13:48.457 : tp-3a209-05:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:48.457 : tp-3a209-05:10335 : From master: SORT_MAPPING
1::com.src.server.Server::start::03-07-2023 16:13:48.656 : tp-3a209-04:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:48.656 : tp-3a209-04:10335 : From master: SORT_SHUFFLING
1::com.src.server.Server::start::03-07-2023 16:13:48.657 : tp-3a209-05:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:48.658 : tp-3a209-05:10335 : From master: SORT_SHUFFLING
1::com.src.client.MasterClient::start::03-07-2023 16:13:48.659 : master : The following ranging (server=frequency threshold) was calculated:
1::com.src.client.MasterClient::lambda$start$3::03-07-2023 16:13:48.660 : master: tp-3a209-05:10335=1770
1::com.src.client.MasterClient::lambda$start$3::03-07-2023 16:13:48.660 : master: tp-3a209-04:10335=443147
13::com.src.server.Server::lambda$createHandler$8::03-07-2023 16:13:49.688 : tp-3a209-05:10335 : Reply OK to tp-3a209-04:10335
1::com.src.server.Server::start::03-07-2023 16:13:49.694 : tp-3a209-04:10335 : Waiting for the master
13::com.src.server.Server::lambda$createHandler$8::03-07-2023 16:13:49.694 : tp-3a209-04:10335 : Reply OK to tp-3a209-05:10335
1::com.src.server.Server::start::03-07-2023 16:13:49.695 : tp-3a209-04:10335 : From master: QUIT
1::com.src.server.Server::start::03-07-2023 16:13:49.695 : tp-3a209-04:10335 : Stopping server program...
1::com.src.server.Server::start::03-07-2023 16:13:49.697 : tp-3a209-05:10335 : Waiting for the master
1::com.src.server.Server::start::03-07-2023 16:13:49.697 : tp-3a209-05:10335 : From master: QUIT
1::com.src.server.Server::start::03-07-2023 16:13:49.697 : tp-3a209-05:10335 : Stopping server program...
1::com.src.server.Server::start::03-07-2023 16:13:49.696 : tp-3a209-04:10335 : Server programm stopped.
1::com.src.server.Server::start::03-07-2023 16:13:49.698 : tp-3a209-05:10335 : Server programm stopped.
1::com.src.client.ClientProgram::main::03-07-2023 16:13:49.723 : Stopping client program...
1::com.src.client.ClientProgram::main::03-07-2023 16:13:49.724 : Client program stopped.
1::com.src.client.ClientProgram::main::03-07-2023 16:13:49.725 : Total time : 20029ms
  ```
</details>

### Results

This ran on 300mb files each on Télécom's machines.

The following results (averages of 5 runs) obtained:

- 2 servers : 26401ms
- 4 servers : 19146ms
- 6 servers : 20388ms
- 8 servers : 22013ms
- 12 servers : 23601ms

![plot](./plot.png)

### Conclusion

As we can see the increase in the number of servers led to a performance boost only in the transition from 2 servers to 4 servers. A further increase in the number of servers did not lead to an increase in performance, but even the opposite has worsened it. I believe this is due to the fact that the size of the tested file is not enough to increase the speed, because at some point the time delay for the number of communications that need to be made exceeds the speed of execution on servers of the same amount of data.