package crdt.sample

import akka.actor.ActorRef

package object vectorclock {
  val VectorSize = 3
}

case object Inc // Each node is restricted to incrementing the values in its index position
case object Get
case object GetTopology
case class SetTopology(partitions: Vector[Vector[ActorRef]])
case class Merge(vector: Seq[Int])
case class Siblings(siblings: Seq[ActorRef])

