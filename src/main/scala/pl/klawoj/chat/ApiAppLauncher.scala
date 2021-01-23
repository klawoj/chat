package pl.klawoj.chat

import akka.actor.ActorSystem
import akka.management.cluster.bootstrap.ClusterBootstrap

trait ApiAppLauncherLike {
  def launch(implicit system: ActorSystem): Unit = {
    ClusterBootstrap(system).start()
    TopSupervisor.actor
  }
}

object ApiAppLauncher extends App with ApiAppLauncherLike {
  launch(ActorSystem("chat"))
}
