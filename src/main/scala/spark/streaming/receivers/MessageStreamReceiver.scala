package spark.streaming.receivers

import akka.actor.{ Actor, ActorRef, actorRef2Scala }
import akka.actor.PoisonPill
import akka.actor.Props

import spark.storage.StorageLevel

case class SubscribeReceiver(receiverActor: ActorRef)
case class UnsubscribeReceiver(receiverActor: ActorRef)

class MessageStreamReceiver[T: ClassManifest](urlOfPublisher: String,
  streamId: Int,
  storageLevel: StorageLevel) extends AbstractActorReceiver[T](streamId, storageLevel) {

  override protected val actorInstanceFactory = () => new MessageReceiverActor

  override protected val actorName = "MessageReceiverActor"

  protected class MessageReceiverActor extends Actor {

    lazy private val remotePublisher = context.actorFor(urlOfPublisher)

    override def preStart = remotePublisher ! SubscribeReceiver(context.self)

    def receive: Receive = {
      case message =>
        pushBlock("input-" + streamId + "-" + System.nanoTime,
          Seq(message.asInstanceOf[T]).iterator)
    }

    override def postStop() = remotePublisher ! UnsubscribeReceiver(context.self)

  }
}


