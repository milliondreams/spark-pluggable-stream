package spark.streaming.examples

import akka.zeromq.Subscribe
import spark.streaming.Seconds
import spark.streaming.StreamingContext
import spark.streaming.StreamingContext.toPairDStreamFunctions
import spark.streaming.receivers.ZeroMQMessageStreamReceiver
import spark.streaming.PluggableNetworkStream

object WordCountZeroMQ {

  def main(args: Array[String]) = {

    if (args.length < 4) {
      System.err.println(
        "Usage: WordCountZeroMQ <master> <batch-duration in seconds> <zeroMQurl> <topic>" +
          "In local mode, <master> should be 'local[n]' with n > 1")
      System.exit(1)
    }

    val Seq(master, batchDuration, url, topic) = args.toSeq

    // Create the context and set the batch size
    val ssc = new StreamingContext(master, "WordCountZeroMQ", 
        Seconds(batchDuration.toLong)) with PluggableNetworkStream

    def bytesToStringIterator(x: Seq[Seq[Byte]]) = (x.map(x => new String(x.toArray))).iterator

    //For this stream, a zeroMQ publisher should be running. 
    val lines = ssc.pluggableNetworkStream((x, y) => new ZeroMQMessageStreamReceiver(
      url, Subscribe(topic), bytesToStringIterator, x, y))

    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)

    wordCounts.print()
    ssc.start()
  }
}