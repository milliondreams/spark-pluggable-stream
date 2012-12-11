package spark.streaming.receivers

import akka.actor.{ Actor, ActorRef, actorRef2Scala }
import akka.actor.PoisonPill
import akka.actor.Props

import spark.storage.StorageLevel
import spark.streaming.NetworkReceiver

case class SubscribeReceiver(receiverActor: ActorRef) 
case class UnsubscribeReceiver(receiverActor: ActorRef)

class MessageStreamReceiver[T: ClassManifest](urlOfPublisher: String,
  streamId: Int,
  storageLevel: StorageLevel) extends NetworkReceiver[T](streamId) {

  lazy protected val remotePublisher = env.actorSystem.actorFor(urlOfPublisher)

  var receiverActor: ActorRef = _

  override def onStart() = receiverActor = env.actorSystem.actorOf(
    Props(new MessageStreamReceiverActor), "MessageStreamReceiver")

  override def onStop() = receiverActor ! PoisonPill

  private class MessageStreamReceiverActor extends Actor {

    override def preStart = remotePublisher ! SubscribeReceiver(context.self)

    def receive: Receive = {
      case message =>
        pushBlock("input-" + streamId + "-" + System.nanoTime, Seq(message.asInstanceOf[T]).iterator, null,storageLevel)
    }

    override def postStop() = remotePublisher ! UnsubscribeReceiver(context.self)

  }

}


