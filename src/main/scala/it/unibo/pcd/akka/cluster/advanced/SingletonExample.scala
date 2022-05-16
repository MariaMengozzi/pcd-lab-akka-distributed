package it.unibo.pcd.akka.cluster.advanced

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.*
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory
import it.unibo.pcd.akka.cluster.seeds

import concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps
import scala.util.Random
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.SingletonActor
import it.unibo.pcd.akka.cluster.*
import AntsRender.*

/** NB! Frontend it is not the best case to use singleton. This is an example for seeing the API & Behaviours of the
  * Singleton actor. Use when:
  *
  * 1) single point of responsibility for certain cluster-wide consistent decisions, or coordination of actions across
  * the cluster system
  *
  * 2) single entry point to an external system
  *
  * 3) single master, many workers
  *
  * 4) centralized naming service, or routing logic
  *
  * Potential problems:
  * https://doc.akka.io/docs/akka/current/typed/cluster-singleton.html#potential-problems-to-be-aware-of
  */
object RootWithSingleton:
  def apply(ants: Int, movePeriod: FiniteDuration = 60 milliseconds): Behavior[Nothing] = Behaviors.setup { ctx =>
    val manager = ClusterSingleton(ctx.system)
    (0 to ants) foreach (_ => {
      given random: Random = Random()
      val (x, y) = (random.nextInt(width), random.nextInt(width))
      ctx.spawnAnonymous(Ant.apply((x, y), movePeriod))
    })
    manager.init(SingletonActor(AntsRender(), "global-gui"))
    Behaviors.empty
  }

object RenderUsingSingleton:
  def apply(x: Int, y: Int): Behavior[Nothing] = Behaviors.setup { ctx =>
    val manager = ClusterSingleton(ctx.system)
    val ref = manager.init(SingletonActor(AntsRender(), "global-gui"))
    ref ! AntsRender.Render(x, y, ctx.self)
    Behaviors.empty
  }

@main def singletonTest(): Unit =
  seeds.foreach(port => startup(port = port)(RootWithSingleton(100)))

@main def usingSingletonDirectly(): Unit =
  startup(port = seeds.head)(RenderUsingSingleton(100, 100))
  startup(port = seeds.last)(RenderUsingSingleton(200, 200))
  //anche se lo faccio 2 volte, vedò solo 1 GUI proprio perchè è un singleton
