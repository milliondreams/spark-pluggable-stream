package spark.streaming.examples

import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.zeromq.{ Bind, Frame, SocketType, ZMQMessage, ZeroMQExtension }

/**
 * A simple publisher for demonstration purposes, repeatedly publishes ping Messages
 * on each given interval.
 */
object SimpleZeroMQPublisher {

  def main(args: Array[String]) = args.toList match {

    case url :: topic :: interval :: messages =>
      val acs: ActorSystem = ActorSystem()
      val pubSocket = ZeroMQExtension(acs).newSocket(SocketType.Pub, Bind(url))
      while (true) {
        Thread.sleep(interval.toLong)
        pubSocket ! ZMQMessage(Frame(topic) :: messages.map(x => Frame(x.getBytes)).toList)
      }

      acs.awaitTermination()

    case _ =>
      System.err.println("Usage: SimpleZeroMQPublisher <zeroMQUrl> <topic> " +
      		"<interval(in millis)> <pingMessages>")
      System.exit(1)
  }

}