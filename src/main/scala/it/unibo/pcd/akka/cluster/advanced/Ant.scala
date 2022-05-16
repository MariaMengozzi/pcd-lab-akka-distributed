package it.unibo.pcd.akka.cluster.advanced

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.akka.Message

import scala.concurrent.duration.FiniteDuration
import scala.util.Random

/** A simple actor that randomly moves around an empty environment. For each move, it send a message to a sequence of
  * frontend interested in ant movements.
  */
object Ant:
  /*sealed -> in un altro file non posso estendere Command, equivale a
  * Enum Command:
      case Stop, Move

  Sealed provides exhaustive checking for our application. Exhaustive checking allows to check that all members of a
  * sealed trait must be declared in the same file as of the source file. That means that all the possible known members
  * of a trait that must be included are known by the compiler in advance
  * */
  sealed trait Command extends Message // Enum needs an ad-hoc serializers...
  case object Stop extends Command // api pubblica
  private case object Move extends Command // and ADT enable also private messages //comporamento interno che non mostro all'esterno. è un'interfaccia privata

  def apply(
      position: (Int, Int),
      period: FiniteDuration,
      frontends: List[ActorRef[AntsRender.Render]] = List.empty
  )(using random: Random): Behavior[Command | Receptionist.Listing] =
    Behaviors.setup[Command | Receptionist.Listing] { ctx =>
      /*dico che voglio registrarmi a tutti quelli che sono registrati come service*/
      ctx.system.receptionist ! Receptionist.Subscribe(AntsRender.Service, ctx.self) // register to new frontend
      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(Move, period)
        antLogic(position, ctx, frontends)
      }
    }

  // Main logic, each period the ant changes its position (using the random generator in the context)
  def antLogic(
      position: (Int, Int),
      ctx: ActorContext[Command | Receptionist.Listing],
      frontends: List[ActorRef[AntsRender.Render]] = List.empty
  )(using random: Random): Behavior[Command | Receptionist.Listing] = Behaviors.receiveMessage {
    case msg: Receptionist.Listing =>
      //contiene tutti i servizi che si sono registrati
      ctx.log.info(s"New frontend! $msg")
      val services = msg.serviceInstances(AntsRender.Service).toList
      if (services == frontends)
        Behaviors.same
      else
        antLogic(position, ctx, msg.serviceInstances(AntsRender.Service).toList)
    case Move =>
      val (deltaX, deltaY) = ((random.nextGaussian * 5).toInt, (random.nextGaussian * 5).toInt)
      val (x, y) = position
      frontends.foreach(_ ! AntsRender.Render(x, y, ctx.self))
      ctx.log.info(s"move from ${(x, y)}, ${ctx.self.path}")
      antLogic((x + deltaX, y + deltaY), ctx, frontends)
    case Stop => Behaviors.stopped
  }
