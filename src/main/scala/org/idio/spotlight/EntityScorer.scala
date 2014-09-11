package org.idio.spotlight

import org.dbpedia.spotlight.model.TokenType

/**
 * Created by dav009 on 10/09/2014.
 */
object EntityScorer {

  def score(vector1:Map[TokenType, Double],vector2:Map[TokenType, Double]):Double={
    var score :Double= 0.0
    val keysOverlap = vector1.keySet.intersect(vector2.keySet).toSet
    keysOverlap.foreach{
      token:TokenType =>

        score = score +( vector1.getOrElse(token, 0.0) * vector2.getOrElse(token, 0.0))

    }
    score / (getMagnitude(vector1)*getMagnitude(vector2))
  }

  def getMagnitude(vector:Map[TokenType, Double]):Double = {

    math.sqrt(vector.values.map(math.pow(_,2)).sum)

  }
}
