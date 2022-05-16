package it.unibo.pcd.akka.cluster.basics

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ClusterEvent.{LeaderChanged, MemberEvent}
import akka.cluster.typed.{Cluster, Subscribe}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd.akka.cluster.*

object ShowEvents:
  def apply(): Behavior[MemberEvent | LeaderChanged] = Behaviors.setup { ctx => //
    val cluster = Cluster(ctx.system) // get the reference of the cluster in which the actor belongs
    // receive all the event of the cluster in which the actor belongs, (via pub sub pattern)
    classOf[MemberEvent] :: classOf[LeaderChanged] :: Nil foreach (event => // I can Subscribe to multiple events
      cluster.subscriptions ! Subscribe(ctx.self, event) //attore che da la possibilità di sottoscriversi agli eventi del sistema.
                                                        // l'attore è come se fosse distribuito su tutti i nodi del sistema
    )
    Behaviors.receiveMessage { msg =>
      ctx.log.info(s"EVENT LISTENER: ${msg.toString}")
      Behaviors.same
    }
  }

def startup(port: Int)(root: => Behavior[_]): Unit =
  // Override the configuration of the port
  val config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.load("base-cluster"))

  // Create an Akka system
  ActorSystem(root, "ClusterSystem", config)
@main def multipleActorsSystems(): Unit =
  startup(seeds.head)(ShowEvents())
  startup(seeds.last)(Behaviors.empty)

// To run multiple jvm, use:
// a) open two sbt shells
// b) in one of them, runMain it.unibo.pcd.akka.cluster.basics.singleJVM <a seed port> true
// c) in another console, type: runMain it.unibo.pcd.akka.cluster.basics.singleJVM <another port> false
// d) in the first console, you should see the events
// e) when you are done, type exit
@main def singleJVM(port: Int, listener: Boolean) =
  startup(port)(if (listener) ShowEvents() else Behaviors.empty)
