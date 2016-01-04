import akka.actor.SupervisorStrategy._
import akka.actor._
import scala.concurrent.duration._

object Supervisor {
  final val name: String = "supervisor"
  def props(children: Int, props: Props): Props = Props(new Supervisor(children, props))
  case class Watch(name: String, props: Props)
  case class SendTo(client: ActorRef, msg: Any)
}

class Supervisor(children: Int, props: Props) extends Actor with ActorLogging {

  import Supervisor._

  private val peers: Map[String, ActorRef] =
    (1 to children)
      .map(i => context.actorOf(props, props.actorClass().getName + i))
      .map(a => a.path.name -> a)(collection.breakOut)

  override def preStart() = {
    log.info("Supervisor is online.")
    peers.foreach {
      case (_, a: ActorRef) => context.watch(a)
    }
  }

  override def postStop() = log.info("Supervisor is offline.")

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 60, withinTimeRange = 10.seconds) {
      case _: ActorInitializationException => Stop
      case _: ActorKilledException => Restart
      case _: Exception => Restart
    }
  }

  def receive = {
    case Watch(name: String, props: Props) =>
      sender() ! context.actorOf(props, name)

    case SendTo(client, msg) if peers.contains(client.path.name) =>
      client forward msg

    case msg: Any =>
      println(s"[Supervisor:Receive]: DUMP: $msg")
  }
}