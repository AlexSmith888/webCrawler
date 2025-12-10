***Multithreaded WEB CRAWLER***
Crawls data for a particular reference.  
The server Listens for incoming requests and sends short acceptance messages to the sender.   
Consists of a few pools of workers that validate / parse / save/ process successive links concurrently.    
Prevents links duplication   
Te multithreaded environment provides the average throughput of 7 million links per day  

1. MongoDB should be up and running  
2. Add the next file: /src/main/resources/config.properties  
3. Add the next parameters below to the file config.properties:  
  ***DB_HOST=***  
  ***DB_PORT=***  
  ***DB_NAME=***  
  ***COLLECTION=***  
