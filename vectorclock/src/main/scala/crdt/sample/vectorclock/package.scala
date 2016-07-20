package crdt.sample

import akka.actor.ActorRef

case object Inc // Each node is restricted to incrementing the values in its index position
case object Get
case object GetTopology
case class SetTopology(partitions: Vector[Vector[ActorRef]])
case class Merge(vector: Seq[Int])
case class Siblings(siblings: Seq[ActorRef])

