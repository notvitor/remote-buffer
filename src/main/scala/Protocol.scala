import akka.actor.ActorRef

//Messages - Protocol
//Register the producers that will publish values
case class RegisterPublisher(publisher: ActorRef)
case class AlreadyRegistered(registerPublisher: RegisterPublisher)

//Register the consumers that will consume values
case class Subscribe(subscriber: ActorRef)
case class AlreadySubscribed(subscribe: Subscribe)

//Messages regarding Publisher-Buffer communication
case class Publish(message: Any)
case class PublishAck(publish: Publish)
case class Full(publish: Publish)

//Messages regarding Subscriber-Buffer communication
case class Notify(message: Any)
case class Pull(message: Any)
case class Consume(message: Int)
case class ConsumeAck(consume: Consume, value: Int)
case class ConsumeNoAck(consume: Consume, value: Int)
case class Empty(consume: Consume)

//Messages that could be implemented
//case class Unsubscribe(subscriber: ActorRef)
//case class Unsubscribed(unsubscribe: Unsubscribe)
//case class NotSubscribed(unsubscribe: Unsubscribe)
//case class GetSubscribers(topic: String)