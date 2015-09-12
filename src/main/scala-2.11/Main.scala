import java.net.{NetworkInterface, InetAddress}

import akka.actor._
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import java.security.MessageDigest
import java.net._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends App{

  val isLocal = true

  val NUM_WORKERS : Int = 8
  val NUM_INIT_MSGS : Int = 100
  val NUM_ZEROS : Int = 3
  val NUM_MSGS_PER_BLOCK : Int = 1000

  sealed trait Msg
  case class Connect(numInitMsgs: Int) extends Msg
  case class WorkBlock(stringList: List[String]) extends Msg
  case class WorkResponse(inputStrings: List[String], hashes: List[String], finder: String) extends Msg

  val nets = NetworkInterface.getNetworkInterfaces

  while(nets.hasMoreElements){
    var temp = nets.nextElement().getInetAddresses
    while(temp.hasMoreElements){
      println(temp.nextElement().getHostAddress)
    }
  }

  //val temp = nets.nextElement().getInetAddresses

  //while(temp.hasMoreElements){
  //  println(temp.nextElement().getHostAddress)
  //}

  if(isLocal){
    //initLocalSystem()
  }
  else{
    //initRemoteSystem()
  }

  //val bigSystem = ActorSystem("BigSystem")
  //val bigSystem = ActorSystem("BigSystem")
  //val bigDaddy = bigSystem.actorOf(Props(BigDaddy), name = "BigDaddy")

  //val workerSystem = ActorSystem("WorkerSystem")
  //val localMiddleMan = bigSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, isLocal)), name = "LocalMiddleMan")

  def initLocalSystem(){
    val bigSystem = ActorSystem("BigSystem")
    val bigDaddy = bigSystem.actorOf(Props(new BigDaddy()), name = "BigDaddy")

    val daddy = bigSystem.actorSelection(bigDaddy.path)

    val middleMan = bigSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  def initRemoteSystem(){
    val remoteSystem = ActorSystem("RemoteSystem")
    val daddy = remoteSystem.actorSelection("akka.tcp://BigSystem@192.168.1.245:8009/user/BigDaddy")

    val middleMan = remoteSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  //Defining hashing function
  def hash256(in : String): String = {
    val bytes: Array[Byte] = MessageDigest.getInstance("SHA-256").digest(in.getBytes("UTF-8"))
    val sep: String = ""
    bytes.map("%02x".format(_)).mkString(sep)
  }

  //Define Worker Actor
  class Worker(numZeros: Int) extends Actor{

    //Create a zero string of length numZeros
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

        sender ! new WorkResponse(inputStrings.toList, hashes.toList, "PAWEL")

    }

  }

  //Define Manager Actor
  class MiddleMan(numWorkers: Int, daddy: ActorSelection) extends Actor {

    val props = Props(classOf[Worker], NUM_ZEROS) //NOT PASSED IN CONSTANT
    val workerRouter = context.actorOf(props.withRouter(SmallestMailboxRouter(numWorkers)), name = "workerRouter")
    //var daddy : ActorSelection = context.actorSelection(daddyPath)

    daddy ! new Connect(NUM_INIT_MSGS)

    def receive = {

      case wb: WorkBlock => {
        workerRouter ! wb
      }

      case wr: WorkResponse => {
        daddy ! wr
      }

    }

  }

  class BigDaddy() extends Actor {

    val sgen = new StringGen("pawel")

    def receive = {
      case Connect(numInitMsgs: Int) => {

        println("New Connection! "+sender.path.toString)

        for(i <- 0 to numInitMsgs){
          sender ! new WorkBlock(sgen.genStringBlock(NUM_MSGS_PER_BLOCK))
        }

      }

      case WorkResponse(inputStrings: List[String], hashes: List[String], finder: String) => {

        sender ! new WorkBlock(sgen.genStringBlock(NUM_MSGS_PER_BLOCK))

        for (i <- inputStrings.indices) {
          println(finder + " found a hash! Hash: " + inputStrings(i) + " is " + hashes(i))
        }

      }

    }

  }

}