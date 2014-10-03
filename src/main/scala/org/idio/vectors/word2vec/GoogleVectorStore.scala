package org.idio.vectors.word2vec

import org.idio.vectors.FeatureVectorStore
import org.idio.vectors.word2vec.GoogleVectorStore.Word2VecVector
import scala.collection.JavaConversions._
import scala.collection.par._
import scala.collection.par.Scheduler.Implicits.global

object GoogleVectorStore {
  type Word2VecVector = Array[Float]
}

/**
 * Represents a way to Store and Access the Word2Vec Vectors
 */
class GoogleVectorStore(pathToModelFolder:String, typeSamples:Map[String, List[String]]) extends FeatureVectorStore{

  // Deserialize the Word2VecModels
  val word2vec: Word2VEC = new Word2VEC()
  word2vec.loadModel(pathToModelFolder)

  /*
  * Since the type vectors do not exist in the word2vec models
  * we have to create type vectors by:
  *     - merging the vectors of the notable topics surrounding a type.
  *       i.e: Type: Politician -- Vector is created from merging (Barack Obama, Angela Merkel ...etc)
  * */
  val typeVectors: Map[String, Word2VecVector]  = {
    println("calculating type vectors....")
    // For each type create TypeVector
    typeSamples.par.mapValues(createTypeVector(_)).seq.collect {
      case (key, Some(value)) =>
             key -> value
    }
  }.asInstanceOf[scala.collection.immutable.Map[String, Word2VecVector]]

  /*
  *  Returns the similarity between a type an an entity
  * */
  def getSimilarity(midType: String, entity: String): Double ={
    var simScore = -2.0
    try{
      val entityVector = getVector(entity)
      val typeVector = typeVectors.get(midType).get
      simScore = score(typeVector, entityVector)
    }catch{
      case e:Exception => e.printStackTrace()
    }
    simScore
  }

  /*
 *  Calculates the Cosine distance between two Word2Vec Vectors
 * */
  private def cosine(vec1: Word2VecVector, vec2: Word2VecVector): Double = {
    var dot, sum1, sum2 = 0.0
    for (i <- 0 until vec1.length) {
      dot += (vec1(i) * vec2(i))
      sum1 += (vec1(i) * vec1(i))
      sum2 += (vec2(i) * vec2(i))
    }
    dot / (math.sqrt(sum1) * math.sqrt(sum2))
  }

  /*
  * Calculates a score between two word2vec Vectors
  * */
  private def score(vector1: Word2VecVector,vector2: Word2VecVector): Double={
    cosine(vector1, vector2)
  }

  /*
  * Given a list of Vectors returned a merged version of the vectors.
  * This is used for creating the type-vectors.
  * */
  def mergeVectors(vectors: List[Word2VecVector]): Option[Word2VecVector]={
    if (vectors.size>0) {

         val mergedVector: scala.collection.mutable.ArrayBuffer[Float] =
                    scala.collection.mutable.ArrayBuffer.fill(vectors(0).size)((0.0).toFloat)

         // the dimensions of the new vector is the average per dimension
         vectors.foreach {
              vector: Word2VecVector =>
                  for ((x, i) <- vector.view.zipWithIndex) mergedVector(i) = mergedVector(i) + x
         }
        Some(mergedVector.map(_ / vectors.size).toArray)
    }
    else{
        None
    }
  }

  /*
  * Given a list of Keys it gets all the vectors of those keys and creates a TypeVector
  * by Merging them
  * */
  private def createTypeVector(listOfKeys: List[String]): Option[Word2VecVector] ={
    val contextVectors = listOfKeys.map{
      entityName: String =>
        try{
          // dirty trick , it removes the first slash from a standard mid (/m/123-> m/123)
          // reasong being that mids stored in word2vec are not standard
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

    val mergedVector: Option[Word2VecVector]
                        = mergeVectors(contextVectors.slice(0,500))
   mergedVector
  }

  /*
  * Returns a vector existing in word2vec given an mid
  * */
  def getVector(mid:String): Word2VecVector ={
    val contextVector: Word2VecVector = word2vec.getWordVector(mid.slice(1, mid.size))
    contextVector
  }

}
