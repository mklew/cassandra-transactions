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

import _root_.akka.actor.Props
import com.typesafe.scalalogging.StrictLogging
import mklew.cts.akka.CtsModule
import mklew.cts.cluster.ClusterSeedObserver

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 13/06/15
 */
object Boot extends App with StrictLogging with CtsModule
{

  logger.info(
    """
      |  ==============================================
      |
      |     Cassandra Transactions seed node starts
      |
      |  ==============================================
    """.stripMargin)

  // Create an Akka system
  val system = actorSystem
  // Create an actor that handles cluster domain events
  system.actorOf(Props[ClusterSeedObserver], name = "clusterObserver")
}
