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

import akka.actor.{ActorSystem, Identify}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import mklew.cts.lookup.LookupProtocol
import mklew.cts.lookup.LookupProtocol.EventExecutorRef
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
class SingleNodeEventExecutorLookupTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                                      with WordSpecLike with Matchers with BeforeAndAfterAll
{
  def this() = this(ActorSystem("TestSystem", ConfigFactory.parseString(TestCfg.configStr)))

  "Single Node Event Executor Lookup" must
  {
    SingleNodeBoot.startup(system)

    "find event executor in current actor system" in
    {
      val lookupActor = system.actorSelection(LookupProtocol.path)

      def testLookup() = {
        lookupActor ! LookupProtocol.LookupEventExecutor
        val eventExecutorRefMsg = expectMsgType[EventExecutorRef]

        system.actorSelection("/user/NodeManager/EventExecutor") ! Identify

        expectMsg(Identify)

        assert(eventExecutorRefMsg.actorRef == lastSender, "ActorRef should be EventExecutor")
      }
      testLookup()
      testLookup()
      testLookup()
    }
  }

  override def afterAll()
  {
    TestKit.shutdownActorSystem(system)
  }

}

object TestCfg {
  val configStr =
    """
      |akka {
      |      loglevel = "DEBUG"
      |      stdout-loglevel = "DEBUG"
      |    }
    """.stripMargin

}

