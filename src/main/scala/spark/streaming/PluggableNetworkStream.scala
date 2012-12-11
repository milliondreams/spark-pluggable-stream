package spark.streaming

import spark.storage.StorageLevel

trait PluggableNetworkStream {

  this: StreamingContext =>

  def pluggableNetworkStream[T: ClassManifest](
    receiver: (Int, StorageLevel) => NetworkReceiver[T],
    storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER_2): DStream[T] = {
    val inputStream = new PluggableReceiverInputDStream[T](this,
      receiver,
      storageLevel)
    graph.addInputStream(inputStream)
    inputStream
  }

}