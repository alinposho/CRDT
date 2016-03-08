import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import crdt.sample.counter.Master
import crdt.sample._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Main {

  implicit val timeout: akka.util.Timeout = 5.seconds

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("mySystem")
    val master = system.actorOf(Props(classOf[Master], counter.VectorSize))

    val initialTopology = Await.result(master ? GetTopology, 5.seconds).asInstanceOf[Vector[Vector[ActorRef]]]

    for (i <- 0 until counter.VectorSize) {
      initialTopology(0) foreach { actor => actor ! Inc(i) }
    }

    val (p1, p2) = initialTopology(0).splitAt(1)
    master ! SetTopology(Vector(p1, p2))
    Thread.sleep(1000)

    // Since we have a partition we should notice divergent values
    p1 foreach (actor => actor ! Inc(1))
    var resPartition1 = getStateFrom(node=p1(0))
    println(s"results partition1=$resPartition1")

    var resPartition2 = getStateFrom(node=p2(0))
    println(s"results partition2=$resPartition2")

    p2(0) ! Inc(2)
    p2(0) ! Inc(2)
    p1(0) ! Inc(2)
    Thread.sleep(1000)
    resPartition2 = getStateFrom(node=p2(1))
    println(s"results partition2=$resPartition2")

    resPartition1 = getStateFrom(node=p1(0))
    println(s"results partition1=$resPartition1")

    // Heal the partition
    master ! SetTopology(initialTopology)
    Thread.sleep(1000)
    resPartition2 = getStateFrom(node=p2(1))
    println(s"results partition2=$resPartition2")

    resPartition1 = getStateFrom(node=p1(0))
    println(s"results partition1=$resPartition1")


    system.scheduler.scheduleOnce(1.minute) {
      system.shutdown()
    }
  }

  def getStateFrom(node: ActorRef): Vector[Int] = {
    Await.result(node ? Get, 5.seconds).asInstanceOf[Vector[Int]]
  }
}
