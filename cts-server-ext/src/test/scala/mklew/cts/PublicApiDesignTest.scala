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

import java.util.UUID

import akka.actor.{ActorRef, Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.datastax.driver.core.Statement
import com.datastax.driver.core.querybuilder.QueryBuilder
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

  case class Event(eventId: Option[String], st: String, dependencies: Set[Event], mappedValues: Map[Any, Any]) // TODO need to work on types
  {
    def dependsOn(e: Event): Event = this.copy(dependencies = dependencies + e)
  }

  object Event
  {
    def create(st: Statement): Event = Event(None, st.toString, Set(), Map())

    def create(eventId: String, st: Statement): Event = Event(Some(eventId), st.toString, Set(), Map())
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
   *
   * TODO Investigate if this is possible just by looking at statement (probably update) itself.
   *
   * Restore semantics:
   *   If Update then restore means having data which was before UPDATE -> Another Update statement with previous data
   *   If Insert then restore means having no data -> Delete inserted row
   *
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

  import system.dispatcher
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
    val independentUpdateEvent = Event.create(independentUpdate)

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

  /**
   * Domain: Music
   *
   * Entities:
   *   Song
   *   Album
   *   Artists
   *   Genre
   *
   *   Use cases:
   *    Use case 1:  Add new album of some artist Xxx Yyy
   *       uses case: Find or Create artist
   *
   *       1. Find or create artist
   *       2. Create Id for Album
   *       3. Create Ids for songs
   *       4. Insert Album with song ids and names and with artist id
   *       5. Insert Songs
   *
   *       Steps 4 & 5 could run concurrently
   *
   *
   *    Use case 2: Find or Create artist by full name
   *
   *      Artist name is Xxxx
   *
   *      1. Get up to first 2 letters from artist name
   *      2. Create key: up to first 2 letters, artist name
   *      3. Query for artist
   *      4. If exists then return him
   *      5. If doesn't exist then insert new artist and return his ID
   *
   *
   *
   *    II Update
   *
   *
   *
   *      Complex scenario
   *
   *      1. Query for something ( this data could be changing and we care about state before next insert)
   *      2. Insert data based on that query
   *      3. Query something else
   *      4. Calculate something based on previous query results.
   *      5. Update something with calculated value
   *
   *      How to pass data between events?
   *
   *      1. Query for something (this data could be changing and we care about state before next insert)
   *      2. Insert data based on that query
   *
   *      Concrete example:
   *
   *      Query all songs
   *      Insert new song with number := total of songs + 1
   *
   *      SELECT * FROM SONGS WHERE ALBUM_ID = 62c36092-82a1-3a00-93d1-46196ee77204;
   *
   *
   *      Passing result of Query to event:
   *        - expression like: result.one().getString("song_name")
   *        - function name
   *            where function is in library accessible by cassandra node AND function has type
   *               F: ResultSet -> Map[key: String, ByteString]
   *        - callback to user's actor which receives Map[eventName: String, result: ResultSet]
   *             and returns Map[key: String, ByteString ???]
   *
   *
   *       Example:
   *
   *
   *
   *
   *      To do that query needs to be executed and query results have to be passed into next event
   *
   *
   */
  object AlbumsAndSongsDomainExample {
    case class Song(name: String, order: Int, genre: String, albumId: UUID)
    case class SongInAlbum(name: String, songId: UUID)
    case class Album(name: String, artist: String, artistId: UUID)

    val albumId = "62c36092-82a1-3a00-93d1-46196ee77204"
    val queryForCountOfAllSongsInAlbum: Statement = QueryBuilder.select().countAll().from("MUSIC", "SONGS").where(QueryBuilder.eq("album_id", albumId))




  }

  object EventDataPassing {

    val queryForSongsCount = "SELECT COUNT(*) FROM songs WHERE album_name = 'SnowBiz'"

//    count
//    -------
//    12
//
//    (1 rows)

    val updateStatementString = "UPDATE artist SET songs_count = :incremented WHERE name = 'Muse'"


    private val queryCountSongsEvt: Event = Event.create("query_count",
                                                         QueryBuilder.select().
                                                           countAll().
                                                           from("music", "songs").
                                                           where(QueryBuilder.eq("album_name", "SnowBiz")))

    trait DataTransformation

    trait DataTransformationFunctionName

    object IncrementIntUnaryFunction extends DataTransformationFunctionName {
        // TODO def some increment method
    }

    case class DataTransformationScalarExpression(eventId: String, column: String) extends DataTransformation
    case class DataTransformationUnaryFunction(funtion: DataTransformationFunctionName, argument: DataTransformation)


    val updateEvent: Event = Event(eventId = None,
                                   st = updateStatementString,
                                   dependencies = Set(queryCountSongsEvt),
                                   mappedValues = Map("incremented" -> /* TODO need to work on that */
                                                      DataTransformationUnaryFunction(IncrementIntUnaryFunction, DataTransformationScalarExpression("query_count", "count"))))

  }
}
