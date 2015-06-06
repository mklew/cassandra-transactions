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

package mklew.cts.algorithms

import scala.concurrent.Future

/**
 * Just a documentation of my thinking process of how processing should be started / resumed.
 *
 * @author Marek Lewandowski <marek.lewandowski@semantive.com>
 * @since 06/06/15
 */
trait StartEventProcessing
{

  trait Event extends Ordered[Event]
  {
    def isDone: Boolean
  }

  type LargeTimeSlice

  trait TimeSlice extends Ordered[TimeSlice]

  def isTimeSliceHappeningNow(ts: TimeSlice): Boolean

  def switchToNormalOperationMode(): Unit

  /**
   * TimeSlice + Bucket Id
   */
  case class TimeSliceId(ts: TimeSlice, bucket: Bucket)

  type Bucket

  /**
   * @return all buckets for that TimeSlice
   */
  def bucketing(ts: TimeSlice): Seq[TimeSliceId] = ???

  def readEventsToBeExecutedForTimeSlice(ts: TimeSlice) =
  {
    val currentTimeSliceBuckets: Seq[TimeSliceId] = bucketing(ts)
    // optimization can be done to operate just on sample of buckets, but let's just assume that
    // time slices and buckets have size good enough for processing all of it at once.
    val sequences: Seq[Future[Seq[Event]]] = currentTimeSliceBuckets.map(timeSliceId =>
                                                                           queryFor[TimeSliceId, Future[Seq[Event]]](timeSliceId)).map(_.map(_.filterNot(_.isDone)))
    val sequence: Future[Seq[Seq[Event]]] = Future.sequence(sequences)

    sequence.map(_.flatten).map(_.sorted)
  }

  case class MetaRow(currentTimeSlice: TimeSlice, currentLargeTimeSlice: LargeTimeSlice)

  /**
   * Complexity:
   *
   * read meta row x 1
   * read event rows x BucketSize
   */
  def executeBootstraping(): Unit =
  {
    val metaData: MetaRow = readMetaRow()

    // TODO need to check if currentTimeSlice is in the past. If it is then what is below is correct,
    // if currentTimeSlice happens to be in present, therefore new events can be added to this timeslice during
    // bootstrapping then this is not correct behaviour.

    val currentTimeSlice: TimeSlice = metaData.currentTimeSlice
    startBootstrapingFor(currentTimeSlice, metaData.currentLargeTimeSlice)
  }

  def startBootstrapingFor(currentTimeSlice: TimeSlice, largeTimeSlice: LargeTimeSlice)
  {
    if (isTimeSliceHappeningNow(currentTimeSlice))
    {
      switchToNormalOperationMode()
    }
    else
    {
      val eventsF: Future[Seq[Event]] = readEventsToBeExecutedForTimeSlice(currentTimeSlice)

      eventsF.map(eventsToBeExecuted =>
                  {
                    if (eventsToBeExecuted.nonEmpty)
                    {
                      sendEventsForExecution(eventsToBeExecuted)
                      val remainingTimeSlicesInCurrentLargeSlice: Seq[TimeSlice] = readNextEventsBy(largeTimeSlice).
                        filter(ts => ts > currentTimeSlice)

                      remainingTimeSlicesInCurrentLargeSlice.map(remainingTimeSlice =>
                                                                 {
                                                                   persistCurrentTimeSlice(remainingTimeSlice)

                                                                   if (isTimeSliceHappeningNow(remainingTimeSlice))
                                                                   {
                                                                     switchToNormalOperationMode()
                                                                   }
                                                                   else
                                                                   {
                                                                     readEventsToBeExecutedForTimeSlice(remainingTimeSlice).map
                                                                     { eventsInNextSlice =>
                                                                       sendEventsForExecution(eventsInNextSlice)
                                                                     }
                                                                   }
                                                                 })
                    }
                    else
                    {
                      executeEventsInNextLargeTimeSlice(largeTimeSlice, currentTimeSlice)
                    }
                  })
    }
  }

  def executeEventsInNextLargeTimeSlice(currentLargeTimeSlice: LargeTimeSlice, currentTimeSlice: TimeSlice)
  {
    val nextTimeSlices: Seq[TimeSlice] = readNextEventsBy(currentLargeTimeSlice).filter(ts => ts > currentTimeSlice)

    if (nextTimeSlices.nonEmpty)
    {
      val nextTimeSlice: TimeSlice = nextTimeSlices.head

      inParallel({
                   // can be done in parallel because if this operation fails then worst
                   // case scenario is that next boostraping will be slower.
                   persistCurrentTimeSlice(nextTimeSlice)
                 },
                 {
                   // TODO need to check if this nextTimeSlice is happening now and act accordingly
                   readEventsToBeExecutedForTimeSlice(nextTimeSlice).map
                   { eventsInNextSlice =>
                     sendEventsForExecution(eventsInNextSlice)
                     executeEventsInNextLargeTimeSlice(currentLargeTimeSlice, nextTimeSlice)
                   }
                 })
    }
    else
    {
      val lts: LargeTimeSlice = advanceLargeTimeSlice(currentLargeTimeSlice)
      inParallel({
                   persistCurrentLargeTimeSlice(lts)
                 },
                 {
                   executeEventsInNextLargeTimeSlice(lts, currentTimeSlice)
                 })
    }
  }

  def advanceLargeTimeSlice(lts: LargeTimeSlice): LargeTimeSlice

  def persistCurrentLargeTimeSlice(lts: LargeTimeSlice): Unit

  def inParallel[A, B](block1: => A, block2: => B)

  /**
   * Saves current time slice to metadata.
   * @param ts current time slice
   */
  def persistCurrentTimeSlice(ts: TimeSlice)


  def readNextEventsBy(largeSlice: LargeTimeSlice): Seq[TimeSlice] =
  {
    queryFor(largeSlice)
  }

  def readMetaRow(): MetaRow

  /**
   * Represents cassandra query operation
   */
  def queryFor[A, T](a: A): T

  /**
   * Sends events for execution.
   */
  def sendEventsForExecution(events: Seq[Event]): Unit
}
