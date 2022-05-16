package it.unibo.pcd.akka.artery

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{Behavior, ActorRef, ActorSystem}
import akka.actor.ActorSystem as ClassicActorSystem // Used for actorSelection
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import it.unibo.pcd.akka.Message
import it.unibo.pcd.akka.artery.GreetApp.*

import concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import scala.concurrent.ExecutionContext.Implicits.global

import scala.language.postfixOps

// NB! Do not use akka artery, it is a low level library (indeed it does not support typed)
object GreetApp:
  //come in java anche qui abbiamo bisogno che gli oggetti siano serializzati.
  // questo perchè AKKA ha disabilitato la serializzazione di default di java, quindi se non la mettiamo ci viene generato un errore
  case class Greet(whom: String, replyTo: ActorRef[Greeted]) extends Message // Required for serialization
  case class Greeted(whom: String, sender: ActorRef[Greet]) extends Message

  def greet(me: String): Behavior[Greet] = Behaviors.receive { case (ctx, Greet(whom, replyTo)) =>
    ctx.log.info(s"$whom at ${replyTo.path} greet to me!")
    replyTo ! Greeted(me, ctx.self)
    Behaviors.same
  }

  def greeted(): Behavior[Greeted] = Behaviors.receive { case (ctx, Greeted(whom, sender)) =>
    ctx.log.info(s"$whom at ${sender.path} has received my greet!")
    Behaviors.stopped
  }

def configFrom(port: Int): Config = ConfigFactory
  .parseString(s"""akka.remote.artery.canonical.port=$port""")
  .withFallback(ConfigFactory.load("base-remote"))

@main def alice(): Unit =
  val config = configFrom(8080)
  ActorSystem(greet("alice"), "foo", config)

@main def gianluca(): Unit =
  anyGreet("gianluca", 8081)

@main def anyGreet(who: String, port: Int): Unit =
  given Timeout = 2 seconds // required for actorSelection.
  val remoteReferencePath = "akka://foo@127.0.0.1:8080/user/"
  val config = configFrom(port)
  val system = ClassicActorSystem.apply("foo", config) //fino a qui creo il mio sisteama in locale
  val remoteReference = system.actorSelection(remoteReferencePath).resolveOne() //dato un URL mi da il suo riferimento. il metodo è asincrono, quindi restituisce una future
  //risolvo la future
  for remote <- remoteReference do
    val actor = system.spawn(greeted(), who) //prendo il riferimento all'altro attore
    remote ! GreetApp.Greet(who, actor) //invio il messaggio all'attore remoto

  //rispetto al caso concentrato, cambia solo che dobbiamo dire dove sono gli attori.
