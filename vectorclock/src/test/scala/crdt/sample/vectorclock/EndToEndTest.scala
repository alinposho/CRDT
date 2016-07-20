package crdt.sample.vectorclock

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import crdt.sample._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class EndToEndTest extends TestKit(ActorSystem("EndToEndTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  val VectorSize = 3

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Node actors" must {
    "synchronize their state after the updates" in {
      withMasterActor { master =>
        // Prepare
        val Vector(nodes) = getTopology(master)
        val expectedConvergedVectorValue = (0 until VectorSize map (_ => 1)).toVector

        // Exercise
        for (i <- 0 until VectorSize) {
          nodes(i) ! Inc
        }

        // Verify
        assertStateIs(expectedState = expectedConvergedVectorValue, actors = nodes)
      }
    }

    "notice divergent values during a partition" in {
      withMasterActor { master =>

        // Exercise
        val (partition1, partition2) = partitionTopologyAt(index = 2, master)

        partition1(1) ! Inc // This update should be experienced only in partition 1

        // Verify
        assertStateIs(expectedState = Vector(0, 1, 0), partition1)
        assertStateIs(expectedState = Vector(0, 0, 0), partition2)
      }
    }

    "merge the state after the partion is healed" in {
      withMasterActor { master =>
        // Prepare
        val initialTopology = getTopology(master)
        val (partition1, partition2) = partitionTopologyAt(index = 2, master)

        // Exercise

        partition1(0) ! Inc // This update should be experienced only in partition 1
        partition1(1) ! Inc
        partition2(0) ! Inc
        partition2(0) ! Inc // This update should be experienced by the second partition

        // Verify states have diverged
        assertStateIs(expectedState = Vector(1, 1, 0), partition1)
        assertStateIs(expectedState = Vector(0, 0, 2), partition2)

        // heal network
        master ! SetTopology(initialTopology)
        val newTopology = getTopology(master)
        assert(initialTopology === newTopology)


        val mergeState = Vector(1, 1, 2) // Notice that for the last element we picked the max, i.e. 2
        assertStateIs(expectedState = mergeState, partition1)
        assertStateIs(expectedState = mergeState, partition2)
      }
    }

  }

  def setInitialState(actors: Vector[ActorRef]): Vector[Int] = {
    for (i <- 0 until VectorSize) {
      actors(i) ! Inc
    }
    val initialState: Vector[Int] = Vector(1, 1, 1)
    assertStateIs(expectedState = initialState, actors = actors)

    initialState
  }

  def withMasterActor(body: ActorRef => Unit): Unit = {
    val actor = system.actorOf(Props(classOf[Master], VectorSize))
    try {
      body(actor)
    } finally {
      system.stop(actor)
    }
  }

  def getTopology(master: ActorRef): Vector[Vector[ActorRef]] = {
    master ! GetTopology
    val topology = receiveOne(5.seconds).asInstanceOf[Vector[Vector[ActorRef]]]
    topology
  }

  def partitionTopologyAt(index: Int, master: ActorRef): (Vector[ActorRef], Vector[ActorRef]) = {
    val Vector(initialTopology) = getTopology(master)
    val (partition1, partition2) = initialTopology.splitAt(2)
    master ! SetTopology(Vector(partition1, partition2))
    assert(getTopology(master) === Vector(partition1, partition2))

    (partition1, partition2)
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
