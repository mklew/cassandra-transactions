# Cassandra Transactions (CTS) - specification

## Events

_Events_ are statements that are executed at some time in the future. (when EventExecutor executes them).

Event:

* belongs to single event group
* can be executed
* can be rolled back
* events can have dependencies on each other

### Rollback mechanics

Possible rollback scenarios:

* rollback by client via asynchronous callback.
* rollback by restoring data to previous state (requries READ for data + WRITE on rollback)


### Event dependencies

Event within event group can have multiple dependencies on other events which causes them to be executed sequentially. 

Examples:

  1. No dependencies

     If event **A** has no dependencies then it is executed in parallel with other events within event group.

  1. Single dependnecy

  If event **A** depends on event **B** then events are executed sequentially. 
  
  	1. Execute event **B**
  	2. If successfulyl executed event **B** then execute event **A**. 
  	   Else event **A** will not be executed.

 1. Multiple dependencies

	If  event **A** depends on events **E1**, **E2**, ..., **EN** and events **E1**, **E2**, ..., **EN** do not depend on each other nor on any other events then
	
	  1. Execute in parallel events **E1**, **E2**, ..., **EN**
      2. If successfully executed events **E1**, **E2**, ..., **EN** then execute event **A**
         Else event **A** will not be executed.
         

Summary:

* Events within event group forms directed acylic graph.   
* In general all cases derive from multiple dependencies. Single and No dependencies are just cases when dependency list has 1 or none elements.
* Circular dependencies are illegal and defining them **must** fail fast.


## Event Groups

**Event Group** is a set of events that can be executed as whole successfully or rolled back.
If execution of event fails then whole event group is rolled back.

**TODO do I know exactly what does it mean for event to fail?**

## Event Processor

Event processor consumes Event Group Stream and executes Event Groups sequentially. This guarantees that no locks are needed and there are no race conditions.

__Optimization__
  Event Groups can be executed in parallel if they do not act on same _CQL DataModel_ rows.

**Important**  
   It is not known in all cases which events act on which rows, but if it is known and there is    no collision then these event groups can be executed in parallel.
  
## Event data transfer

In general events should be simple statements that can be executed, but there are valid cases when in order to execute event some data is required.

Abstract example: You have to do query (__Event Query__) for some data and some of that data is required for update statement (__Event Update__ with dependency on __Event Query__)

In general flow looks like: 

		Query Data -> apply function -> Update data
		
__Query Data__ can return one or many rows with one or many columns.

__Apply function__ can be arbitrary logic like: 
  
  - extract column value
  - execute arbitrary piece of code that produces result.
 
__Update data__ can execute when all of its dependencies produce results.


If **Event A** requires data from **Event B** then is **must** have dependency on **Event B**.


Implementation details:
__Event A__ defines that is depends on __Event B__ and has __Event B.name__ on its dependency list.
__Event A__ defines that is required data from __Event B__ and specifies **name for result**.
__Event A__ specifies data requirements.

Data requirement can be:

   - Arbitrary callback message that sends back response in expected form within certain configurable timeout.

   Here there should be some protocol in place. 
   What needs to be defined:
   
      - __actorRef__ - actor to send message to which should answer with data.
      
      - __timeout__ - time to wait for data.

      - __name__ - name under which data from response is saved.
   
   - Data from some event in event group which imposes dependnecy relationship on that event.  
     What needs to be defined:
     
      - __event name__ - event that is dependnecy
      - __actorRef__ - actor to send event's result which applies function to it and answers with data.
      - __name__ - name under which data is saved
      - __timeout__ - time to wait for apply function and data.


In any case recevied data will be saved within event under specified __name__ so that if playback of event is required, no communication with actorRef (which probably won't exist) is necessary.


## Event Pre & Post execution hooks


- Pre hook: can specify actorRef (name or path) which gets message **before** _Event_ is executed.
- Post hook: can specify actorRef (name or path) which gets message **after** _Event_ is executed.

Guarantees:

- a message is always sent to pre hook actor. Fire & forget fasion.
- post hook always receive message whether event succeeded or failed
- Event execution will not wait for _ACK_ (if acking set) from _PreHook_ which means that under bad network conditions _PostHook_ might receive message first and then _PreHook_.

Extras:
- optional ACKing for delivery of pre & post hook messages.

### Valid uses of pre & post hooks:

Progress report - report on progress to some high level components  
Trigger actions - could submit another event group doing something else. Could be thought as nested transaction (althought it will only be executed concurrently if it does not modify same rows and there are no other Event Groups waiting for execution).


## Event Group Pre & Post execution hooks 


### Valid uses of pre & post hooks:

Pre hook could start up some actors that could potentially be required for further processing on event group, but that requires built in hook type and props for actor.

__TODO__ check if Props can be persisted. It is case class so it is serializable so in theory it can be.

__TODO__ if _CTS_ has to create actor out of this Props then actor class as well as any other arguments must be available on _CTS_ classpath, which means that _CTS_ has to load some JAR on startup which has all the classes.


#### Implementation detail:
  I didn't expect event groups to be present as such somewhere but only as logical grouping of events within same group id. In order to implement Pre&Post hooks EmptyEvent can be introduced.
  EmptyEvent won't have any statement to execute nor values, but can have dependencies and other hooks defined. 
  
  __Pre Event Group hook:__
      Modify all events to depend on _EmptyEvent_. When _DAG_ is created, _EmptyEvent_ will be first to execute, its pre execute hook will be Pre Event Group hook.
      
  __Post Event Group hook:__
      _EmptyEvent_ which depends on all other events within EventGroup. When DAG is created, _EmptyEvent_ will be last to execute, its post execute hook will be Post Event Group hook.
      
      
## Library loading

Predefined hooks which startup actors (moving computation to data kind of thing) will require classes and its dependencies to be present.

Options: Do it as triggers in Cassandra -> put JAR into Cassandra installation

Feature: Resolving dependnecy automatically just like in build tool (Maven, Ivy)

__TODO__ for prototype let's just investigate how it is done with triggers.

**Crazy idea:** If there could be event's that could load jars, that would resolve dependnecy and load it (and unload older version) then data would evolve with code without worrying about migrations, because until some time version v1.0.0 is used and then after some event in Event Stream version v2.0.0 is used.
      
       
 

         