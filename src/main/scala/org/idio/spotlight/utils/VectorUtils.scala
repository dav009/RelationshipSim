package org.idio.spotlight.utils

/**
 * Created by dav009 on 10/09/2014.
 */
object VectorUtils {


}



import scala.Predef._
import scala.collection.mutable
import scala.collection.JavaConverters._
/**
 * Created by dav009 on 03/02/2014.
 */
class VectorUtils  {





  def mergeVectors(vectors:List[Array[Float]]):Array[Float]={
    val mergedVector : scala.collection.mutable.ArrayBuffer[Float]= new scala.collection.mutable.ArrayBuffer[Float](vectors(0).size)


    vectors.foreach{
         vector:Array[Float] =>
           for((x,i) <- vector.view.zipWithIndex) mergedVector(i)=x
    }

    mergedVector.map(_/vectors.size).toArray

  }


}