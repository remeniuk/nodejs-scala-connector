/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.remote.protocol.serializer

import com.vasilrem.remote.protocol.serializer._
import org.specs._
import scala.actors.remote._
import com.vasilrem.remote.protocol.RemoteActorProtocol._

class ProtobufSerializerSpec extends Specification {

  val serializer = new ProtobufSerializer(null, null)

  val dummySend = NamedSend(
    Locator(Node("10.6.122.29",8275),'remotesender0),
    Locator(Node("127.0.0.1",12345),'server),
    "msg".getBytes,
    'nosession)

  "Deserialization of a serialized object returns original object" in {
    val deserialized = (serializer deserialize(
        serializer serialize dummySend
      )).asInstanceOf[NamedSend]
    deserialized.senderLoc must be equalTo(dummySend.senderLoc)
    deserialized.receiverLoc must be equalTo(dummySend.receiverLoc)
  }

}
