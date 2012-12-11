package spark.streaming

import spark.storage.StorageLevel

class PluggableReceiverInputDStream[T: ClassManifest](
  @transient ssc_ : StreamingContext,
  receiver: (Int, StorageLevel) => NetworkReceiver[T],
  storageLevel: StorageLevel) extends NetworkInputDStream[T](ssc_) {

  def createReceiver(): NetworkReceiver[T] = {
    receiver(id, storageLevel)
  }
}
