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

import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.io.Source

/**
 *
 *  Experiments with serializing Props as a way of moving actors between nodes. (move computation to data)
 *
 *  Pickling doesn't work - it can serialize class to json, but it cannot deserialize it.
 *
 *  TODO try spray-json or GSON or something.
 *
 *  IMPORTANT: during deserialization (potentially on different node)
 *  {@code Thread.currentThread().setContextClassLoader(customClazzLoader)}
 *  should be used to CustomClassLoader which knows about these actor classes and everything that it depend on.
 *
 *
 * @author Marek Lewandowski <marek.m.lewandowski@gmail.com>
 * @since 11/07/15
 */
class SerializablePropsTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
                                                          with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SerializableProps"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  def serializeToBytes(a: AnyRef): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(stream)

    out.writeObject(a)
    out.close()
    stream.close()
    stream.toByteArray
  }

  def deserializeFromBytes(in: Array[Byte]): AnyRef = {
    val inputBytes: ByteArrayInputStream = new ByteArrayInputStream(in)
    val objectInputStream: ObjectInputStream = new ObjectInputStream(inputBytes)
    val readObject: AnyRef = objectInputStream.readObject()
    objectInputStream.close()
    inputBytes.close()
    readObject
  }

  "Class[T] " must {
    "be serializable via pickle" in {
      import scala.pickling.Defaults._, scala.pickling.json._
      val clazz: Class[SimpleActor] = classOf[SimpleActor]

      val pickle = clazz.pickle
      println("printing clazz with pickle")
      println(pickle.value)

      val actual: Class[SimpleActor] = pickle.unpickle[Class[SimpleActor]]

      actual shouldEqual clazz
    }

    "be serializable using serialization API" in {
      val clazz: Class[SimpleActor] = classOf[SimpleActor]

      val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
      val out = new ObjectOutputStream(stream)

      out.writeObject(clazz)
      out.close()

      stream.close()

      val string = stream.toByteArray.map(_.toString).mkString
      val string2 = new String(stream.toByteArray, java.nio.charset.StandardCharsets.UTF_8)

      println("printing string ")
      println(string)

      println("####")
      println(string2)

      val inputBytes: ByteArrayInputStream = new ByteArrayInputStream(stream.toByteArray)

      val objectInputStream: ObjectInputStream = new ObjectInputStream(inputBytes)

      val readClazz: AnyRef = objectInputStream.readObject()

      readClazz shouldEqual clazz
      println("readClazz is same as clazz")
      println(readClazz)
      println(clazz)

    }
  }

  "Serializable props" must {

    val expectedNumber = 5
    val expectedText = "Foo bar tra la la la"

    def getProps = Props(classOf[SimpleActor], expectedNumber, expectedText)

    import scala.pickling.Defaults._, scala.pickling.json._

    "be serializable" ignore {

      val simpleActorProps = getProps

      val propsPickle = simpleActorProps.pickle

      val json = propsPickle.value

      json should include ("5")
      json should include (expectedText)
      json should include ("{")
      json should include ("}")

      println(json)
    }

    "Props serialized and deserialized using pickling should be same" ignore {
      val simpleActorProps = getProps

      val propsPickle = simpleActorProps.pickle

      val deserializedSimpleActorProps = JSONPickle(propsPickle.value).unpickle[Props]

      deserializedSimpleActorProps shouldEqual simpleActorProps
    }


    "Props serialized and deserialized using standard should be same" in {
      val simpleActorProps = getProps

      val serializedBytes = serializeToBytes(simpleActorProps)

      val deserializedProps = deserializeFromBytes(serializedBytes)

      deserializedProps shouldEqual simpleActorProps
    }

    "Actor created from Props and deserialized (pickle) Props should behave in same way" ignore {
      val simpleActorProps = getProps

      val simpleActorOg = system.actorOf(simpleActorProps)

      val simpleActorDeserialized = system.actorOf(JSONPickle(simpleActorProps.pickle.value).unpickle[Props])

      testActorBevaiour(simpleActorOg)
      testActorBevaiour(simpleActorDeserialized)
    }

    "Actor created from Props and deserialized (standard) Props should behave in same way" in {
      val simpleActorProps = getProps

      val simpleActorOg = system.actorOf(simpleActorProps)

      val simpleActorDeserialized = system.actorOf(deserializeFromBytes(serializeToBytes(simpleActorProps)).asInstanceOf[Props])

      testActorBevaiour(simpleActorOg)
      testActorBevaiour(simpleActorDeserialized)
    }

    def testActorBevaiour(actorRef: ActorRef) = {
      actorRef ! "get int"
      expectMsg(expectedNumber)
      expectNoMsg()
      actorRef ! "get text"
      expectMsg(expectedText)
      expectNoMsg()
    }


  }
}

class SimpleActor(val number: Int, val text: String) extends Actor {

  override def receive: Receive = {
    case "get int" => sender ! number
    case "get text" => sender ! text
  }
}
