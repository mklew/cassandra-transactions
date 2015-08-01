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

import akka.actor.{Props, Actor, ActorRef}
import mklew.cts.lookup.LookupProtocol

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
class EventHandler(val eventExecutor: ActorRef, val eventPersister: ActorRef) extends BaseCtsActor
{
  override def receive: Receive = {
    case x =>
      // 0. If in current time window when need to send to event executor, otherwise just persist
      // 1. Ask for event executor
      // 2. Send events to persist them
      // 3. Respond with Ack
      // 4. When event executor is received send him events for execution

      val sendToEventExecutorActor = context.actorOf(Props[SendToEventExecutorActor])

      context.actorSelection(LookupProtocol.path).tell(LookupProtocol.LookupEventExecutor, sendToEventExecutorActor)


      log.info(s"event handler received $x")
      unhandled(x)
  }

  class SendToEventExecutorActor extends BaseCtsActor {
    override def receive: Actor.Receive = {
      case LookupProtocol.EventExecutorRef(eventExecutorRef) =>

    }
  }
}



