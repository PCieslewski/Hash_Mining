import scala.collection.mutable
import scala.collection.mutable.ListBuffer

//This classes purpose is to iteratively generate new hash strings.
class StringGen(base: String){
  val chars = ('!' to '~')
  var perms:mutable.MutableList[Int] = mutable.MutableList(0)

  //Create the next string to hash
  def genString(): String = {

    val sb = new StringBuilder(base)
    for (i <- perms.indices) {
      sb.append(chars(perms(i)))
    }
    incPerms()
    sb.toString

  }

  //Create a block of strings to hash
  def genStringBlock(len: Int): List[String] = {

    val list = new ListBuffer[String]()

    for (i <- 0 to len-1) {
      list += genString()
    }

    list.toList

  }

  //Keep track of where we are in our string generation.
  def incPerms(){

    for(i <- 0 to perms.length-2) {
      perms(i) = perms(i) + 1
      if (perms(i) < chars.length) {
        return
      }
      else {
        perms(i) = 0
      }
    }

    //Handle the last spot in the list
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