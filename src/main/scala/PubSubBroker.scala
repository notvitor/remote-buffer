import akka.actor._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

object PubSubBroker {
  def props(maxCapacity: Int): Props = Props(new PubSubBroker(maxCapacity))
}

class PubSubBroker(maxCapacity: Int) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info(s"Broker started at $self")
  private var queue: ArrayBuffer[Int] = ArrayBuffer[Int](maxCapacity)
  private var subscribers: Map[String, ActorRef] = Map[String, ActorRef]()
  private var publishers: Map[String, ActorRef] = Map[String, ActorRef]()

  //Stores the amount of 'incoming/outgoing' messages
  var sent = 0L
  var received = 0L

  //Every 3 seconds it shows the current 'status' of the broker.
  //The current buffer size, how many messages it received/sent,
  //the amount of publishers and subscribers.
  var showCount: Cancellable = context.system.scheduler
    .schedule(3.second, 3.second, self, "showCount")(context.dispatcher)

  override def receive = {
    case register@RegisterPublisher(actor: ActorRef)
      if publishers.contains(actor.path.name) =>
      sender() ! AlreadyRegistered(register)

    case RegisterPublisher(actor: ActorRef) =>    //Adds a new producer
      publishers += actor.path.name -> sender()

    case subscribe@Subscribe(actor: ActorRef)
      if subscribers.contains(actor.path.name) =>
      sender() ! AlreadySubscribed(subscribe)

    case Subscribe(actor: ActorRef) =>            //Adds a new subscriber
      subscribers += actor.path.name -> sender()

    case publish@Publish(message) if queue.size >= maxCapacity =>
      received += 1
      sender() ! Full(publish)                    //Reply alerting about the buffer size.
      queue.clear()

    case publish@Publish(msg: Any) =>             //Adds a new value to the queue
      received += 1
      queue += msg.toString.toInt
      sender() ! PublishAck(publish)
      subscribers.foreach(_._2 ! Notify(msg))     //Notify the arrival to every subscriber.

    case consume@Consume(msg: Any) if queue.isEmpty =>
      sender() ! Empty(consume)
      sent += 1

    case consume@Consume(value: Any) if queue.contains(value.toInt) =>
      synchronized {
        sender() ! ConsumeAck(consume, value)     //Sends an 'Ack' message confirming retrieval
        queue -= value.toInt                      //Naive impl., removing value from the buffer.
        sent += 1
      }
    case consume@Consume(value: Any) if !queue.contains(value.toInt) =>
      sender() ! ConsumeNoAck(consume, value)     //Sends NoAck message as the value was already taken.
      sent += 1

    case "showCount" =>
      val s1 = s"| Messages (Received/Sent): $received/$sent"
      val s2 = s"| Publishers: ${publishers.size} | Subscribers: ${subscribers.size}"
      println(s"\nBuffer Size: ${queue.size} $s1 $s2\n")
  }
}