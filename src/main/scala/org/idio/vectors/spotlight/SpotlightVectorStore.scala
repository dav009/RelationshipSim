//package org.idio.vectors.spotlight
//
//import org.idio.vectors.FeatureVectorStore
//import org.dbpedia.spotlight.model.{DBpediaResource, TokenType}
//import java.io.{File, FileInputStream}
//import scala.collection.mutable
//import scala.collection.JavaConversions._
//import scala.collection.JavaConverters._
//import scala.Predef._
//import org.dbpedia.spotlight.db.memory.{MemoryContextStore, MemoryResourceStore, MemoryStore, MemoryTokenTypeStore}
//
///**
// * Created with IntelliJ IDEA.
// * User: dav009
// * Date: 12/09/2014
// * Time: 16:14
// * To change this template use File | Settings | File Templates.
// */
//class SpotlightVectorStore(pathToModelFolder:String, typeSamples:Map[String, List[String]]) extends FeatureVectorStore{
//
//
//  val tokenMemFile = new FileInputStream(new File(pathToModelFolder, "tokens.mem"))
//  var tokenStore: MemoryTokenTypeStore = MemoryStore.loadTokenTypeStore(tokenMemFile)
//
//  val resourceFile = new FileInputStream(new File(pathToModelFolder, "res.mem"))
//  var resStore: MemoryResourceStore = MemoryStore.loadResourceStore(resourceFile)
//
//  val contextMemFile = new FileInputStream(new File(pathToModelFolder, "context.mem"))
//  var contextStore: MemoryContextStore = MemoryStore.loadContextStore(contextMemFile, this.tokenStore)
//
//
//  // build type vectors
//  val typeVectors: Map[String, Map[TokenType, Double]]  = {
//    println("calculating type vectors....")
//    typeSamples.mapValues(createTypeVector(_))
//  }
//
//  def getSimilarity(typeKey:String, entityKey:String):Double={
//    var simScore = -2.0
//
//    try{
//      val typeVector = typeVectors.get(typeKey).get
//      val entityVector = getVector(entityKey)
//      simScore = score(typeVector, entityVector)
//    }catch{
//      case e:Exception => println(e.getMessage())
//    }
//
//    simScore
//  }
//
//  private def createTypeVector(listOfKeys:List[String])={
//    val typeResources = listOfKeys.map{
//      entityName: String =>
//        try{
//          Some(resStore.getResourceByName(entityName))
//        }catch{
//          case e:Exception => None
//        }
//    }.flatten.toList
//
//    val contextVectors : List[Map[TokenType, Int]]= typeResources.map(contextStore.getContextCounts(_).toMap)
//    val mergedVector = mergeVectors(contextVectors)
//    val preprocessedVector = preprocessVector(mergedVector, 500)
//    preprocessedVector
//  }
//
//  def getVector(dbpediaId:String): Map[TokenType, Double] ={
//    val resource:DBpediaResource = resStore.getResourceByName(dbpediaId);
//    val contextVector:java.util.Map[TokenType, Int] = contextStore.getContextCounts(resource)
//    val preprocessedVector = preprocessVector(contextVector, 500)
//    preprocessedVector
//  }
//
//  /*
//   * Preprocess a vector so that:
//   * - vector contains maxNumberOfDimensions whose frequencies are the highest.
//   * - vector is normalized
//   *
//   * */
//  def preprocessVector(vector:java.util.Map[TokenType, Int], maxNumberOfDimensions:Int):Map[TokenType, Double]={
//    //Prune Dimensions
//    val prunedVector = vector.asScala.toSeq.sortBy(_._2).reverse.slice(0, maxNumberOfDimensions).toMap
//    //Normalize
//    val normalizedVector:Map[TokenType, Double] = normalizeVector(prunedVector)
//    return normalizedVector
//  }
//
//
//  def mergeVectors(vectors:List[Map[TokenType, Int]]):Map[TokenType, Int]={
//    val mergedVector : scala.collection.mutable.HashMap[TokenType, Int]= new scala.collection.mutable.HashMap[TokenType, Int]()
//    vectors.foreach{
//      contextVector: Map[TokenType, Int] =>
//        contextVector.foreach{
//          case(key:TokenType, value:Int) =>
//            val newCountValue = mergedVector.getOrElse(key, 0) + value
//            mergedVector.put(key,newCountValue)
//        }
//
//    }
//    mergedVector.toMap
//  }
//
//  def score(vector1:Map[TokenType, Double],vector2:Map[TokenType, Double]):Double={
//    var score :Double= 0.0
//    val keysOverlap = vector1.keySet.intersect(vector2.keySet).toSet
//    keysOverlap.foreach{
//      token:TokenType =>
//
//        score = score +( vector1.getOrElse(token, 0.0) * vector2.getOrElse(token, 0.0))
//
//    }
//    score / (getMagnitude(vector1)*getMagnitude(vector2))
//  }
//
//  def getMagnitude(vector:Map[TokenType, Double]):Double = {
//
//    math.sqrt(vector.values.map(math.pow(_,2)).sum)
//
//  }
//
//  /*
// * returns a normalized version of vector
// * */
//  def normalizeVector(vector:Map[TokenType, Int]):Map[TokenType, Double]={
//    val totalSumOfTokens = vector.values.sum
//    var normalizedVector = mutable.Map[TokenType, Double]()
//    for( (token, counts) <- vector){
//      val normalizedCount = counts / totalSumOfTokens.toDouble
//      normalizedVector(token) = normalizedCount
//    }
//
//    return normalizedVector.toMap
//  }
//}
