/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cts.grouping

/**
 * Simplest execution model: Execute EventGroup one by one sequentially. -> This is poor performance.
 * Better execution model: Execute EventGroups as soon as they don't act on same partition keys. -> Should be better
 *
 * There should be a function which takes a Stream of EventGroup and consumes EventGroups until either:
 * - limit of Events in EventGroups,
 * let's say we handle 10 000 events so it might be either 2000 event groups with 5 events each or
 * 2 EGs with 5000 events each
 * - partition keys are already processed by other event
 *
 * EventGroups can be executed in parallel
 *
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 01/08/15
 */
trait EventGroupGrouping
{


  trait Event
  {
    def partitionKey: String
  }

  trait EventGroup
  {
    def events: Seq[Event]
  }

  case class EventProcessingChunk(egs: Seq[EventGroup])

  trait Grouping
  {

    def maximumEventsLimit: Int

    /**
     * @param eventGroupsStream All EventGroups can be executed concurrently.
     */

    // TODO abstract over Stream. Look at either some Monads or Akka Streaming

    def divideIntoChunks(eventGroupsStream: Stream[EventGroup]): Stream[EventProcessingChunk] =
    {
      def willBeBelowLimit(currentCount: Int, eg: EventGroup) = currentCount + eg.events.size <= maximumEventsLimit

      def doNotActOnSamePartitions(partitionKeys: scala.collection.Set[String], newPartitions: Set[String]) = partitionKeys.intersect(newPartitions).isEmpty

      if(eventGroupsStream.isEmpty) Stream()
      else {
        var currentCount = 0
        var partitionKeys: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()

        val chunk: Seq[EventGroup] = eventGroupsStream.takeWhile(eg =>
         {
           val partitionKeysForThatEventGroup = eg.events.map(_.partitionKey).toSet
           if (currentCount == 0 || (doNotActOnSamePartitions(partitionKeys, partitionKeysForThatEventGroup) && willBeBelowLimit(currentCount, eg)))
           {
             partitionKeys ++= partitionKeysForThatEventGroup
             currentCount += eg.events.size
             true
           }
           else false
         }).toSeq

        def remainingStream = eventGroupsStream.drop(chunk.size)

        EventProcessingChunk(chunk) #:: divideIntoChunks(remainingStream)
      }
    }
  }

}
