package org.idio.vectors.word2vec

import org.idio.vectors.FeatureVectorStore
import scala.collection.JavaConversions._
import scala.collection.par._
import scala.collection.par.Scheduler.Implicits.global
/**
 * Created with IntelliJ IDEA.
 * User: dav009
 * Date: 12/09/2014
 * Time: 16:14
 * To change this template use File | Settings | File Templates.
 */
class GoogleVectorStore(pathToModelFolder:String, typeSamples:Map[String, List[String]]) extends FeatureVectorStore{

  val word2vec: Word2VEC = new Word2VEC()
  word2vec.loadModel(pathToModelFolder)

  // build type vectors
  val typeVectors: Map[String, Array[Float]]  = {
    println("calculating type vectors....")
    typeSamples.par.mapValues(createTypeVector(_)).seq.collect {
      case (key, Some(value)) => key -> value
    }
  }.asInstanceOf[scala.collection.immutable.Map[String,Array[Float]]]

  def cosine(vec1: Array[Float], vec2: Array[Float]): Double = {
    var dot, sum1, sum2 = 0.0
    for (i <- 0 until vec1.length) {
      dot += (vec1(i) * vec2(i))
      sum1 += (vec1(i) * vec1(i))
      sum2 += (vec2(i) * vec2(i))
    }
    dot / (math.sqrt(sum1) * math.sqrt(sum2))
  }

  def score(vector1:Array[Float],vector2:Array[Float]):Double={
    cosine(vector1, vector2)
  }

  def getSimilarity(midType:String, entity:String): Double ={
    var simScore = -2.0

    try{


      val entityVector = getVector(entity)
      val typeVector = typeVectors.get(midType).get
      simScore = score(typeVector, entityVector)
      //score = new EntityScorer(typeVector, entityVector).score()
    }catch{
      case e:Exception => e.printStackTrace()
    }

    simScore
  }

  private def createTypeVector(listOfKeys:List[String]):Option[Array[Float]] ={
    val contextVectors = listOfKeys.map{
      entityName: String =>
        try{

          val mEntityName = entityName.slice(1, entityName.size)
          //println("creating type vector")
          //println("entity:" + mEntityName)
          val vector = word2vec.getWordVector(mEntityName)
          //println("vector:"+ vector)
          //println("---")
          Some(vector)
        }catch{
          case e:Exception => None
        }
    }.flatten.toList

    val mergedVector:Option[Array[Float]] = mergeVectors(contextVectors.slice(0,500))
   mergedVector
  }

  def getVector(mid:String): Array[Float] ={
    val contextVector:Array[Float] = word2vec.getWordVector(mid.slice(1, mid.size))
    contextVector
  }



  def mergeVectors(vectors:List[Array[Float]]):Option[Array[Float]]={
     if (vectors.size>0) {
       val mergedVector: scala.collection.mutable.ArrayBuffer[Float] =  scala.collection.mutable.ArrayBuffer.fill(vectors(0).size)((0.0).toFloat)

       vectors.foreach {
         vector: Array[Float] =>
           for ((x, i) <- vector.view.zipWithIndex) mergedVector(i) = mergedVector(i) + x
       }
       Some(mergedVector.map(_ / vectors.size).toArray)
     }
     else{
       None
     }
  }

}
