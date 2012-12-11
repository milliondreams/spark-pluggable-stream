---
layout: global
title: Tutorial - Spark streaming, Plugging in a custom receiver.
---

A "Spark streaming" receiver can be a simple network stream, streams of messages from a message queue, files etc. A receiver can also assume roles more than just receiving data like filtering, preprocessing, to name a few of the possibilities. The api to plug-in any user defined custom receiver is thus provided to encourage development of receivers which may be well suited to ones specific need.

This guide shows the programming model and features by walking through a simple sample receiver and corresponding Spark Streaming application.


## A quick and naive walk-through

### Write a simple receiver

This starts with implementing NetworkReceiver.

Following is a simple socket text-stream receiver, which is appearently overly simplified using Akka's socket.io api.

{% highlight scala %}

	class SocketTextStreamReceiver(host: String,
	  port: Int,
	  bytesToObjects: ByteString => Iterator[String],
	  streamId: Int,
	  storageLevel: StorageLevel) extends NetworkReceiver[String](streamId) {

	    var receiverActor: ActorRef = _

	    override def onStart = receiverActor = env.actorSystem.actorOf(Props(new SocketStreamReceiverActor),
	      "SocketTextReceiver")

	    override def onStop() = receiverActor ! PoisonPill

	    private class SocketStreamReceiverActor extends Actor {

	     override def preStart = IOManager(env.actorSystem).connect(host, port)

	     def receive: Receive = {

	       case IO.Read(socket, bytes) => pushBlock("input-" + streamId + "-" + System.nanoTime, bytesToObjects(bytes), storageLevel)

	     }
	  }

	}

{% endhighlight %}


_Please see implementations of NetworkReceiver for more generic NetworkReceivers._

### A sample spark application

* First create a Spark streaming context with master url and batchduration and mix in `PluggableNetworkStream` trait.

{% highlight scala %}

    val ssc = new StreamingContext(master, "WordCountCustomStreamSource",
      Seconds(batchDuration)) with PluggableNetworkStream

{% endhighlight %}

* Plug-in the network stream into the spark streaming context and create a DStream.

{% highlight scala %}

    val lines = ssc.pluggableNetworkStream((x, y) => new SocketTextStreamReceiver(
      "localhost",8445, z => Seq(z.utf8String).iterator, x, y))

{% endhighlight %}

 _Parameters x and y signify the streamId and storageLevel, They may or may not be overridden_

* Process it.

{% highlight scala %}

    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)

    wordCounts.print()
    ssc.start()

{% endhighlight %}


* After processing it, stream can be tested using the netcat utility.

     $ nc -l localhost 8445
     hello world
     hello hello


## Multiple homogeneous/heterogeneous receivers.

A DStream union operation is provided for taking union on multiple input streams.

{% highlight scala %}

    val lines = ssc.pluggableNetworkStream((x, y) => new SocketTextStreamReceiver(
      "localhost",8445, z => Seq(z.utf8String).iterator, x, y))

    val lines2 = ssc.pluggableNetworkStream((x, y) => new SocketTextStreamReceiver(
      "localhost",8446, z => Seq(z.utf8String).iterator, x, y))

    val union = lines.union(lines2)

{% endhighlight %}

Above stream can be easily process as described earlier.

_A more comprehensive example is provided in the spark streaming examples_


## More than just a naive receiver.

Multiple receivers may receive streams from different sources and preprocessing/filtering etc.. can makes it an interesting use case.

* Let us write a function for filtering word "hello" from input stream.

  A more practical application can be filtering a stream of stocks or a stream of social media with "interesting words"

{% highlight scala %}

    def bytesToIterator(z:String) = z.utf8String.split(" ").filter(_ != "hello").iterator

{% endhighlight %}


* Use this function to filter a stream in above mentioned receiver.

{% highlight scala %}

    val lines = ssc.pluggableNetworkStream((x, y) => new SocketTextStreamReceiver(
      "localhost",8445, bytesToIterator, x, y))

{% endhighlight %}


Now with this possibilities are endless and open for exploration.


## Advanced/experimental stuff using eventsourced.

* Eventsourced provides a nice abstraction for a receiver like maintaining sequence ids of received messages and support for eventsourced Receiver. Which means a receiver in case of crash can be resurrected by replaying all the messages by enabling journaling using eventsourced APIs. These are set of stackable traits and thus can be plugged in with our existing set of receivers to enhance their capabilities. This mandates a receiver has to be an actor (as described in the above example) for this to be applicable. A more in-depth discussion can be a topic for another document where things like how actor supervision/behaviour can cover more versatile use cases of receivers.
