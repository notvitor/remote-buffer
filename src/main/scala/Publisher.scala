import akka.actor._
import scala.concurrent.duration._

object Publisher {
  def props(broker: String): Props = Props(new Publisher(broker))
}

class Publisher(broker: String) extends Actor with ActorLogging {

  private val brokerRef = context.actorSelection(broker)
  private val random = scala.util.Random
  private var bench = Map[Publish, Long]()

  //Send a random integer every 1 second to the buffer
  private val tick = context.system.scheduler
    .schedule(1.second, 1.second, self, "random")(context.dispatcher)

  //Send a single boot message to the current actor to ask the buffer for registration.
  context.system.scheduler.scheduleOnce(500.millisecond, self, "boot")(context.dispatcher)

  override def postStop() = tick.cancel()

  def receive: Receive = {
    case "boot" =>
      brokerRef ! RegisterPublisher(self)

    case "random" =>
      val v = random.nextInt(100000)
      brokerRef ! Publish(v)
      bench += Publish(v) -> System.nanoTime()

    case PublishAck(msg) =>
      val s1 = s"${(System.nanoTime - bench(msg)) / 1e6}ms"
      val s2 = s"Colocado o valor ${msg.message.toString} no Buffer pelo Produtor ${self.path.name} em"
      println(s"$s2 $s1.")
      bench -= msg

    case Full(msg) =>
      println(s"\nProdutor ${self.path.name} tentou colocar ${msg.message.toString} no Buffer cheio.\n")
  }
}