import akka.actor._
import scala.concurrent.duration._

object Subscriber {
  def props(broker: String): Props = Props(new Subscriber(broker))
}

class Subscriber(broker: String) extends Actor with ActorLogging {

  private val brokerRef = context.actorSelection(broker)
  private var bench = Map[Consume, Long]()

  //Send a single boot message to the current actor to ask the buffer for registration.
  context.system.scheduler
    .scheduleOnce(500.millisecond, self, "boot")(context.dispatcher)

  override def receive: Receive = {
    case "boot" => brokerRef ! Subscribe(self)

    case Pull(value) =>   //Get the value from the buffer
      val v = value.toString.toInt
      brokerRef ! Consume(v)
      bench += Consume(v) -> System.nanoTime()

    case Notify(value) => //Receiving the notification from the buffer
      self ! Pull(value)  //Sends to itself a message to pull the value

    case ConsumeAck(consume: Consume, value: Int) =>
      val s1 = s"${(System.nanoTime - bench(consume)) / 1e6}ms"
      val s2 = s"Retirado o valor $value no Buffer pelo Produtor ${self.path.name} em"
      println(s"$s2 $s1")
      bench -= consume

    case ConsumeNoAck(consume: Consume, value: Int) => //Remove o valor do buffer interno
      //println(s"${self.path.name} tentou remover valor $value inexistente no Buffer.")
      bench -= consume

    case Empty(msg) =>
      println(s"\nConsumidor ${self.path.name} tentou retirar item do Buffer vazio.\n")
  }
}