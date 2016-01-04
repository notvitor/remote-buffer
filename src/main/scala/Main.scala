import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
*http://www.enterpriseintegrationpatterns.com/patterns/messaging/ObserverJmsExample.html
*(1)( )The push model requires a single, one-way communication
*(2)(X)The pull model requires three one-way communications

* To examplify a Pub/Sub(Observer Pattern) as requested, the following solution shows
* ..."the pull model,where the subject sends basic notification and each observer requests the
* new state from the subject. Thus each observer can request the exact details it wants,
* even none at all, but the subject often has to serve multiple requests for the same data."
*
* Publisher -v-> Buffer [Notify]-v-> Subscriber [Pull]-v-> Subscriber [Consume]-v-> Buffer [Ack|NoAck]-v-> Subscriber(v)
*/
object Main extends App {
  val buffer = RemoteQueue.start("127.0.0.1", 8000, maxQueueCapacity = 10)
  RemoteQueue.startPublishers("127.0.0.1", 8001, number = 3, buffer)
  RemoteQueue.startSubscribers("127.0.0.1", 8002, number = 3, buffer)
}

object RemoteQueue {

  //Loads the application.conf under the /resources folder
  lazy private val configBuffer = ConfigFactory.load()

  final val hostPath = "akka.remote.netty.tcp.hostname"
  final val portPath = "akka.remote.netty.tcp.port"
  private def online(ip: String, port: Int, name: String, config: Config): ActorSystem = {
    val settings = config
      .withValue(hostPath, ConfigValueFactory.fromAnyRef(ip))
      .withValue(portPath, ConfigValueFactory.fromAnyRef(port))
      .withFallback(config)

    implicit val actorSystem = ActorSystem(name, settings)
    actorSystem
  }

  // Starts a bounded Buffer/Queue with the given maximum capacity at the hostname and IP address specified.
  def start(ip: String = "127.0.0.1", port: Int = 0, maxQueueCapacity: Int): String = {
    val system = online(ip, port, "remote-queue", configBuffer)
    val buffer = system.actorOf(Props(new PubSubBroker(maxQueueCapacity)), name = "broker")

    val bufferIP = system.settings.config.getString(hostPath)
    val bufferPort = system.settings.config.getString(portPath)
    s"akka.tcp://${system.name}@$bufferIP:$bufferPort/user/${buffer.path.name}"
  }

  // Starts n number of 'Producer' actors at given hostname and port targeting the specified buffer.
  // All Producers actors will be placed under a a fault-tolerant mechanism
  // which has its behavior specified in the Supervisor.scala
  def startPublishers(ip: String = "127.0.0.1", port: Int = 0, number: Int, buffer: String): ActorRef = {
    implicit val system = online(ip, port, "publishers", configBuffer)
    spawnPublishers(number, buffer)
  }

  // Starts n number of 'Consumer' actors at given hostname and port targeting the specified buffer.
  // All Consumer actors will be placed under a a fault-tolerant mechanism
  // which has its behavior specified in the Supervisor.scala
  def startSubscribers(ip: String = "127.0.0.1", port: Int = 0, number: Int, buffer: String): ActorRef = {
    implicit val system = online(ip, port, "subscribers", configBuffer)
    spawnSubscribers(number, buffer)
  }

  private def spawnPublishers(amount: Int, buffer: String)(implicit actorSystem: ActorSystem): ActorRef = {
    val props = Props(new Supervisor(amount, Props(new Publisher(buffer))))
    actorSystem.actorOf(props, "publishers")
  }

  private def spawnSubscribers(amount: Int, buffer: String)(implicit actorSystem: ActorSystem): ActorRef = {
    val props = Props(new Supervisor(amount, Props(new Subscriber(buffer))))
    actorSystem.actorOf(props, "subscribers")
  }
}