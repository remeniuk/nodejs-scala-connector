/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.remote

import com.vasilrem.remote.protocol.serializer._
import java.io._
import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote._
import scala.actors.remote.RemoteActor._
import scala.actors.remote.proto._
import com.vasilrem.remote.protocol.RemoteActorProtocol._

object TestRemoteProtoActor{
  scala.actors.Debug.level_=(100)
  println("Starting dummy remote actor...")

  actor{
    RemoteProtoActor.alive(12345)
    RemoteProtoActor.register('server, self)
    loop {
      react {
        case msg:StringMessage => println(msg.getMessage)
          reply(StringMessage.newBuilder.setMessage("Server replied to [%s]" format(msg.getMessage)))
        case unknown => throw new Exception("Unknown message %s was received by remote actor!" format(unknown))
      }
    }
  }

  println("Started.")
}
