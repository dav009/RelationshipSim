package org.idio.spotlight

import java.io.{PrintWriter, File, FileInputStream}
import java.util.concurrent.atomic.AtomicInteger
import _root_.spray.json.DefaultJsonProtocol
import org.idio.spotlight.utils.VectorUtils
import scala.collection.JavaConversions._
import spray.json._
import DefaultJsonProtocol._
import org.idio.word2vec.Word2Vec

/**
 * Created by dav009 on 10/09/2014.
 */
class EntitySimilarity(pathToModelFolder:String, val typeSamples:Map[String, List[String]]){

  val model = new Word2Vec()
  model.load(pathToModelFolder)


  // build type vectors
  val typeVectors: Map[String, Array[Float]]  = {
    println("calculating type vectors....")
    typeSamples.mapValues(getVector(_))
  }


  def getVector(dbpediaIds:List[String]):  Array[Float] ={
    val contextVectors = dbpediaIds.map{
      entityName: String =>
        try{
          Some(model.vocab.get(entityName).get)
        }catch{
          case e:Exception => None
        }
    }.flatten.toList
    val releavanceScoreUtils = new VectorUtils()

    val mergedVector = releavanceScoreUtils.mergeVectors(contextVectors)
    //val preprocessedVector = releavanceScoreUtils.preprocessVector(mergedVector, 500)
    mergedVector
  }

  def getVector(dbpediaId:String): Array[Float] ={

    val contextVector:Array[Float] = model.vocab.get(dbpediaId).get
    contextVector
  }

  def getSimilarity(midType:String, entity:String): Double ={
    var score = -2.0

    try{
      val typeVector = typeVectors.get(midType).get
      val entityVector = getVector(entity)
      score = EntityScorer.score(typeVector, entityVector)
      //score = new EntityScorer(typeVector, entityVector).score()
    }catch{
      case e:Exception => println(e.getMessage())
    }

    score
  }

}

object EntitySimilarity{


  def loadTypeSamples(pathToFile: String):Map[String, List[String]]={
    val allFileContent = scala.io.Source.fromFile(pathToFile).getLines().mkString("\n")
    val typeSamples = allFileContent.parseJson.convertTo[Map[String, List[String]]]
    typeSamples
  }


  def main(args:Array[String]): Unit ={
    val pathToFileWithRels = args(0)
    val pathsToFileWithTypeSamples = args(1)
    val pathToSpotlightModel = args(2)
    val pathToOutputFile= args(3)

    val writer = new PrintWriter(new File(pathToOutputFile))

    println("loading type samples file..")
    val typeSamples = loadTypeSamples(pathsToFileWithTypeSamples)

    println("loading similarity calculator..")
    val similarityCalculator = new EntitySimilarity(pathToSpotlightModel, typeSamples)

    val allRelationshipLines = scala.io.Source.fromFile(pathToFileWithRels).getLines().toIterable

    println("calculating weights..")
    val counter:AtomicInteger = new AtomicInteger(0)
    allRelationshipLines.par.foreach{
      line:String =>

        try {
          counter.set(counter.get() + 1)
          println(counter.get()+"..")
          val relationship = line.trim().parseJson.convertTo[Map[String, String]]
          val typeId = relationship.get("type_id").get
          val topicDbpedia = relationship.get("topic_dbpedia").get
          val topicMid = relationship.get("topic_mid").get
          val similarityScore = similarityCalculator.getSimilarity(typeId, topicDbpedia)

          val lineToWrite:String = similarityScore +"\t" + topicMid  +"\t" + topicDbpedia  +"\t" + typeId + "\n"

          //println(lineToWrite)
          //println("---------------------")
          writer.write(lineToWrite)

        }catch {
          case e:Exception => {
            println(e.getMessage)

          }
        }


    }

    writer.close()

  }

}



