package crdt.sample.vectorclock

import akka.actor.{Actor, ActorLogging, ActorRef}
import crdt.sample.{Get, Inc, Merge, Siblings}

/** The index is 0 based. */
class Node(val index: Int, val vectorSize: Int) extends Actor with ActorLogging {

  var state: Seq[Int] = (0 until vectorSize) map (_ => 0)
  var siblings = Seq.empty[ActorRef]

  def receive = {
    case Inc =>
      state = state.updated(index, state(index) + 1)
      siblings foreach (actor => actor ! Merge(state))
      log.info(s"Actor=$index received Inc. New state=$printState")
    case Get => sender ! state.toVector
    case Merge(seq) =>
      log.info(s"Received merge request with content=$seq")
      state = for ((s, news) <- state.zip(seq)) yield Math.max(s, news)
      log.info(s"State after merge=$printState")
    case Siblings(newSiblings) =>
      log.info(s"Received new siblings list $newSiblings")
      siblings = newSiblings
      siblings foreach (a => a ! Merge(state))
  }

  def printState = state.mkString("(", ",", ")")


}
