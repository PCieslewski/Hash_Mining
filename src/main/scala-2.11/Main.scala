import java.net.{NetworkInterface, InetAddress}

import akka.actor._
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import java.security.MessageDigest
import java.net._

import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends App {

  var isLocal = true

  val NUM_WORKERS: Int = 8
  val NUM_INIT_MSGS: Int = 100
  var NUM_ZEROS: Int = 3
  val NUM_MSGS_PER_BLOCK: Int = 1000
  val IP_ADDR = getIP()

  sealed trait Msg
  case class Connect(numInitMsgs: Int) extends Msg
  case class WorkBlock(stringList: List[String], numZeros: Int) extends Msg
  case class WorkResponse(inputStrings: List[String], hashes: List[String], finder: String) extends Msg

  if (args(0).contains(".")) {
    isLocal = false
    initRemoteSystem()
  }
  else{
    isLocal = true
    NUM_ZEROS = args(0).toInt
    initLocalSystem()
  }

//  if(isLocal){
//    initLocalSystem()
//  }
//  else{
//    initRemoteSystem()
//  }

  def getIP(): String = {
    val nets = NetworkInterface.getNetworkInterfaces
    while(nets.hasMoreElements){
      val temp = nets.nextElement().getInetAddresses
      while(temp.hasMoreElements){
        val s = temp.nextElement()
        if(s.isInstanceOf[Inet4Address]) {
          return s.getHostAddress
        }
      }
    }
    return "No Network??"
  }

  def initLocalSystem(){
    val backup = ConfigFactory.load("application.conf")

    val a = """
    akka {
      loglevel = "INFO"

      actor {
        provider = "akka.remote.RemoteActorRefProvider"
      }

      remote {
        enabled-transports = ["akka.remote.netty.tcp"]
        netty.tcp {
          hostname = """

    val b = "\""+getIP()+"\""

    val c = """
    port = 8009
      }

      log-sent-messages = on
      log-received-messages = on
      }

      }"""

    val testConf = ConfigFactory.parseString(a+b+c)



    //println(testConf.toString())

    val bigSystem = ActorSystem("BigSystem", testConf.withFallback(backup))
    //val bigSystem = ActorSystem("BigSystem")
    val bigDaddy = bigSystem.actorOf(Props(new BigDaddy(NUM_ZEROS)), name = "BigDaddy")

    //val myConfig = ConfigFactory.load("application.conf")
    //val backup = ConfigFactory.parseString("akka.remote.netty.hostname = "+getIP())   // same tree structure as config file where hostname value usually goes
    //val system = ActorSystem("BigSystem", backup.withFallback(myConfig))

    val daddy = bigSystem.actorSelection(bigDaddy.path)

    val middleMan = bigSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  def initRemoteSystem(){
    val remoteSystem = ActorSystem("RemoteSystem")
    val daddy = remoteSystem.actorSelection("akka.tcp://BigSystem@"+args(0)+":8009/user/BigDaddy")

    //val myConfig = ConfigFactory.load("application.conf")
    //val backup = ConfigFactory.parseString("akka.remote.netty.hostname = "+getIP())   // same tree structure as config file where hostname value usually goes
    //val system = ActorSystem("RemoteSystem", backup.withFallback(myConfig))

    val middleMan = remoteSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  //Defining hashing function
  def hash256(in : String): String = {
    val bytes: Array[Byte] = MessageDigest.getInstance("SHA-256").digest(in.getBytes("UTF-8"))
    val sep: String = ""
    bytes.map("%02x".format(_)).mkString(sep)
  }

  //Define Worker Actor
  class Worker() extends Actor{

    //Create a zero string of length numZeros

    def receive = {

      case WorkBlock(stringList: List[String], numZeros: Int) =>

        val zeroString = ("%0"+numZeros.toString+"d").format(0)

        val hashes = new ListBuffer[String]()
        val inputStrings = new ListBuffer[String]()

        for(str <- stringList){
          val hash = hash256(str)

          if(hash.startsWith(zeroString)) {
            hashes += hash
            inputStrings += str
          }

        }

        sender ! new WorkResponse(inputStrings.toList, hashes.toList, IP_ADDR)

    }

  }

  //Define Manager Actor
  class MiddleMan(numWorkers: Int, daddy: ActorSelection) extends Actor {

    val props = Props(classOf[Worker])
    //val props = Props(classOf[Worker], NUM_ZEROS) //EXAMPLE OF HOW TO PASS ARGS TO ACTOR FACTORY
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

  class BigDaddy(numZeros: Int) extends Actor {

    val sgen = new StringGen("pawel")

    def receive = {
      case Connect(numInitMsgs: Int) => {

        println("New Connection! "+sender.path.toString)

        for(i <- 0 to numInitMsgs){
          sender ! new WorkBlock(sgen.genStringBlock(NUM_MSGS_PER_BLOCK), numZeros)
        }

      }

      case WorkResponse(inputStrings: List[String], hashes: List[String], finder: String) => {

        sender ! new WorkBlock(sgen.genStringBlock(NUM_MSGS_PER_BLOCK), numZeros)

        for (i <- inputStrings.indices) {
          println(finder + " found a hash! Hash: " + inputStrings(i) + " is " + hashes(i))
        }

      }

    }

  }

}