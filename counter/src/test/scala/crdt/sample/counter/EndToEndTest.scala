package crdt.sample.counter

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import crdt.sample._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * These tests might not work on IntelliJ since they are not meant to be run in parallel.
  */
class EndToEndTest extends TestKit(ActorSystem("EndToEndTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Node actors" must {
    "synchronize their state after the updates" in {
      withMasterActor { master =>
        // Prepare
        master ! GetTopology
        val Vector(nodes) = receiveOne(5.seconds).asInstanceOf[Vector[Vector[ActorRef]]]
        val expectedConvergedVectorValue = (0 until counter.VectorSize map (_ => 1)).toVector

        // Exercise
        for (i <- 0 until counter.VectorSize) {
          nodes foreach { actor => actor ! Inc(i) }
        }

        // Verify
        assertStateIs(expectedState = expectedConvergedVectorValue, actors = nodes)
      }
    }

    "notice divergent values during a partition" in {
      withMasterActor { master =>
        master ! GetTopology
        val Vector(initialTopology) = receiveOne(5.seconds).asInstanceOf[Vector[Vector[ActorRef]]]

        // Exercise
        val (partition1, partition2) = initialTopology.splitAt(2)
        master ! SetTopology(Vector(partition1, partition2))
        expectNoMsg(1.second)

        partition1(0) ! Inc(1) // This update should be experienced only in partition 1

        // Verify
        assertStateIs(expectedState = Vector(0, 1, 0), partition1)
        assertStateIs(expectedState = Vector(0, 0, 0), partition2)
      }
    }

  }

  def withMasterActor(body: ActorRef => Unit): Unit = {
    val actor = system.actorOf(Props(classOf[Master], counter.VectorSize))
    try {
      body(actor)
    } finally {
      system.stop(actor)
    }
  }

  def assertStateIs(expectedState: Seq[Int], actors: Seq[ActorRef]): Unit = {
    for (actor <- actors) {
      awaitAssert({
        actor ! Get
        expectMsg(1.second, expectedState)
      }, max = 5.seconds, interval = 100.millis)
    }
  }

}
