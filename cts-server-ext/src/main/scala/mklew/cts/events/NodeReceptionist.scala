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

package mklew.cts.events

import akka.actor.{Identify, ActorRef}

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
class NodeReceptionist extends BaseCtsActor
{
  import ReceptionistProtocol._
  import ReceptionistInternalProtocol._

  var eventExecutor: Option[ActorRef] = None
  var nodeManger: Option[ActorRef] = None

  override def receive = {
    case GetEventExecutor => eventExecutor.map(EventExecutorRef).map(x => sender() ! x).getOrElse(sender() ! ActorNotFound)
    case GetNodeManager => nodeManger.map(NodeMangerRef).map(x => sender() ! x).getOrElse(sender() ! ActorNotFound)
    case RegisterNodeManager(actorRef) => nodeManger = Some(actorRef)
    case RegisterEventExecutor(actorRef) => eventExecutor = Some(actorRef)
    case Identify => sender ! Identify
  }
}

object ReceptionistProtocol {

  case object GetEventExecutor
  case class EventExecutorRef(actorRef: ActorRef)

  case object GetNodeManager
  case class NodeMangerRef(actorRef: ActorRef)
  case object ActorNotFound
}

private[events] object ReceptionistInternalProtocol {
  case class RegisterNodeManager(actorRef: ActorRef)
  case class RegisterEventExecutor(actorRef: ActorRef)
}
