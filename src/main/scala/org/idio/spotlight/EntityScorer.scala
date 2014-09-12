package org.idio.spotlight


/**
 * Created by dav009 on 10/09/2014.
 */
object EntityScorer {

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


}


