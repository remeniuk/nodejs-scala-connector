/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scala.actors.remote.proto

import com.vasilrem.remote._
import com.vasilrem.remote.protocol.serializer.ProtobufSerializer
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import scala.actors.Debug
import scala.actors.remote._
import scala.collection.mutable.HashMap
import scala.util.Random

object TcpProtoService {

  private val random = new Random
  private val ports = new HashMap[Int, TcpProtoService]
  	
  def apply(port: Int, cl: ClassLoader): TcpProtoService =
    ports.get(port) match {
      case Some(service) =>
        service
      case None =>
        val service = new TcpProtoService(port, cl)
        ports += Pair(port, service)
        service.start()
        Debug.info("created service at "+service.node)
        service
    }

}

class TcpProtoService(port: Int, cl: ClassLoader) extends TcpService(port, cl){

  override val serializer: ProtobufSerializer = new ProtobufSerializer(this, cl)

  override def terminate() {
    shouldTerminate = true
    super.terminate
  }

  private var shouldTerminate = false

  override def run() {
    try {
      val socket = new ServerSocket(port)
      while (!shouldTerminate) {
        Debug.info(this+": waiting for new connection on port "+port+"...")
        val nextClient = socket.accept()
        if (!shouldTerminate) {
          val worker = new TcpServiceWorker(this, nextClient)
          Debug.info("Started new "+worker)
          worker.readNode
          val clientNode = Node(nextClient.getInetAddress.getHostAddress, nextClient.getPort)
          println("Adding connection to %s" format(clientNode))
          addConnection(clientNode, worker)
          worker.start()          
        } else
          nextClient.close()
      }
    } catch {
      case e: Exception =>
        Debug.info(this+": caught "+e)
    } finally {
      Debug.info(this+": shutting down...")
    }
  }

}
