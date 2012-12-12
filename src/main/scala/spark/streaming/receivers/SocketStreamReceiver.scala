package spark.streaming.receivers

import spark.storage.StorageLevel
import spark.Logging

import akka.actor.{ Actor, ActorRef, actorRef2Scala }
import akka.actor.{ IO, IOManager }
import akka.actor.PoisonPill
import akka.actor.Props
import akka.util.ByteString

/**
 * A receiver receives input from a network socket.
 */
class SocketStreamReceiver[T: ClassManifest](host: String,
  port: Int,
  bytesToObjects: ByteString => Iterator[T],
  streamId: Int,
  storageLevel: StorageLevel) extends AbstractActorReceiver[T](streamId, storageLevel) {

  require(bytesToObjects != null)

  override protected val actorInstanceFactory = () => new SocketReceiverActor

  override protected val actorName = "SocketReceiver"

  protected class SocketReceiverActor extends Actor with Logging {

    override def preStart = IOManager(env.actorSystem).connect(host, port)

    def receive: Receive = {

      case IO.Connected(socket, address) =>
        logInfo("Successfully connected to " + address)

      case IO.Read(socket, bytes) =>
        pushBlock("input-" + streamId + "-" + System.nanoTime, bytesToObjects(bytes))

      case IO.Closed(socket: IO.SocketHandle, cause) =>
        logInfo("Socket has closed, cause: " + cause)
        stop()
    }
  }
}

