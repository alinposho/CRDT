import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

val customConf = ConfigFactory.parseString(
  """
     akka.version = 2.3.14
      akka.actor.deployment {
        /my-service {
          router = round-robin-pool
          nr-of-instances = 3
        }
      }
  """)

//val system = ActorSystem("mySystem")

0 until 5


val a = new Array[Int](5)
a.mkString(",")