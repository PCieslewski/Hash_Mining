import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class StringGen(base: String){
  val chars = ('!' to '~')
  var perms:mutable.MutableList[Int] = mutable.MutableList(0)

  def genString(): String = {

    val sb = new StringBuilder(base)
    for (i <- perms.indices) {
      sb.append(chars(perms(i)))
    }
    incPerms()
    sb.toString

  }

  def genStringBlock(len: Int): List[String] = {

    val list = new ListBuffer[String]()

    for (i <- 0 to len-1) {
      list += genString()
    }

    list.toList

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