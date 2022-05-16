package it.unibo.pcd.akka.cluster.basics

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.ClusterEvent.{LeaderChanged, MemberEvent}
import akka.cluster.typed.{Cluster, Join, Subscribe, Leave}
import com.typesafe.config.ConfigFactory
import akka.actor.AddressFromURIString
import it.unibo.pcd.akka.cluster.*
/** programmatic joining using another cluster ref */
@main def join(): Unit =
  val first = startup("base-cluster-no-seed", 3521)(Behaviors.empty)
  val clusterRefA = Cluster(first) //decoro i sistemi con le API del cluster
  val second = startup("base-cluster-no-seed", 3522)(Behaviors.empty)
  val clusterRefB = Cluster(second)
  clusterRefA.manager ! Join(clusterRefA.selfMember.address) //mando al cluster il msg dove gli dico che voglio partecipare
  Thread.sleep(5000)
  clusterRefB.manager ! Join(clusterRefA.selfMember.address) // same cluster!!
  Thread.sleep(5000)
  clusterRefA.manager ! Leave(clusterRefB.selfMember.address) //il nodo esce dal cluster  
  Thread.sleep(5000)
  println(clusterRefA.state) // I can read the cluster state

@main def withSeed(): Unit =
  seeds.foreach(port => startup(port = port)(Behaviors.empty))

// Use two (or multiple) sbt shells
// choose one seed node (with the port)
// connect to that cluster
// command: runMain it.unibo.pcd.akka.cluster.basics.usingRemote myPort seedPort (one command should have myPort == seedPort)
@main def usingRemote(myPort: Int, seedPort: Int): Unit =
  val system = startup("base-cluster-no-seed", myPort)(Behaviors.empty)
  val seed = AddressFromURIString.parse(s"akka://ClusterSystem@127.0.0.1:$seedPort")
  Cluster(system).manager ! Join(seed)
