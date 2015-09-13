import java.net.{NetworkInterface, InetAddress}

import akka.actor._
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import java.security.MessageDigest
import java.net._

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Main extends App {

  val NUM_WORKERS: Int = 8 //Number of workers each middle man spawns
  val NUM_INIT_MSGS: Int = 100 //Numbers of messages the boss initially sends to the middle man.
  var NUM_ZEROS: Int = 3 //Zeros we are looking for
  val NUM_MSGS_PER_BLOCK: Int = 1000 //Hashes of work per block
  val IP_ADDR = getIP() //Local IP

  sealed trait Msg

  //Connection message to attach a middleman to the main boss.
  case class Connect(numInitMsgs: Int) extends Msg

  //Contains work.
  case class WorkBlock(stringList: List[String], numZeros: Int) extends Msg

  //Work response sent by the workers to the big boss.
  case class WorkResponse(inputStrings: List[String], hashes: List[String], finder: String) extends Msg

  //-----MAIN------------

  //If the argument is an IP, run remote configuration
  //else run the local configuration
  if (args(0).contains(".")) {
    initRemoteSystem()
  }
  else{
    NUM_ZEROS = args(0).toInt
    initLocalSystem()
  }

  //------END MAIN--------

  def initLocalSystem(){
    //Generate custom config file for local system to listen on LAN IP.
    val backup = ConfigFactory.load("application.conf")
    val localConfig = genConfig(getIP())

    //Create the system using the custom config file.
    val bigSystem = ActorSystem("BigSystem", localConfig.withFallback(backup))

    //Create the overall boss of the entire distributed system.
    val bigDaddy = bigSystem.actorOf(Props(new BigDaddy(NUM_ZEROS)), name = "BigDaddy")

    //Create the local branch of workers that connect to the main boss.
    //daddy is the path to the big boss thats passed into a middle man.
    val daddy = bigSystem.actorSelection(bigDaddy.path)
    val middleMan = bigSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  def initRemoteSystem(){

    //Create the remote system with the default configuration.
    val remoteSystem = ActorSystem("RemoteSystem")

    //Create a local branch of workers that connect remotely to the big boss. Daddy is the path to the big boss.
    val daddy = remoteSystem.actorSelection("akka.tcp://BigSystem@"+args(0)+":8009/user/BigDaddy")
    val middleMan = remoteSystem.actorOf(Props(new MiddleMan(NUM_WORKERS, daddy)), name = "MiddleMan")

  }

  //Define Worker Actor
  class Worker() extends Actor{

    def receive = {

      case WorkBlock(stringList: List[String], numZeros: Int) => {

        val zeroString = ("%0" + numZeros.toString + "d").format(0)
        val hashes = new ListBuffer[String]()
        val inputStrings = new ListBuffer[String]()

        for (str <- stringList) {
          val hash = hash256(str)

          if (hash.startsWith(zeroString)) {
            hashes += hash
            inputStrings += str
          }

        }

        sender ! new WorkResponse(inputStrings.toList, hashes.toList, IP_ADDR)

      }

    }

  }

  //Define the Middle Man Actor.
  //This actor spawns off an array of workers on the machine that the middle man is on.
  //The middle man controls all communication between the workers and the main boss.
  class MiddleMan(numWorkers: Int, daddy: ActorSelection) extends Actor {

    val props = Props(classOf[Worker])
    //val props = Props(classOf[Worker], NUM_ZEROS) //EXAMPLE OF HOW TO PASS ARGS TO ACTOR FACTORY
    val workerRouter = context.actorOf(props.withRouter(SmallestMailboxRouter(numWorkers)), name = "workerRouter")

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

  //Actor definition for big daddy
  //BigDaddy is the main boss that sends work, receives completed work, and prints bitcoins on the server machine.
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

  //Defining hashing function
  def hash256(in : String): String = {
    val bytes: Array[Byte] = MessageDigest.getInstance("SHA-256").digest(in.getBytes("UTF-8"))
    val sep: String = ""
    bytes.map("%02x".format(_)).mkString(sep)
  }

  //Define the function that returns the LAN IP address of the current machine.
  //This is used to setup config files and identify machines in the logs.
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

  //This function generates a custom configuration file with the host set for the correct IP address.
  def genConfig(ip: String): Config ={

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

    val b = "\""+ip+"\""

    val c = """
    port = 8009
      }

      log-sent-messages = on
      log-received-messages = on
      }

      }"""

    return ConfigFactory.parseString(a+b+c)

  }

}

