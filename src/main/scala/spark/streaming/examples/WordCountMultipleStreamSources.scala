package spark.streaming.examples

import spark.streaming.Seconds
import spark.streaming.StreamingContext
import spark.streaming.StreamingContext.toPairDStreamFunctions
import spark.streaming.receivers.MessageStreamReceiver
import spark.streaming.receivers.SocketStreamReceiver
import spark.streaming.PluggableNetworkStream

/**
 * A sample program demonstrating the use of multiple stream sources of different types.
 */
object WordCountMultipleStreamSources {
  def main(args: Array[String]) {
    if (args.length < 6) {
      System.err.println(
        "Usage: WordCountMultipleStreamSources <master> <batch-duration in seconds>" +
          " <remoteAkkaHost> <remoteAkkaPort> <remoteSocketHost> <remoteSocketPort>" +
          "In local mode, <master> should be 'local[n]' with n > 2")
      System.exit(1)
    }

    val Seq(master, batchDuration, remoteAkkaHost,
      remoteAkkaPort, remoteSocketHost, remoteSocketPort) = args.toSeq

    // Create the context and set the batch size
    val ssc = new StreamingContext(master, "WordCountMultipleStreamSources",
      Seconds(batchDuration.toLong)) with PluggableNetworkStream

    //Plug in multiple stream sources.

    //1.For this stream input to work, a feeder should be started to which it goes and subscribe.
    // A sample is provided FileTextStreamFeeder 
    val lines = ssc.pluggableNetworkStream[String]((x, y) => new MessageStreamReceiver(
      "akka://spark@%s:%s/user/FeederActor".format(remoteAkkaHost, remoteAkkaPort.toInt), x, y))

    //2.For this stream, nc can be used to generate stream input manually, i.e. on return key
    val lines2 = ssc.pluggableNetworkStream((x, y) => new SocketStreamReceiver(
      remoteSocketHost, remoteSocketPort.toInt, z => Seq(z.utf8String).iterator, x, y))

    val words = (lines union lines2).flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)

    wordCounts.print()
    ssc.start()

  }
}
