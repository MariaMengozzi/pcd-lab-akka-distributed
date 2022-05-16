package it.unibo.pcd.akka.cluster.routers

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.ClusterEvent.{LeaderChanged, MemberEvent, MemberUp}
import akka.cluster.typed.{Cluster, Subscribe}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd.akka.Message
import it.unibo.pcd.akka.cluster.*
import it.unibo.pcd.akka.cluster.routers.SendRequests.Tick

import concurrent.duration.DurationInt
import scala.language.postfixOps

object WorkerRouter:
  val workerService = ServiceKey[Worker.Command]("worker")
  def apply(workers: Int): Behavior[Unit] = Behaviors.setup { ctx =>
    1 to workers map { i => ctx.spawn(Worker(), i.toString) } foreach { ref =>
      ctx.system.receptionist ! Receptionist.Register(workerService, ref)
    } //ogni attore deve essere registrato, questo è il meccanismo usato per creare i routre
    Behaviors.empty
  }

object SendRequests:
  case object Tick extends Message
  def apply(): Behavior[Tick.type | Worker.Result] = Behaviors.withTimers { timers =>
    Behaviors.setup { ctx =>
      val router = Routers.group(WorkerRouter.workerService)
      val ref = ctx.spawn(router, "worker-router")
      timers.startSingleTimer(Tick, 5000 milli)
      Behaviors.receiveMessage {
        case Tick =>
          (10 to 20) foreach {
            ref ! Worker.EvalFactorial(_, ctx.self)
          }
          Behaviors.same
        case Worker.Result(request, result) =>
          ctx.log.info(s"Done!! $request ! = $result")
          Behaviors.same
      }
    }
  }
@main def groupRouter(): Unit =
  seeds.foreach(port => startup(port = port)(WorkerRouter(10)))

@main def computeFactorial(): Unit =
  val master = startup(port = 8080)(SendRequests())
