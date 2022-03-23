# CS4262 - Distributed Chat System (grpc)

## pre-requirements

* openjdk version "17.0.2"
* Apache Maven 3.8.4

(we did not check other versions)

## instructions 

* ``` git clone https://github.com/shashimalcse/ds2.git ```
* ``` cd ds2 ```
* ``` mvn clean package -Denforcer.skip=true -Djacoco.skip=true ```
* ``` java -cp target/demo-1.0-SNAPSHOT.jar com.heartbeat.App [server_id] [path to servers_conf.txt] ``` 
* ex: ``` java -cp target/demo-1.0-SNAPSHOT.jar com.heartbeat.App s1 servers_conf.txt ```
