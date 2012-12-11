---
layout: global

title: ZeroMQ Stream setup guide
---

## Install ZeroMQ

To work with zeroMQ, some native libraries have to be installed.

1. Install zeroMQ core libraries. [ZeroMQ Install guide](http://www.zeromq.org/bindings:java)

2. Install java bindings. [JZMQ](http://www.zeromq.org/bindings:java)

3. Copy the generated zmq.jar to `$SPARK_HOME/lib` or `$PROJECT_HOME/lib`


## Classpath settings

The run command is pre-configured to pick up the jar and look into native libraries. Incase you want to run it using some other way following steps might be necessary.

1. Put the jar zmq.jar on your classpath.

2. Start JVM with the location of installed libraries usually:`/usr/local/lib/` by: `-Djava.library.path=/usr/local/lib/` or alternatively `export JAVA_OPTS=" -Djava.library.path=/usr/local/lib/`


_An important note: the presence of the library zmq.jar is important for enabling sending of messages, its absence will not produce any errors_

## Sample scala code

A publisher is an entity assumed to be outside the spark ecosystem. A sample zeroMQ publisher is provided to try out the sample spark ZeroMQ application.

1. Start the sample publisher.

{% highlight scala %}

      val acs: ActorSystem = ActorSystem()

      val pubSocket = ZeroMQExtension(acs).newSocket(SocketType.Pub, Bind(url))

      pubSocket ! ZMQMessage(Seq(Frame("topic"), Frame("My message".getBytes)))


{% endhighlight %}

It does nothing more than publishing the message on the specified topic

2. Start the spark application by plugging the zeroMQ stream receiver.

{% highlight scala %}

	val lines = ssc.pluggableNetworkStream[String]((x, y) =>

	new ZeroMQMessageStreamReceiver[String](url, Subscribe("topic"), bytesToObjectsIterator, x, y))

{% endhighlight %}


bytesToObjectsIterator is going to be a function for decoding the Frame data.

_For example: For decoding into strings using default charset:_

{% highlight scala %}

    def bytesToStringIterator(x: Seq[Seq[Byte]]) = (x.map(x => new String(x.toArray))).iterator

{% endhighlight %}
