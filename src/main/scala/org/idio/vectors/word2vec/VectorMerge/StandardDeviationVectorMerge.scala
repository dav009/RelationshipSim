package scala.org.idio.vectors.word2vec.VectorMerge

import org.idio.vectors.word2vec.GoogleVectorStore.Word2VecVector

/**
 * Created by dav009 on 03/10/2014.
 */
class StandardDeviationVectorMerge(vectors: List[Word2VecVector]) extends VectorMerger{


  val totalDimensions = vectors(0).size
  val numberOfSelectedDimensions:Int = (0.4 * totalDimensions).floor.toInt
  val avgOfDimension: Seq[Pair[Double, Int]]  = {

    val range =  0 to totalDimensions

    val avgOfDimension = range.map{
      index: Int =>
        vectors.foldLeft(0.0){
          (sum:Double, vector: Array[Float]) =>
            val dimension = vector(index)
            sum + dimension
        }
    }.map(_/vectors.size).zipWithIndex

    avgOfDimension

  }

  val deviations: Seq[Pair[Double, Int]] = {
    avgOfDimension.map{
      case (avg: Double, index: Int) =>
        vectors.foldLeft(0.0){
          (sum:Double, vector: Array[Float]) =>
            val dimension = vector(index)
            Math.pow(avg - dimension, 2) + sum
        }
    }.map(_/vectors.size).map(Math.sqrt(_)).zipWithIndex
  }


  def merge(): Option[Word2VecVector] = {
    if (vectors.size>0){
      Some(this.mergeVectorsPickDimensions(vectors))
    }else{
      None
    }
  }

  def selectDimensions(vectors:List[Word2VecVector]): Set[Int] = {
    // Select a % of dimensions with best std
    val selectedDimensions = deviations.sortBy(_._1).reverse.slice(0, numberOfSelectedDimensions).map(_._2).toSet
    selectedDimensions
  }


  def mergeVectorsPickDimensions(vectors:List[Array[Float]]):Array[Float]={
    // new vector
    val mergedVector : scala.collection.mutable.ArrayBuffer[Float]= new scala.collection.mutable.ArrayBuffer[Float](vectors(0).size)

    // getting the selected Dimensions
    val selectedDimensions = selectDimensions(vectors)

    // make vectors with selected dimensiones
    val avgMap = avgOfDimension.map{ case (value:Double,index:Int) => (index,value) }.toMap

    vectors.foreach{
      vector:Array[Float] =>
        for((x,i) <- vector.view.zipWithIndex){
          if (selectedDimensions.contains(i)){
            //
            mergedVector(i)= avgMap.get(i).get.toFloat
          }
          else{
            // Filled bad dimensions with zeroies
            mergedVector(i) =(0.0).toFloat
          }
        }
    }

    mergedVector.toArray

  }

}
