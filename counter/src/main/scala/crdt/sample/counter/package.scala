package crdt.sample

import akka.actor.ActorRef

package object counter {
  val VectorSize = 5
}

case class Inc(position: Int)
case object Get
case object GetTopology
case class SetTopology(partitions: Vector[Vector[ActorRef]])
case class Merge(vector: Seq[Int])
case class Siblings(siblings: Seq[ActorRef])

