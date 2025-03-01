- # General
    - #### Team: cs122b-winter25-aj

    - #### Names: Alfredo Leon and Juan Varela

    - #### Project 4 Video Demo Link:

    - #### Instruction of deployment:
      - Deployed project on AWS with JDBC Connection Pooling
      - Deployed master, slave, and load balancer using apache onto AWS following the instructions given to us
      - Enables sticky sessions to allow proper handling of sessions
      - Setup GCP load balancer with the same instructions

    - #### Collaborations and Work Distribution:
      - Juan Varela - Task 1
      - Alfredo Leon - Task 2-4


- # Connection Pooling
    - #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling.
      - **File Path:** `META-INF/context.xml`, `src/*.java`, `src/utils/*.java`

    - #### Explain how Connection Pooling is utilized in the Fabflix code.
      - Our init methods of our Servlets retrieve DataSource from JNDI
      - Instead of creating new connections, we use dataSource.getConnection() to get a connection from the pool

    - #### Explain how Connection Pooling works with two backend SQL.
      - The master is the only one who handles the write queries, and read queries are sent equally between Master and Slave


- # Master/Slave
    - #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL.
      - **File Path:** `META-INF/context.xml`, `WEB-INF/web.xml`, `src/*.java`, `src/utils/*.java`
      - **Master/Slave Query Routing:** Implemented in all servlets under `src/` by obtaining connections from `dataSource.getConnection()`.

    - #### How read/write requests were routed to Master/Slave SQL?
      - Write requests would go to the master, while read requests were equally distributed between Master and Slave for load balancing 