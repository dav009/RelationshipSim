package org.idio.spotlight

import java.io.{PrintWriter, File, FileInputStream}
import java.util.concurrent.atomic.AtomicInteger
import _root_.spray.json.DefaultJsonProtocol
import scala.collection.JavaConversions._
import spray.json._
import DefaultJsonProtocol._
import org.idio.vectors.word2vec.{GoogleVectorStore}
import org.idio.vectors.FeatureVectorStore
import org.idio.vectors.spotlight.SpotlightVectorStore

/**
 * Created by dav009 on 10/09/2014.
 */
class EntitySimilarity(val vectorStore:FeatureVectorStore){

  def calculateSimilarities(pathToOutputFile:String, pathToRelsFile:String, objectKey:String){


    val allRelationshipLines = scala.io.Source.fromFile(pathToRelsFile).getLines().toIterable

    val writer = new PrintWriter(new File(pathToOutputFile))

    println("calculating weights..")
    val counter:AtomicInteger = new AtomicInteger(0)
    allRelationshipLines.par.foreach{
      line:String =>

        try {
          counter.set(counter.get() + 1)
          println(counter.get()+"..")

          // Deserializing the relationship object
          val relationship = line.trim().parseJson.convertTo[Map[String, String]]
          val typeId = relationship.get("type_id").get
          val topicDbpedia = relationship.get("topic_dbpedia").get
          val topicMid = relationship.get("topic_mid").get
          val objectKeyValue = relationship.get(objectKey).get

          val similarityScore = vectorStore.getSimilarity(typeId, objectKeyValue)

          val lineToWrite:String = similarityScore +"\t" + topicMid  +"\t" + topicDbpedia  +"\t" + typeId + "\n"
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

object EntitySimilarity{


  def loadTypeSamples(pathToFile: String):Map[String, List[String]]={
    val allFileContent = scala.io.Source.fromFile(pathToFile).getLines().mkString("\n")
    val typeSamples = allFileContent.parseJson.convertTo[Map[String, List[String]]]
    typeSamples
  }


  def main(args:Array[String]): Unit ={

    val pathToFileWithRels = args(0)
    val pathsToFileWithTypeSamples = args(1)
    val pathToVectorModel = args(2)
    val pathToOutputFile= args(3)
    val choice=args(4)

    println("loading type samples file..")
    val typeSamples = loadTypeSamples(pathsToFileWithTypeSamples)

    val tokenStore:FeatureVectorStore = choice match{
      case "word2vec" => new SpotlightVectorStore(pathToVectorModel, typeSamples)
      case "spotlight" => new GoogleVectorStore(pathToVectorModel, typeSamples)
    }

    val objectKey:String = choice match{
      case "word2vec" => "topic_mid"
      case "spotlight" => "topic_dbpedia"
    }


    println("loading similarity calculator..")
    val similarityCalculator = new EntitySimilarity(tokenStore)

    similarityCalculator.calculateSimilarities(pathToOutputFile, pathToFileWithRels, objectKey)

  }

}



