package org.idio.vectors

/**
 * Created with IntelliJ IDEA.
 * User: dav009
 * Date: 12/09/2014
 * Time: 16:02
 * To change this template use File | Settings | File Templates.
 */
trait FeatureVectorStore {


  def getSimilarity(typeKey:String, objectKey:String):Double

}
