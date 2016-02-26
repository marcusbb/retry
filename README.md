# Retry Framework (Store and Callback)
===============

Store and Callback framework based on top the grid framework hazelcast.
www.hazelcast.com

I originally built this framework at @Blackberry.  It's useful for garanteed (eventual) notification deliver, but could be purposed for other such similar use cases that require a dependable asynchronous batch oriented message processing.

Store "retry" objects that will be later called based on a callback policy and configuration.  
Retries are distributed according to hazelcast policy (configuration) and optionally stored in relational DB or
[Cassandra](http://cassandra.apache.org)


Retry is intended to be used as a library embedded within the application that both adds and takes from the Retry Queue.  
Is it a Queue?
Retry is currently backed by Hazelcast distributed Map, so lacked the ordering principles of a total ordered queue.  Unlike a queue, the set of messages can be queried as well as removed by the client application.


### What's in the box?

- Store RetryHolder serializable data structures, with callback semantics
- Scalable within the bounds of the Hazelcast grid
- Partitioned scalable in memory processing (locality) of retry objects
- Garanteed ordering per type/id of message
- Configurable storage to DB (Oracle, Derby, Postgres) with write behind asynchronous queueing
- Incremental batch loading from Storage and eventual memory storage 
- Configurable back off strategies per type
- Configurable per thread pool message consumption (callback)
- Configurable per node throttling (per type) on success/failure algorithm


### Retry as a service

```java
		//dynamic configuration
		Retry.getRetryManager().getConfigManager().addConfiguration(new RetryConfiguration()); //set at will
		
		Retry.getRetryManager().registerCallback(new RetryCallback() {
			
			@Override
			public boolean onEvent(RetryHolder retry) throws Exception {
				//process which can throw exception
				
				return false;
			}
		}, "my_endpoint_type");
		Retry.getRetryManager().addRetry(new RetryHolder("my_uuid", "my_endpoint_type",new Serializable() {
			public String content;
			public long id;
		}));

```

### Usage


### Requirements
Please check pom for an egregious errors.  I have done my best to keep internal repositories and dependencies out of project.
- jdk 1.7+
- cql-util - It's not published to public maven repository but can be pulled and built [here](http://github.com/marcusbb/cql-util)


