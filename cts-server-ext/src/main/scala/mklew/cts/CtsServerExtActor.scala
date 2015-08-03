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

import java.net.InetAddress

import akka.actor.{Inbox, ActorSystem, Actor, Props}
import mklew.cts.events.BaseCtsActor
import org.apache.cassandra.gms._

/**
 *  Root actor for each Cassandra node.
 *  Given that it is loaded from same classloader as rest of Cassandra it can use Cassandra singletons.
 *
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 03/08/15
 */
class CtsServerExtActor extends BaseCtsActor {

  var ctsGossiperListener = context.actorOf(Props[CtsGossiperListenerActor], "gossiper")

  override def receive: Receive = {
    case msg => log.info("[CTS ROOT]" + msg)
  }
}

class CtsGossiperListener(val actorSystem: ActorSystem) extends IEndpointStateChangeSubscriber {

  val inbox: Inbox = Inbox.create(actorSystem)

  lazy val gossiperActor = actorSystem.actorSelection(s"/user/${CtsServerExtActor.actorName}/gossiper")

  import CtsGossiperListenerActorProtocol._

  def tellActor(msg: Any) = gossiperActor.tell(msg, inbox.getRef())

  override def onJoin(endpoint: InetAddress, epState: EndpointState): Unit = tellActor(OnJoin(endpoint, epState))

  override def onRestart(endpoint: InetAddress, state: EndpointState): Unit = tellActor(OnRestart(endpoint, state))

  override def onChange(endpoint: InetAddress, state: ApplicationState, value: VersionedValue): Unit = tellActor(OnChange(endpoint, state, value))

  override def beforeChange(endpoint: InetAddress, currentState: EndpointState, newStateKey: ApplicationState, newValue: VersionedValue): Unit = {}

  override def onRemove(endpoint: InetAddress): Unit = tellActor(OnRemove(endpoint))

  override def onDead(endpoint: InetAddress, state: EndpointState): Unit = tellActor(OnDead(endpoint, state))

  override def onAlive(endpoint: InetAddress, state: EndpointState): Unit = tellActor(OnAlive(endpoint, state))
}

class CtsGossiperListenerActor extends BaseCtsActor {
  import CtsGossiperListenerActorProtocol._

  def logIt(msg: Any) = {
    log.info("[CTS:CtsGossiperListenerActor] " + msg.toString)
  }

  override def receive: Actor.Receive = {
    case msg @ OnJoin(endpoint: InetAddress, epState: EndpointState) => logIt(msg)
    case msg @ OnAlive(endpoint: InetAddress, state: EndpointState) => logIt(msg)
    case msg @ OnDead(endpoint: InetAddress, state: EndpointState) => logIt(msg)
    case msg @ OnRemove(endpoint: InetAddress) => logIt(msg)
    case msg @ OnChange(endpoint: InetAddress, state: ApplicationState, value: VersionedValue) => logIt(msg)
    case msg @ OnRestart(endpoint: InetAddress, state: EndpointState) => logIt(msg)
  }
}

object CtsGossiperListenerActorProtocol {
  case class OnJoin(endpoint: InetAddress, epState: EndpointState)
  case class OnAlive(endpoint: InetAddress, state: EndpointState)
  case class OnDead(endpoint: InetAddress, state: EndpointState)
  case class OnRemove(endpoint: InetAddress)
  case class OnChange(endpoint: InetAddress, state: ApplicationState, value: VersionedValue)
  case class OnRestart(endpoint: InetAddress, state: EndpointState)
}

object CtsServerExtActor {
  val actorName = "cts"
}
