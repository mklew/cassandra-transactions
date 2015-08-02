import akka.actor._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import mklew.cts.{Ping, Pong}

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

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

/**
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 02/08/15
 */
object ClientBoot extends App with StrictLogging
{
  val config = ConfigFactory.load("application.conf")
  lazy val actorSystem = ActorSystem("CtsClient", config)
  logger.info(
    """
      |  ==============================================
      |
      |     Cassandra Transactions client starts
      |
      |  ==============================================
    """.stripMargin)

  // Create an Akka system
  val system = actorSystem
  // Create a pong actor
  system.actorOf(Props[ClientPingActor], name = "ping")
}

class ClientPingActor extends Actor with ActorLogging {
  private val actorSelection: ActorSelection = context.actorSelection("akka.tcp://CtsCassandraServer@127.0.0.1:50123/user/pingpong")

  import context.dispatcher

  context.system.scheduler.schedule(FiniteDuration(1, duration.SECONDS), FiniteDuration(5, duration.SECONDS), self, SendPing)

  var pingCounter = 0L

  override def receive: Receive = {
    case SendPing =>
      val ping: Ping = mklew.cts.Ping(s"Ping[$pingCounter}]")
      log.info(getClass.getSimpleName + " sending ping " + ping)
      actorSelection ! ping
      pingCounter += 1
    case Pong(msg, ping) =>
      log.info(getClass.getSimpleName + " received Pong message: " + msg)
  }
}

object SendPing
