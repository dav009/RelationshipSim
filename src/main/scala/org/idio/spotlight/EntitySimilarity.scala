package org.idio.spotlight

import java.io.{PrintWriter, File, FileInputStream}
import java.util.concurrent.atomic.AtomicInteger
import _root_.spray.json.DefaultJsonProtocol
import scala.collection.JavaConversions._
import spray.json._
import DefaultJsonProtocol._
import org.idio.vectors.word2vec.{GoogleVectorStore}
import org.idio.vectors.FeatureVectorStore

import scala.collection.par._
import scala.collection.par.Scheduler.Implicits.global

/**
 *  Calculates the similarities between freebase types and other entities
 */
class EntitySimilarity(val vectorStore:FeatureVectorStore){


  /*
  *
  * */
  def calculateSimilarities(pathToOutputFile: String,
                            pathToRelsFile: String,
                            objectKey: String){


    // Gets all the relationships
    val allRelationshipLines = scala.io.Source.fromFile(pathToRelsFile).getLines().toIterable

    // Outputfile
    val writer = new PrintWriter(new File(pathToOutputFile))

    println("calculating weights..")

    // Printing how many lines have been processed
    val counter:AtomicInteger = new AtomicInteger(0)



    allRelationshipLines.toParArray.foreach{
      line:String =>

        try {

          counter.set(counter.get() + 1)
          println(counter.get()+"..")

          // Deserializing the relationship object
          val relationship = line.trim().parseJson.convertTo[Map[String, String]]

          // The idenfitifer of the typeTopic
          val typeId = relationship.get("type_id").get

          // The dbpediaId and Mid/enid of the nonTypeTopic
          val topicDbpedia = relationship.get("topic_dbpedia").get
          val topicMid = relationship.get("topic_mid").get
          val topicIdentifier = relationship.get(objectKey).get

          println("getting sim:"+ typeId +" and " + topicIdentifier )

          // calculate the similaity between the type and the topic
          val similarityScore = vectorStore.getSimilarity(typeId, topicIdentifier)

          println(similarityScore)
          println("-----------------")

          val lineToWrite:String = similarityScore +"\t" + topicMid  +"\t" + topicDbpedia  +"\t" + typeId + "\n"

          // save in the outputFile
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


  def main(args: Array[String]): Unit ={

    /*
    * Path to file where every line is a json dictionary:
    *  {  "type_id": /m/123, "topic_mid":/m/abc, "topic_dbpedia":Wikipedia_Title }
    * */
    val pathToFileWithRels = args(0)

    /*
    * Path to file with a json dict:
    * { "midType1": [ keyOfNotable1, keyOfNotable2, ...] ..}
    * */
    val pathsToFileWithTypeSamples = args(1)

    /*
    * Path to word2vec serialized model
    * */
    val pathToVectorModel = args(2)

    /*
    * Path to file where rels scores will be output
    * */
    val pathToOutputFile = args(3)

    /*
    * Choose whether calculate the similarity using word2vec vectors or spotlight vectors
    * options:
    *     - word2vec
    *     - spotlight
    * */
    val choice = args(4)

    println("loading type samples file..")
    val typeSamples = loadTypeSamples(pathsToFileWithTypeSamples)


    val tokenStore:FeatureVectorStore = choice match{
      // spotlight vectors can't be read on scala 2.10...
      //case "spotlight" => new SpotlightVectorStore(pathToVectorModel, typeSamples)
      case "word2vec" => new GoogleVectorStore(pathToVectorModel, typeSamples)
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



