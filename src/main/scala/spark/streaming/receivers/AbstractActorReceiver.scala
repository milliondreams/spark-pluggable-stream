package spark.streaming.receivers

import akka.actor.{ Actor, ActorRef, Props, PoisonPill }
import spark.streaming.NetworkReceiver
import spark.storage.StorageLevel

abstract class AbstractActorReceiver[T: ClassManifest](streamId: Int,
  storageLevel: StorageLevel)
  extends NetworkReceiver[T](streamId) {

  var receiverActor: ActorRef = _

  protected val actorInstanceFactory: () => Actor
  protected val actorName: String

  override def onStart() = receiverActor = env.actorSystem.actorOf(
    Props(actorInstanceFactory), actorName)

  override def onStop() = receiverActor ! PoisonPill

  def pushBlock(blockId: String, iter: Iterator[T]) {
    pushBlock(blockId, iter, null, storageLevel)
  }
}