package crdt.sample.vectorclock

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import crdt.sample.{Siblings, SetTopology, GetTopology}

class Master(vectorSize: Int) extends Actor with ActorLogging {

  var partitions = Vector.empty[Vector[ActorRef]]

  override def preStart(): Unit = {
    val partition1 = (for (i <- 0 to vectorSize) yield context.actorOf(Props(classOf[Node], i), s"Node$i")).toVector
    partition1 foreach (a => a ! Siblings(partition1.filter(_ != a)))
    partitions = Vector(partition1)
  }

  override def postStop(): Unit = {
    for (actor <- context.children) {
      context.stop(actor)
    }
  }

  override def receive: Receive = {
    case GetTopology => sender ! partitions
    case SetTopology(newPartitions: Vector[Vector[ActorRef]]) =>
      log.info(s"Received new topology {$newPartitions}")
      partitions = newPartitions
      for (partition <- partitions; actor <- partition) {
        actor ! Siblings(partition.filter(_ != actor))
      }
  }
}
