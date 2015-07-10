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

package mklew.cts

import akka.actor.{ActorRef, Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.datastax.driver.core.Statement
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
class PublicApiDesignTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers
{

  trait Builder
  {
    def withPartitioningMetaData(partitioningMetaData: PartitioningMetaData): Builder

    def build(): CtsCluster
  }

  trait CtsCluster
  {

    def ?(any: Any): Future[Any]
  }

  type CtsSession = ActorRef

  case class TableName(name: String)

  case class ParitionKeyName(key: String)

  case class PartitioningMetaData(metaData: Map[TableName, List[ParitionKeyName]])

  case class Event(eventId: Option[String], st: Statement, dependencies: Set[Event])
  {
    def dependsOn(e: Event): Event = this.copy(dependencies = dependencies + e)
  }

  object Event
  {
    def create(st: Statement): Event = Event(None, st, Set())

    def create(eventId: String, st: Statement): Event = Event(Some(eventId), st, Set())
  }

  case class EventGroup(events: Set[Event])
  {
    def add(event: Event) = this.copy(events = events + event)
  }

  object EventGroup
  {
    def create(): EventGroup = EventGroup(Set())
  }


  object Connect

  object CtsCluster
  {
    object Builder
    {
      def create(): Builder = ???
    }
  }

  case class EventTransaction(eventGroup: EventGroup, actorToMessageOnCommit: String, actorToMessageOnFailure: String)

  /**
   * Defines possible event rollback strategies
   */
  trait EventRollbackStrategy

  /**
   * Can be used for insert statement, it will remove created row.
   */
  case object DeleteEventStrategy

  /**
   * Before execution of the event it will READ all columns,
   * SAVE them on the side as Map[Key, Value]
   * UPDATE in case of rollback
   */
  case object RestoreDataEventStrategy

  /**
   * actor will receive message RollbackEvent(eventId: Option[String])
   */
  case object CustomRollbackStrategy

  /**
   * Enum for rolling back event group or just failed events.
   */
  trait EventGroupRollback

  /**
   * If any event fails then whole event group will be rolled back
   */
  object AllForOneEventGroupRollback

  /**
   * Only failed events will be rolled back
   */
  object OneForOneEventGroupRollback


  // THIS HAPPENS SOMEWHERE IN BEGINING OF APPLICATION
  val metaDataExample = PartitioningMetaData(
    Map(
      TableName("USERS") -> List(ParitionKeyName("CITY"), ParitionKeyName("FIRST_LETTER")),
      TableName("SONGS") -> List(ParitionKeyName("ALBUM"))
    ))

  val ctsCluster: CtsCluster = CtsCluster.Builder.create().withPartitioningMetaData(metaDataExample).build()
  val sessionF: Future[Any] = ctsCluster ? Connect

  // this happens inside actor

  sessionF.map
  { case session: CtsSession =>

    val insertStmt: Statement = ???
    val updateStmt: Statement = ???
    val anotherUpdateStmt: Statement = ???
    val independentUpdate: Statement = ???

    val insertEvent = Event.create("insert new user", insertStmt)
    val updateEvent = Event.create("update user counter", updateStmt)
    val anotherUpdateEvent = Event.create("update statistics", anotherUpdateStmt)
    val independentUpdateEvent = Event.create(independentUpdateEvent)

    insertEvent.dependsOn(updateEvent)
    updateEvent.dependsOn(anotherUpdateEvent)

    val eventGroup = EventGroup.create().
      add(insertEvent).
      add(updateEvent).
      add(anotherUpdateEvent).
      add(independentUpdateEvent)

    val selfAddress: String = ??? // self actor ref to string or something

    session ! EventTransaction(eventGroup, selfAddress, selfAddress)
  }
}
