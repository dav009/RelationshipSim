package org.idio.spotlight

import java.io.{PrintWriter, File, FileInputStream}
import java.util.concurrent.atomic.AtomicInteger
import _root_.spray.json.DefaultJsonProtocol
import org.dbpedia.spotlight.db.memory.{MemoryTokenTypeStore, MemoryContextStore, MemoryStore, MemoryResourceStore}
import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
import org.idio.spotlight.utils.VectorUtils
import scala.collection.JavaConversions._
import spray.json._
import DefaultJsonProtocol._

/**
 * Created by dav009 on 10/09/2014.
 */
class EntitySimilarity(pathToModelFolder:String, val typeSamples:Map[String, List[String]]){

  val tokenMemFile = new FileInputStream(new File(pathToModelFolder, "tokens.mem"))
  var tokenStore: MemoryTokenTypeStore = MemoryStore.loadTokenTypeStore(tokenMemFile)

  val resourceFile = new FileInputStream(new File(pathToModelFolder, "res.mem"))
  var resStore: MemoryResourceStore = MemoryStore.loadResourceStore(resourceFile)

  val contextMemFile = new FileInputStream(new File(pathToModelFolder, "context.mem"))
  var contextStore: MemoryContextStore = MemoryStore.loadContextStore(contextMemFile, this.tokenStore)

  // build type vectors
  val typeVectors: Map[String, Map[TokenType, Double]]  = {
    println("calculating type vectors....")
    typeSamples.mapValues(getVector(_))
  }


  def getVector(dbpediaIds:List[String]): Map[TokenType, Double] ={
    val typeResources = dbpediaIds.map{
      entityName: String =>
        try{
          Some(resStore.getResourceByName(entityName))
        }catch{
          case e:Exception => None
        }
    }.flatten.toList
    val releavanceScoreUtils = new VectorUtils()
    val contextVectors : List[Map[TokenType, Int]]= typeResources.map(contextStore.getContextCounts(_).toMap)
    val mergedVector = releavanceScoreUtils.mergeVectors(contextVectors)
    val preprocessedVector = releavanceScoreUtils.preprocessVector(mergedVector, 500)
    preprocessedVector
  }

  def getVector(dbpediaId:String): Map[TokenType, Double] ={
    val resource:DBpediaResource = resStore.getResourceByName(dbpediaId);
    val contextVector:java.util.Map[TokenType, Int] = contextStore.getContextCounts(resource)
    val releavanceScoreUtils = new VectorUtils()
    val preprocessedVector = releavanceScoreUtils.preprocessVector(contextVector, 500)
    preprocessedVector
  }

  def getSimilarity(midType:String, entity:String): Double ={
    var score = -2.0

    try{
      val typeVector = typeVectors.get(midType).get
      val entityVector = getVector(entity)
      score = new EntityScorer(typeVector, entityVector).score()
    }catch{
      case e:Exception =>
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

    println("loading type samples file..")
    val typeSamples = loadTypeSamples(pathsToFileWithTypeSamples)

    println("loading similarity calculator..")
    val similarityCalculator = new EntitySimilarity(pathToSpotlightModel, typeSamples)

    val allRelationshipLines = scala.io.Source.fromFile(pathToFileWithRels).getLines().toIterable

    println("calculating weights..")
    val counter:AtomicInteger = new AtomicInteger(0)
    val weightedRelationships = allRelationshipLines.par.map{
      line:String =>
        val relationship = line.trim().parseJson.convertTo[Map[String, String]]
        try {
          val typeId = relationship.get("type_id").get
          val topicDbpedia = relationship.get("topic_dbpedia").get
          val topicMid = relationship.get("topic_mid").get
          val similarityScore = similarityCalculator.getSimilarity(typeId, topicDbpedia)
          Some( (similarityScore, topicMid, topicDbpedia, typeId) )
          counter.set(counter.get() + 1)
          println(counter.get()+"..")
        }catch {
          case e:Exception =>  None
        }


    }.flatten



    // write them to file
    println("saving file..")
    val writer = new PrintWriter(new File(pathToOutputFile))

    weightedRelationships.toList.foreach{
        case(similarityScore:Double, topicMid:String, topicDbpedia:String, typeId:String) =>
          writer.write(similarityScore +"\t" + topicMid  +"\t" + topicDbpedia  +"\t" + typeId + "\n")
    }

    writer.close()

  }

}



