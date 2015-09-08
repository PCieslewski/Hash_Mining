import akka.actor._
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import java.security.MessageDigest

import scala.collection.mutable

object Main extends App{

  val numWorkers : Int = 8
  val numBlocks : Int = 5000
  val lenBlock : Int = 5000

  sealed trait PiMessage
  case class WorkBlock(start: Int, lenBlock: Int) extends PiMessage
  case class ResultOfBlock(result: Double) extends PiMessage

  println("Hashing pawel123")
  //var test : Array[Byte] = hash256("pawel123")

  println(hash256("pawel123"))

  var sgen = new StringGen("pawel")

  for(i <- 0 to 30) {
    println(sgen.genString())
  }

  val system = ActorSystem("HelloSystem")
  val manActor = system.actorOf(Props(new Manager(numWorkers,numBlocks,lenBlock)), name = "manActor")

  def hash256(in : String): String = {
    val bytes: Array[Byte] = (MessageDigest.getInstance("SHA-256")).digest(in.getBytes("UTF-8"))
    val sep: String = ""
    bytes.map("%02x".format(_)).mkString(sep)
  }

  /*class StringGen(base: String){
    var baseString : String = base
    val chars = ('!' to '~')

    def genString(): String = {
      baseString + randomStringFromCharList(16, chars)
    }

    def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
      val sb = new StringBuilder
      for (i <- 1 to length) {
        val randomNum = util.Random.nextInt(chars.length)
        sb.append(chars(randomNum))
      }
      sb.toString
    }
  }*/

  class StringGen(base: String){
    var baseString : String = base
    //val chars = ('!' to '~')
    val chars = ('a' to 'c')
    var perms:mutable.MutableList[Int] = mutable.MutableList(0)

    def genString(): String = {

      val sb = new StringBuilder("test")
      for (i <- perms.indices) {
        sb.append(chars(perms(i)))
      }
      incPerms()
      sb.toString

    }

    def incPerms(){

      //
      for(i <- 0 to perms.length-2) {
        perms(i) = perms(i) + 1
        if (perms(i) < chars.length) {
          return
        }
        else {
          perms(i) = 0
        }
      }

      //HANDLE LAST NUM
      perms(perms.length-1) = perms(perms.length-1) + 1
      if (perms.last < chars.length) {
        return
      }
      else {
        perms(perms.length-1) = 0
        perms = perms ++ mutable.MutableList(0)
      }

    }

  }

  //Define Worker Actor
  class Worker extends Actor{

    //Function to convert each index into a fractional part of pi
    def convIndexToFraction(index: Double): Double = {
      if(index%2==0) -4/(index*2-1)
      else 4/(index*2-1)
    }

    //Sum all of the fractional parts of the block into a single value
    def calcBlock(start: Int, lenBlock: Int): Double = {
      //return ((start to start+lenBlock) map {_.toDouble} map { _*2-1 } map { 4/_ }).sum
      //(start to start+lenBlock-1).map(x => convIndexToFraction(x)).sum
      var acc = 0.0
      for (i ← start until (start + lenBlock))
        acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
      acc
    }

    def receive = {
      //case WorkBlock(start, lenBlock) => println(calcBlock(start,lenBlock))
      case WorkBlock(start, lenBlock) => sender ! new ResultOfBlock(calcBlock(start, lenBlock))
    }

  }

  class Manager(numWorkers: Int, numBlocks: Int, lenBlock: Int) extends Actor {

    var currPi: Double = 0
    var numMsgs: Int = 0
    var startTime : Long = System.currentTimeMillis

    val workerRouter = context.actorOf(Props[Worker].withRouter(SmallestMailboxRouter(numWorkers)), name = "workerRouter")

    for (i <- 0 until numBlocks) workerRouter ! WorkBlock(i*lenBlock, lenBlock)

    def receive = {

      case ResultOfBlock(result) => {

        currPi += result
        numMsgs += 1

        if(numMsgs == numBlocks){
          println(currPi)
          println(System.currentTimeMillis-startTime)
          context.stop(self)
          context.system.shutdown()
        }

      }

    }

  }

}
