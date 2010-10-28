/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scala.actors.remote.proto

import scala.actors.AbstractActor
import scala.actors.Actor
import scala.actors.Debug
import scala.actors.remote._

object RemoteProtoActor {

  private val kernels = new scala.collection.mutable.HashMap[Actor, NetKernel]

  /* If set to <code>null</code> (default), the default class loader
   * of <code>java.io.ObjectInputStream</code> is used for deserializing
   * objects sent as messages.
   */
  private var cl: ClassLoader = null

  def classLoader: ClassLoader = cl
  def classLoader_=(x: ClassLoader) { cl = x }

  /**
   * Makes <code>self</code> remotely accessible on TCP port
   * <code>port</code>.
   */
  def alive(port: Int): Unit = synchronized {
    createNetKernelOnPort(port)
  }

  private def createNetKernelOnPort(port: Int): NetKernel = {
    val serv = TcpProtoService(port, cl)
    val kern = serv.kernel
    val s = Actor.self
    kernels += Pair(s, kern)

    s.onTerminate {
      Debug.info("alive actor "+s+" terminated")
      // remove mapping for `s`
      kernels -= s
      // terminate `kern` when it does
      // not appear as value any more
      if (!kernels.valuesIterator.contains(kern)) {
        Debug.info("terminating "+kern)
        // terminate NetKernel
        kern.terminate()
      }
    }

    kern
  }

  @deprecated("this member is going to be removed in a future release")
  def createKernelOnPort(port: Int): NetKernel =
    createNetKernelOnPort(port)

  /**
   * Registers <code>a</code> under <code>name</code> on this
   * node.
   */
  def register(name: Symbol, a: Actor): Unit = synchronized {
    val kernel = kernels.get(Actor.self) match {
      case None =>
        val serv = TcpProtoService(TcpService.generatePort, cl)
        kernels += Pair(Actor.self, serv.kernel)
        serv.kernel
      case Some(k) =>
        k
    }
    kernel.register(name, a)
  }

  private def selfKernel = kernels.get(Actor.self) match {
    case None =>
      // establish remotely accessible
      // return path (sender)
      createNetKernelOnPort(TcpService.generatePort)
    case Some(k) =>
      k
  }

  /**
   * Returns (a proxy for) the actor registered under
   * <code>name</code> on <code>node</code>.
   */
  def select(node: Node, sym: Symbol): AbstractActor = synchronized {
    selfKernel.getOrCreateProxy(node, sym)
  }

  private[remote] def someNetKernel: NetKernel =
    kernels.valuesIterator.next

  @deprecated("this member is going to be removed in a future release")
  def someKernel: NetKernel =
    someNetKernel

}
