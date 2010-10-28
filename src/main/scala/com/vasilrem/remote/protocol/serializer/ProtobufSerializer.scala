/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.remote.protocol.serializer

import scala.actors.remote.Service
import java.util.zip.Adler32
import scala.actors.remote.JavaSerializer
import scala.actors.remote.Locator
import scala.actors.remote.NamedSend
import scala.actors.remote.Node
import java.io._
import com.vasilrem.remote.protocol._
import com.google.protobuf.AbstractMessage.Builder
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessage
import com.vasilrem.remote.protocol.RemoteActorProtocol._

class ProtobufSerializer(serv: Service, cl: ClassLoader) extends JavaSerializer(serv, cl) {

  import ProtobufSerializer._

  @throws(classOf[IOException])
  override def writeObject(outputStream: DataOutputStream, obj: AnyRef) {
    val byteArray = serialize(obj)
    outputStream.writeInt(byteArray.length)    
    outputStream.write(byteArray)
    outputStream.flush
  }

  @throws(classOf[IOException]) @throws(classOf[ClassNotFoundException])
  override def readObject(inputStream: DataInputStream): AnyRef = {
    val size = inputStream.readInt
    val byteArray = new Array[Byte](size)
    inputStream.read(byteArray)
    deserialize(byteArray)
  }

  override def serialize(o: AnyRef) = {
    val message = o match {
      case node: Node => toNodeProtocol(node)
      case namedSend: NamedSend => toNamedSendProtocol(namedSend)
      case generic: Builder[_] => generic
      case unknown => throw new Exception("ProtobufSerializer: failed to serialize message [%s]" format(unknown))
    }
    GenericMessageProtocol.newBuilder.
    setFqcn(message.getClass.getEnclosingClass.getName).
    setMessage(message.build.toByteString).build.toByteArray
  }

  override def deserialize(bytes: Array[Byte]): AnyRef = {
    val genericMessage = GenericMessageProtocol.parseFrom(bytes)
    Class.forName(genericMessage.getFqcn).
    getDeclaredMethod("parseFrom", Array[Class[_]](classOf[Array[Byte]]):_*).
    invoke(null, genericMessage.getMessage.toByteArray) match {
      case node: NodeProtocol => toNode(node)
      case namedSend: NameSendProtocol => toNamedSend(namedSend)
      case message: GeneratedMessage => message
      case unknown => throw new Exception("ProtobufSerializer: failed to deserialize message [%s]" format(unknown))
    }
  }

}

object ProtobufSerializer {

  implicit def toNode(node: NodeProtocol) = Node(node.getHost, node.getPort toInt)

  implicit def toNodeProtocol(node: Node) = NodeProtocol.newBuilder.
  setHost(node.address).
  setPort(node.port toString)

  implicit def toLocator(locator: LocatorProtocol) = Locator(locator.getNode, Symbol(locator.getServiceName))

  implicit def toLocatorProtocol(locator: Locator) =
    LocatorProtocol.newBuilder.
  setNode(locator.node).
  setServiceName(locator.name.name)

  implicit def toNamedSend(namedSend: NameSendProtocol) =
    NamedSend(namedSend.getSender, namedSend.getReceiver, namedSend.getMessage.toByteArray, 'nosession)

  implicit def toNamedSendProtocol(namedSend: NamedSend) =
    NameSendProtocol.newBuilder.
  setSender(namedSend.senderLoc).
  setReceiver(namedSend.receiverLoc).
  setMessage(ByteString.copyFrom(namedSend.data))

  def arrayChecksum(byteArray: Array[Byte]) = {
    val checksumEngine = new Adler32()
    checksumEngine.update(byteArray, 0, byteArray.length)
    checksumEngine.getValue()
  }

}

