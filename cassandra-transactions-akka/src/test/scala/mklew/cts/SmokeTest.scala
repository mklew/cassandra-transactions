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

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
import akka.actor.{Identify, ActorSystem, Actor, Props}
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import mklew.cts.events.ReceptionistProtocol
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class SmokeTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                           with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SmokeTest"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }


  "A Single Node Boot" must {

    SingleNodeBoot.startup(system)

    "spin up node manager and receptionist" in {
//      val echo = system.actorOf(TestActors.echoActorProps)
//      echo ! "hello world"
//      expectMsg("hello world")



      val nodeManagerSelection = system.actorSelection("/user/NodeManager")

      nodeManagerSelection ! Identify
      expectMsg(Identify)

      val nodeReceptionistSelection = system.actorSelection("/user/Receptionist")
      nodeReceptionistSelection ! Identify
      expectMsg(Identify)
    }

    "receptionist should tell about node manager" in {
      val nodeReceptionistSelection = system.actorSelection("/user/Receptionist")

      nodeReceptionistSelection ! ReceptionistProtocol.GetNodeManager

      val nodeManagerRef = expectMsgType[ReceptionistProtocol.NodeMangerRef]

      nodeManagerRef.actorRef ! Identify
      expectMsg(Identify)
    }
  }
}
