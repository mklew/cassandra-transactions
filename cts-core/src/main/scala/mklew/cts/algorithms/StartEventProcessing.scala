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

  def isLtsHappeningNow(lts: LargeTimeSlice): Boolean

  def goToOperationalMode(): Unit

  /**
   * TimeSlice + Bucket Id
   */
  case class TimeSliceId(ts: TimeSlice, bucket: Bucket)

  type Bucket

  case class MetaRow(currentTimeSlice: TimeSlice, currentLargeTimeSlice: LargeTimeSlice)

  /**
   * @return all buckets for that TimeSlice
   */
  def bucketing(ts: TimeSlice): Seq[TimeSliceId] = ???


  def executeEventsInTimeSlice(ts: TimeSlice) =
  {
    val currentTimeSliceBuckets: Seq[TimeSliceId] = bucketing(ts)
    // optimization can be done to operate just on sample of buckets, but let's just assume that
    // time slices and buckets have size good enough for processing all of it at once.
    val events = readNotDoneEvents(currentTimeSliceBuckets)
    sendEventsForExecution(events)
  }

  def readNotDoneEvents(currentTimeSliceBuckets: Seq[TimeSliceId]): Seq[Event] =
  {
    currentTimeSliceBuckets.map(timeSliceId =>
                                {
                                  queryFor[TimeSliceId, Seq[Event]](timeSliceId)
                                }).flatten.filterNot(_.isDone).sorted
  }

  def findNextTimeSliceInLts(finishedTs: TimeSlice, lts: LargeTimeSlice): Option[TimeSlice] =
  {
    readNextEventsBy(lts).find(ts => ts > finishedTs)
  }

  def persistTimeSlice(ts: TimeSlice): Unit = ???

  def persistLargeTimeSlice(lts: LargeTimeSlice): Unit = ???

  def keepBootstrappingFor(ts: TimeSlice, lts: LargeTimeSlice): Unit =
  {
    findNextTimeSliceInLts(ts, lts) match
    {
      case Some(nextTs) =>
        persistTimeSlice(nextTs)
        if (isTimeSliceHappeningNow(nextTs)) goToOperationalMode()
        else
        {
          executeEventsInTimeSlice(nextTs)
          keepBootstrappingFor(nextTs, lts)
        }
      case None =>
        if (isLtsHappeningNow(lts)) goToOperationalMode()
        else
        {
          val nextLts: LargeTimeSlice = advanceLargeTimeSlice(lts)
          persistLargeTimeSlice(lts)
          keepBootstrappingFor(ts, lts)
        }
    }
  }

  /**
   * Executes all events in past time slices. 
   * Moves to operational mode if:
   *    time slice exists that is current or
   *    current time slice is current and there is no time slice yet.
   *    
   * 
   * Complexity:
   *
   * read meta row x 1
   * read event rows x BucketSize
   */

  def bootstrapEventProcessing(): Unit =
  {
    val metadata = readMetaRow()

    if (isTimeSliceHappeningNow(metadata.currentTimeSlice)) goToOperationalMode()
    else
    {
      executeEventsInTimeSlice(metadata.currentTimeSlice)
      keepBootstrappingFor(metadata.currentTimeSlice, metadata.currentLargeTimeSlice)
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
