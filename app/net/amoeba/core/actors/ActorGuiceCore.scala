package net.amoeba.core.actors

import com.google.inject.Provider
import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import com.google.inject.Inject
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.api.Play

@Singleton
class ActorInstance[T <: Actor] @Inject() (
    systemProvider: Provider[ActorSystem], builder: ActorBuilder, provider: Provider[T]) {
    lazy val ref: ActorRef = builder(systemProvider.get, provider)
}

@ImplementedBy(classOf[ActorBuilderImpl])
trait ActorBuilder {
    def apply(system: ActorSystem, provider: Provider[_ <: Actor]): ActorRef
}

class ActorBuilderImpl extends ActorBuilder {
    def apply(system: ActorSystem, provider: Provider[_ <: Actor]): ActorRef = {
        system.actorOf(Props { provider.get })
    }
}

@Singleton
class PlayActorSystemProvider extends Provider[ActorSystem] {
    val actorSystem = Akka.system(Play.current)
    def get = actorSystem
}

@Singleton
class ActorSystemProvider extends Provider[ActorSystem] {
    val actorSystem = akka.actor.ActorSystem("AmoebaActorSystem")
    def get = actorSystem
}


