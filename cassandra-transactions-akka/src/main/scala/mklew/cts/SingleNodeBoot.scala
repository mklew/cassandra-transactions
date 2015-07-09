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

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.StrictLogging
import mklew.cts.events.{NodeReceptionist, NodeManager}

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 09/07/15
 */
object SingleNodeBoot extends App with StrictLogging with CtsSingleModule
{
  logger.info(
    """
      |  =============================================================
      |
      |     Cassandra Transactions in Single Node Mode starts ...
      |
      |  =============================================================
    """.stripMargin)

  val system = actorSystem
  logger.info("spin up top level actors")
  startup(actorSystem)

  def startup(system: ActorSystem) = {
    // Create an actor that manages this akka node.
    logger.debug("spinning up actor of class: " + classOf[NodeManager].getSimpleName)
    val nodeManager = system.actorOf(Props[NodeManager], name = "NodeManager")

    logger.debug("spinning up actor of class: " + classOf[NodeReceptionist].getSimpleName)
    val receptionist = system.actorOf(Props[NodeReceptionist], name = "Receptionist")

    import mklew.cts.events.NodeProtocol._

    nodeManager ! InitializeNodeManager
    nodeManager ! RegisterWithReceptionist(receptionist)
  }
}
