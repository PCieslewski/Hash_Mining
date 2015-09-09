import akka.actor._
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import java.security.MessageDigest

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends App{

  val numWorkers : Int = 8

  sealed trait Msg
  case class WorkBlock(stringList: List[String]) extends Msg
  case class WorkResponse(inputStrings: List[String], hashes: List[String]) extends Msg

  //var sgen = new StringGen("pawel")

  val system = ActorSystem("HelloSystem")
  val manActor = system.actorOf(Props(new Manager(numWorkers)), name = "manActor")
  //val manActor = system.actorOf(Props(new Manager(numWorkers,numBlocks,lenBlock)), name = "manActor")

  //val system = ActorSystem("HelloSystem")
  //val worker = system.actorOf(Props(new Worker(1)), name = "worker")
  //worker ! new WorkBlock(sgen.genStringBlock(50))

  def hash256(in : String): String = {
    val bytes: Array[Byte] = (MessageDigest.getInstance("SHA-256")).digest(in.getBytes("UTF-8"))
    val sep: String = ""
    bytes.map("%02x".format(_)).mkString(sep)
  }

  //Define Worker Actor
  class Worker(numZeros: Int) extends Actor{

    val zeroString = ("%0"+numZeros.toString+"d").format(0)

    def receive = {
      case WorkBlock(stringList: List[String]) =>

        val hashes = new ListBuffer[String]()
        val inputStrings = new ListBuffer[String]()

        for(str <- stringList){
          val hash = hash256(str)

          if(hash.startsWith(zeroString)) {
            hashes += hash
            inputStrings += str
          }

        }

        sender ! new WorkResponse(inputStrings.toList, hashes.toList)

    }

  }

  class Manager(numWorkers: Int) extends Actor {

    val props = Props(classOf[Worker], 5)
    val workerRouter = context.actorOf(props.withRouter(SmallestMailboxRouter(numWorkers)), name = "workerRouter")
    val sgen = new StringGen("pawel")

    for(i <- 0 to 100){
      workerRouter ! new WorkBlock(sgen.genStringBlock(500))
    }

    def receive = {
      case WorkResponse(inputStrings: List[String], hashes: List[String]) =>

        workerRouter ! new WorkBlock(sgen.genStringBlock(500))

        for(i <- inputStrings.indices){
          println("FOUND! Hash " + inputStrings(i) + " is " + hashes(i))
        }

    }

  }

}

/*for(i <- 0 to 400) {
    println(sgen.genString())
  }*/