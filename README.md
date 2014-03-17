ACDSense Evaluation Data Service
===============================

The ACDSense Evaluation Data Service is a RESTful service for managing evaluation data in the ACDSense scenario, an IoT scenario based on the Extensible Messaging and Presence Protocol (XMPP). This document provides instructions on how to build and deploy the service from source.  

Enabling Strong Encryption
-----------------------

The ACDSense Evaluation Data Service was developed on the LAS2peer Platform, a federated P2P service platform. LAS2peer requires end-to-end encryption for all communication between nodes in a P2P network. If you use an Oracle Java version, you have to enable strong encryption for LAS2peer (and the service) to operate correctly. With an Open JDK, this step is not needed.

- Download policy from Oracle: [JCE for Java 7](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html "JCE-7")
- Extract files to `./lib/security/`, relative to your Java Runtime Environment (replacing the existing files).

Building the Service
-------------------------------------

For building the service you need a running Java SDK (both Oracle and Open JDK should work) and [Apache Ant](http://ant.apache.org/) >1.8.
With the above prerequisites met, change directory to the service's root directory and build the service with Ant.

    cd ${SERVICE_ROOT}
    ant all

The build process will resolve all dependencies to LAS2peer libraries and other third-party libraries using [Apache Ivy](https://ant.apache.org/ivy/). Besides compilation, running JUnit tests and generating JavaDocs the build process produces a set of initial cryptographic agent keys to be used for service operation in a directory ```./startup``` (see also section _LAS2peer Authentication_ later).

Deploying the Service
----------------------------------------

__Database Preparation & Configuration__

The ACDSense Evaluation Data Service makes use of a [mySQL](http://www.mysql.com/) database for data management. Thus, before deploying the service, a respective database must be created and added to the service's configuration. 

1) Use the SQL script ```./doc/sql/database.schema.sql``` to create a database.

2) Configure database parameters in ```./config/i5.las2peer.services.acdsense.DataService.properties```.

__LAS2peer Start Script Configuration__

The service comes with a set of start scripts for starting a LAS2peer node (```./bin/start_network.bat``` for MS Win; ``` ./bin/start_network.sh``` for Unix), including the service and a Web Connector to make its RESTful API available to external callers outside the P2P network.

Per default, the following ports are used for communication:
- Port 9001 - LAS2peer P2P communication 
- Port 8080 - RESTful service access via Web Connector 

To change default configuration, change the start scripts accordingly.

Starting the Service
---
To start the service, simply run one of the LAS2peer start scripts mentioned above.

    ./bin/start_network.bat
    ./bin/start_network.sh

After successful launch of LAS2peer, you are directed to an interactive LAS2peer shell. Typing ```help``` provides information on interactive shell options. If you do not want to start LAS2peer in interactive mode, remove the start parameter ```interactive``` from the respective start script.

LAS2peer Authentication
---

Any connection to LAS2peer requires authentication with a numeric LAS2peer user identifier and a password.
During the build process, cryptographic keys for one example user agent "UserA" are generated with password "userAPass". You find the correct user id in the file ```./startup/agent-user-UserA.xml```.

For information on how to generate additional user agents, please refer to the LAS2peer Tutorial ["Add User Agents"](https://github.com/rwth-acis/LAS2peer-Tutorial-Project/wiki/Adding-User-Agents).
