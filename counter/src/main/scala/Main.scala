import akka.actor.{ActorRef, Props, ActorSystem}
import akka.pattern.ask
import crdt.sample.{Inc, counter, GetTopology}
import crdt.sample.counter.Master
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object Main {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("mySystem")
    val master = system.actorOf(Props[Master])

    implicit val timeout: akka.util.Timeout = 5.seconds

    val res = master ? GetTopology
    val topology = Await.result(res, 5.seconds).asInstanceOf[Vector[Vector[ActorRef]]]

    for (i <- 0 until counter.VectorSize) {
      topology(0) foreach { actor => actor ! Inc(i) }
    }

    system.scheduler.scheduleOnce(5.seconds) {
      system.shutdown()
    }
  }

}
