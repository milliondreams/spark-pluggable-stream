package spark.streaming.receivers

import spark.streaming.NetworkReceiver
import spark.storage.StorageLevel
import spark.Logging

import akka.zeromq._
import akka.actor.{ Actor, ActorRef, actorRef2Scala }
import akka.actor.PoisonPill
import akka.actor.Props

/**
 * A receiver to subscribe to ZeroMQ stream.
 *
 * A zeroMQ stream publishes sequence of frames for each topic and each frame has
 * sequence of byte thus it needs the converter(which might be deserializer of bytes)
 * to translate from sequence of sequence of bytes, where sequence refer to a frame and
 * sub sequence refer to its payload.
 */
class ZeroMQMessageStreamReceiver[T: ClassManifest](urlOfPublisher: String,
  subscribe: Subscribe,
  bytesToObjects: Seq[Seq[Byte]] => Iterator[T],
  streamId: Int,
  storageLevel: StorageLevel) extends AbstractActorReceiver[T](streamId, storageLevel) {

  require(bytesToObjects != null)

  override protected val actorInstanceFactory = () => new ZeroMQMessageReceiverActor

  override protected val actorName = "ZeroMQMessageReceiver"

  protected class ZeroMQMessageReceiverActor extends Actor with Logging {

    override def preStart() = context.system.newSocket(SocketType.Sub, Listener(self),
      Connect(urlOfPublisher), subscribe)

    def receive: Receive = {

      case Connecting => logInfo("connecting ...")

      case m: ZMQMessage =>
        logDebug("Received message for:" + m.firstFrameAsString)

        //We ignore first frame for processing as it is the topic
        val bytes = m.frames.tail.map(_.payload)
        pushBlock("input-" + streamId + "-" + System.nanoTime, bytesToObjects(bytes))

      case Closed => logInfo("received closed ")

    }

  }
}
