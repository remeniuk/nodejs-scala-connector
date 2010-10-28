/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.remote

import com.vasilrem.remote.protocol.serializer._
import java.io.ByteArrayInputStream
import java.io._
import java.net.Socket
import java.util.zip.Adler32
import java.util.zip.CheckedInputStream
import org.specs._
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote._
import scala.actors.remote.RemoteActor._
import scala.actors.remote.proto._
import com.vasilrem.remote.protocol.RemoteActorProtocol._

class ProtoServiceSpecification extends Specification {

  doBeforeSpec {
    scala.actors.Debug.level_=(100)

    val forwarder = actor{
      loop{
        react{
          case msg => reply(msg)
        }
      }
    }

    println("Starting dummy remote actor...")
    actor{
      link(forwarder)
      RemoteProtoActor.alive(12345)
      RemoteProtoActor.register('server, self)
      loop {
        react {
          case msg:StringMessage => println(msg.getMessage)
            forwarder forward(StringMessage.newBuilder.setMessage("Server replied to [%s]" format(msg.getMessage)))
          case unknown => throw new Exception("Unknown message %s was received by remote actor!" format(unknown))
        }
      }
    }
    println("Started.")
  }

  "Message serialized into protobuf is sent directly via socket to remote actor" in {
    val socket= new Socket("localhost", 12345)
    val ser = new ProtobufSerializer(null, null)
    val dataout = new DataOutputStream(socket.getOutputStream)
    val datain = new DataInputStream(socket.getInputStream)

    try{
      val data = ser.serialize(Node("localhost", 12345))
      dataout.writeInt(data.length)
      println("Checksum of MessageTypeProtocol[%s]: %s" format(data.length, ProtobufSerializer.arrayChecksum(data)))
      dataout.write(data)

      val data2 = ser.serialize(
        NamedSend(
          Locator(Node(socket.getLocalAddress.getHostAddress, socket.getLocalPort),'remotesender0),
          Locator(Node("127.0.0.1",12345),'server),
          ser.serialize(StringMessage.newBuilder.setMessage("Hello!")),
          'nosession)
      )
      dataout.writeInt(data2.length)
      dataout.write(data2)
      dataout.flush()

      val serverResponse = ser.deserialize(ser.readObject(datain).asInstanceOf[NamedSend].data)
      .asInstanceOf[StringMessage]
      println("Server response %s" format(serverResponse))
      serverResponse.getMessage must be equalTo("Server replied to [Hello!]")
    } finally{
      datain.close
      dataout.close
      socket.close
    }
  }

  doAfterSpec {
    println("Stopping dummy remote actor...")
    actor {
      select(Node("127.0.0.1", 12345), 'server) ! exit
    }
    println("Stopped.")
  }

}
