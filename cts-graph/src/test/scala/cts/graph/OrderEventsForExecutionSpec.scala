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

package cts.graph

import org.scalatest.{OptionValues, Matchers, FreeSpec}

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 01/08/15
 */
class OrderEventsForExecutionSpec extends FreeSpec with Matchers with OptionValues
{
  "OrderEventsForExecution should " - {

    val orderEventsService: OrderEventsForExecution = new JGraphBasedImpl

    "order correctly events in simple case" in {
      val eventA = Event("A")
      val eventB = Event("B")
      val eventC = Event("C")
      val eventD = Event("D")

      val eventNodeA = EventNode(eventA, Seq(eventD))
      val eventNodeB = EventNode(eventB, Seq(eventA, eventC))
      val eventNodeC = EventNode(eventC, Seq(eventA))
      val eventNodeD = EventNode(eventD, Seq())

      val all = Seq(eventNodeA, eventNodeB, eventNodeC, eventNodeD)

      val orderedEvents: Option[List[List[Event]]] = orderEventsService.orderEvents(all)

      orderedEvents.value should have size(1)

      orderedEvents.value.head should have size(4)

      orderedEvents.value.head shouldEqual List(eventB, eventC, eventA, eventD)
    }

    "more complex case " in {
      val eventA = Event("A")
      val eventB = Event("B")
      val eventC = Event("C")
      val eventD = Event("D")

      val eventNodeA = EventNode(eventA, Seq(eventD))
      val eventNodeB = EventNode(eventB, Seq(eventA, eventC))
      val eventNodeC = EventNode(eventC, Seq())
      val eventNodeD = EventNode(eventD, Seq())

      val all = Seq(eventNodeA, eventNodeB, eventNodeC, eventNodeD)

      val orderedEvents: Option[List[List[Event]]] = orderEventsService.orderEvents(all)

      orderedEvents.value should have size(1)

      orderedEvents.value.head should have size(4)

      // TODO HERE A and C can be executed at same time.
      orderedEvents.value.head shouldEqual List(eventB, eventA, eventC, eventD)
    }

    "return none when cycle exists" in {
      val a = Event("A")
      val b = Event("B")

      val an = EventNode(a, Seq(b))
      val bn = EventNode(b, Seq(a))

      orderEventsService.orderEvents(Seq(an, bn)) shouldBe 'isEmpty
    }

    "return events to be executed in parallel case 1" in {
      val eventA = Event("A")
      val eventB = Event("B")
      val eventC = Event("C")
      val eventD = Event("D")

      val eventNodeA = EventNode(eventA, Seq())
      val eventNodeB = EventNode(eventB, Seq())
      val eventNodeC = EventNode(eventC, Seq())
      val eventNodeD = EventNode(eventD, Seq())

      val all = Seq(eventNodeA, eventNodeB, eventNodeC, eventNodeD)

      val orderedEvents: Option[List[List[Event]]] = orderEventsService.orderEvents(all)

      orderedEvents.value should have size(4)
    }

    "return events to be executed in parallel case 2" in {
      val eventA = Event("A")
      val eventB = Event("B")
      val eventC = Event("C")
      val eventD = Event("D")

      val eventNodeA = EventNode(eventA, Seq(eventB))
      val eventNodeB = EventNode(eventB, Seq())
      val eventNodeC = EventNode(eventC, Seq())
      val eventNodeD = EventNode(eventD, Seq())

      val all = Seq(eventNodeA, eventNodeB, eventNodeC, eventNodeD)

      val orderedEvents: Option[List[List[Event]]] = orderEventsService.orderEvents(all)

      orderedEvents.value should have size(3)
      orderedEvents.value.head shouldEqual List(eventA, eventB)
    }

    "return events to be executed in parallel case 3" in {
      val eventA = Event("A")
      val eventB = Event("B")
      val eventC = Event("C")
      val eventD = Event("D")

      val eventNodeA = EventNode(eventA, Seq())
      val eventNodeB = EventNode(eventB, Seq(eventA))
      val eventNodeC = EventNode(eventC, Seq())
      val eventNodeD = EventNode(eventD, Seq())

      val all = Seq(eventNodeA, eventNodeB, eventNodeC, eventNodeD)

      val orderedEvents: Option[List[List[Event]]] = orderEventsService.orderEvents(all)

      orderedEvents.value should have size(3)
      orderedEvents.value.head shouldEqual List(eventB, eventA)
    }

  }
}
