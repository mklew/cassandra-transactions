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

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 01/08/15
 */
case class Event(id: String)

case class EventNode(e: Event, dependents: Seq[Event])

case class OrderedEventNode(order: Int, eventNode: EventNode)

/**
 * Orders events for execution.
 *
 * However this is not optimal execution model. Events can be executed in parellel as soon as their dependencies
 * are done.
 */
trait OrderEventsForExecution
{
  /**
   *
   * @param eventNodes
   * @return None if events have cyclic dependnecy
   *         Some if events form DAG.
   *
   *         List has list of events. 
   *         Lists of events can be executed in parallel.
   *         Events inside of each list have to be executed according to order in list.
   *
   */
  def orderEvents(eventNodes: Seq[EventNode]): Option[List[List[Event]]]
}

class JGraphBasedImpl extends OrderEventsForExecution
{

  import org.jgrapht.alg._
  import org.jgrapht.experimental.dag.DirectedAcyclicGraph
  import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException
  import org.jgrapht.graph._

import scala.collection.JavaConversions._


  override def orderEvents(eventNodes: Seq[EventNode]): Option[List[List[Event]]] =
  {
    val graph = new DirectedAcyclicGraph[Event, DefaultEdge](classOf[DefaultEdge])

    val fromEventToEventNode = eventNodes.map(x => x.e -> x).toMap

    eventNodes.map(e => graph.addVertex(e.e))
    try
    {
      for
      {
        event <- eventNodes
        dependent <- event.dependents
      }
      {
        graph.addDagEdge(event.e, dependent)
      }
      val inspector = new ConnectivityInspector(graph)
      val connectedSets = inspector.connectedSets()
      val toBeExecutedInParallel: List[List[Event]] = connectedSets.map
        { connectedSet =>
          connectedSet.size() match
          {
            case 1 => List(connectedSet.head)
            case 2 =>
              val it = connectedSet.iterator()
              val first = it.next()
              val second = it.next()

              if (fromEventToEventNode(first).dependents.contains(second))
              {
                List(first, second)
              }
              else List(second, first)
            case _ =>
              val g = new DirectedAcyclicGraph[Event, DefaultEdge](classOf[DefaultEdge])
              connectedSet.foreach(e => g.addVertex(e))
              connectedSet.foreach(e => fromEventToEventNode(e).dependents.map(d => g.addDagEdge(e, d)))
              g.iterator().toList
          }
        }.toList
      Option(toBeExecutedInParallel)
    }
    catch
      {
        case e: CycleFoundException => None
      }
  }
}