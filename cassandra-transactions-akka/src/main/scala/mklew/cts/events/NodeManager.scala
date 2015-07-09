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

import akka.actor.{Identify, ActorRef, Props}

/**
 * Error Kernel actor
 *
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
class NodeManager extends BaseCtsActor
{

  import mklew.cts.events.NodeProtocol._


  var eventPersister: Option[ActorRef] = None
  var eventExecutor: Option[ActorRef] = None
  var eventHandler: Option[ActorRef] = None

  override def receive: Receive =
  {
    case Identify => sender ! Identify
    case InitializeNodeManager => initialize()
    case RegisterWithReceptionist(receptionist) => receptionist ! ReceptionistInternalProtocol.RegisterNodeManager(self)
//    case SomeEventMsg => eventHandler.get.forward(msg)
  }

  def initialize() =
  {
    log.debug("initialize NodeManager")
    eventPersister = Some(context.actorOf(Props[EventPersister], "EventPersister"))
    eventExecutor = Some(context.actorOf(Props[EventExecutor], "EventExecutor"))
    eventHandler = Some(context.actorOf(Props(classOf[EventHandler], eventExecutor.get, eventPersister.get), "EventHandler"))
  }

}

object NodeProtocol
{

  case class RegisterWithReceptionist(receptionist: ActorRef)

  case object InitializeNodeManager

}
