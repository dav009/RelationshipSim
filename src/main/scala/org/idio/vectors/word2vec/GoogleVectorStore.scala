package org.idio.vectors.word2vec

import org.idio.vectors.FeatureVectorStore
import scala.collection.JavaConversions._

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
    typeSamples.mapValues(createTypeVector(_))
  }

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
      val typeVector = typeVectors.get(midType).get
      val entityVector = getVector(entity)
      simScore = score(typeVector, entityVector)
      //score = new EntityScorer(typeVector, entityVector).score()
    }catch{
      case e:Exception => println(e.getMessage())
    }

    simScore
  }

  private def createTypeVector(listOfKeys:List[String]):Array[Float] ={
    val contextVectors = listOfKeys.map{
      entityName: String =>
        try{

          Some(word2vec.getWordVector(entityName))
        }catch{
          case e:Exception => None
        }
    }.flatten.toList

    val mergedVector:Array[Float] = mergeVectors(contextVectors)
   mergedVector
  }

  def getVector(mid:String): Array[Float] ={
    val contextVector:Array[Float] = word2vec.getWordVector(mid)
    contextVector
  }



  def mergeVectors(vectors:List[Array[Float]]):Array[Float]={
      val mergedVector : scala.collection.mutable.ArrayBuffer[Float]= new scala.collection.mutable.ArrayBuffer[Float](vectors(0).size)

      vectors.foreach{
        vector:Array[Float] =>
          for((x,i) <- vector.view.zipWithIndex) mergedVector(i)=x
      }
      mergedVector.map(_/vectors.size).toArray
  }

}
