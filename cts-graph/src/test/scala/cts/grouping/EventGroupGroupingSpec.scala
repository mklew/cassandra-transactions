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

import org.scalatest.{FreeSpec, Matchers}

import scala.language.implicitConversions

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 01/08/15
 */
class EventGroupGroupingSpec extends FreeSpec with Matchers with EventGroupGrouping
{
  implicit def strToDummyEvent(s: String): DummyEvent = DummyEvent(s)

  case class DummyEvent(partitionKey: String) extends Event

  case class DummyEventGroup(events: Seq[DummyEvent]) extends EventGroup

  case class SimpleEventGroupGrouping(maximumEventsLimit: Int) extends Grouping

  "EventGrouping should" - {

    val egsWithUniquePartitions = List(DummyEventGroup(Seq("A1", "A2", "A3", "A4")), DummyEventGroup(Seq("B1", "B2")),
                                       DummyEventGroup(Seq("C1")), DummyEventGroup(Seq("D1", "D2")))

    "process EventGroups one by one if maximum events limit is 1" in {
      val grouper = SimpleEventGroupGrouping(1)
      val chunks = grouper.divideIntoChunks(egsWithUniquePartitions.toStream).toList
      chunks should have size egsWithUniquePartitions.size
      chunks.head.egs.head.events.head shouldEqual DummyEvent("A1")
    }

    "process all EventGroups" in {
      val grouper = SimpleEventGroupGrouping(25)

      val chunks = grouper.divideIntoChunks(egsWithUniquePartitions.toStream).toList

      chunks should have size 1
      chunks.head.egs should have size egsWithUniquePartitions.size
    }

    "process until limit" in {
      val grouper = SimpleEventGroupGrouping(6)

      val chunks = grouper.divideIntoChunks(egsWithUniquePartitions.toStream).toList

      chunks should have size 2
      chunks.head.egs should have size 2
      chunks.tail.head.egs should have size 2
    }

    "process until limit case 2" in {
      val grouper = SimpleEventGroupGrouping(7)

      val chunks = grouper.divideIntoChunks(egsWithUniquePartitions.toStream).toList

      chunks should have size 2
      chunks.head.egs should have size 3
      chunks.tail.head.egs should have size 1
      chunks.tail.head.egs.head.events.head shouldEqual DummyEvent("D1")
    }
  }
}

