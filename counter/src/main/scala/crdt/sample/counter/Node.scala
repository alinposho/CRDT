package crdt.sample.counter

import akka.actor.{ActorRef, Actor, ActorLogging}
import crdt.sample.{Siblings, Merge, Get, Inc}

class Node extends Actor with ActorLogging {

  val state = new Array[Int](VectorSize)
  var siblings = Seq.empty[ActorRef]

  def receive = {
    case Inc(pos) =>
      state(pos) += 1
      siblings foreach (actor => actor ! Merge(state.toSeq))
      log.info(s"Received Inc($pos). New state=$printState")
    case Get => sender ! state.toVector
    case Merge(seq) =>
      log.info(s"Received merge request with content=$seq")
      for (i <- state.indices) {
        state(i) = Math.max(state(i), seq(i))
      }
      log.info(s"State after merge=$printState")
    case Siblings(newSiblings) =>
      log.info(s"Received new siblings list $newSiblings")
      siblings = newSiblings
      siblings foreach(a => a ! Merge(state.toSeq))
  }

  def printState = state.mkString(",")


}
