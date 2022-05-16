package it.unibo.pcd.akka.cluster.routers

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.ClusterEvent.{LeaderChanged, MemberEvent}
import akka.cluster.typed.{Cluster, Subscribe}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd.akka.Message
import it.unibo.pcd.akka.cluster.*

object Worker:
  sealed trait Command
  case class EvalFactorial(n: Int, resultSendTo: ActorRef[Result]) extends Message with Command
  case class Result(number: Int, result: Long) extends Message
  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage { case EvalFactorial(n, resultSendTo) =>
      ctx.log.info(s"Eval factorial of $n, I am ${ctx.self.path}")
      resultSendTo ! Result(n, factorial(n))
      Behaviors.same
    }
  }

  private def factorial(n: Int): Long = if (n == 0)
    1
  else
    n * factorial(n - 1)
