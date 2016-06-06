package org.qcri.rheem.apps.kmeans

import java.util

import org.qcri.rheem.api._
import org.qcri.rheem.core.api.RheemContext
import org.qcri.rheem.core.function.ExecutionContext
import org.qcri.rheem.core.function.FunctionDescriptor.ExtendedSerializableFunction
import org.qcri.rheem.core.platform.Platform
import org.qcri.rheem.java.JavaPlatform
import org.qcri.rheem.spark.platform.SparkPlatform
import scala.collection.JavaConversions._

import scala.util.Random

/**
  * K-Means app for Rheem.
  */
class Kmeans(k: Int, inputFile: String, iterations: Int = 20) {

  def run(platforms: Platform*): Iterable[Point] = {
    // Set up the RheemContext.
    implicit val rheemCtx = new RheemContext
    platforms.foreach(rheemCtx.register)

    // Read and parse the input file(s).
    val points = rheemCtx
      .readTextFile(inputFile).withName("Read file")
      .map { line =>
        val fields = line.split(",")
        Point(fields(0).toDouble, fields(1).toDouble)
      }.withName("Create points")

    // Create initial centroids.
    val initialCentroids = rheemCtx
      .readCollection(createRandomCentroids(k)).withName("Load random centroids")

    // Do the k-means loop.
    val finalCentroids = initialCentroids.repeat(iterations, { currentCentroids =>
      val newCentroids = points
        .mapJava(new SelectNearestCentroid).withBroadcast(currentCentroids, "centroids").withName("Find nearest centroid")
        .reduceByKey(_.centroidId, _ + _).withName("Add up points")
        .map(_.average).withName("Average points")

      newCentroids
    }).withName("Loop")

    // Collect the result.
    finalCentroids
      .map(_.toPoint).withName("Strip centroid names")
      .collect()
  }


  /**
    * Creates random centroids.
    *
    * @param n      the number of centroids to create
    * @param random used to draw random coordinates
    * @return the centroids
    */
  def createRandomCentroids(n: Int, random: Random = new Random()) =
    for (i <- 1 to n) yield TaggedPoint(random.nextDouble(), random.nextDouble(), i)

}

/**
  * Companion object of [[Kmeans]].
  */
object Kmeans {

  def main(args: String*): Unit = {
    if (args.length != 3) {
      println("Usage: scala <main class> <platform(,platform)*> <point file> <k> <#iterations>")
      sys.exit(1)
    }

    val platforms = args(0).split(",").map {
      case "spark" => SparkPlatform.getInstance
      case "java" => JavaPlatform.getInstance
      case other: String => throw new IllegalArgumentException(s"Unsupported platform: ${other}.")
      case _ => throw new IllegalArgumentException
    }
    val file = args(1)
    val k = args(2).toInt
    val numIterations = args(3).toInt

    val centroids = run(file, k, numIterations, platforms: _*)

    // Print the result.
    println(s"Found ${centroids.size} centroids:")

  }

  def run(file: String, k: Int, numIterations: Int, platforms: Platform*) = {
    new Kmeans(k, file, numIterations).run(platforms: _*)
  }

}

/**
  * UDF to select the closest centroid for a given [[Point]].
  */
class SelectNearestCentroid extends ExtendedSerializableFunction[Point, TaggedPointCounter] {

  /** Keeps the broadcasted centroids. */
  var centroids: util.Collection[TaggedPoint] = _

  override def open(executionCtx: ExecutionContext) = {
    centroids = executionCtx.getBroadcast[TaggedPoint]("centroids")
  }

  override def apply(point: Point): TaggedPointCounter = {
    var minDistance = Double.PositiveInfinity
    var nearestCentroidId = -1
    for (centroid <- centroids) {
      val distance = point.distanceTo(centroid)
      if (distance < minDistance) {
        minDistance = distance
        nearestCentroidId = centroid.centroidId
      }
    }
    new TaggedPointCounter(point, nearestCentroidId, 1)
  }
}


/**
  * Represents objects with an x and a y coordinate.
  */
sealed trait PointLike {

  /**
    * @return the x coordinate
    */
  def x: Double

  /**
    * @return the y coordinate
    */
  def y: Double

}

/**
  * Represents a two-dimensional point.
  *
  * @param x the x coordinate
  * @param y the y coordinate
  */
case class Point(x: Double, y: Double) extends PointLike {

  /**
    * Calculates the Euclidean distance to another [[Point]].
    *
    * @param that the other [[PointLike]]
    * @return the Euclidean distance
    */
  def distanceTo(that: PointLike) = {
    val dx = this.x - that.x
    val dy = this.y - that.y
    math.sqrt(dx * dx + dy * dy)
  }

  override def toString: String = f"($x%.2f, $y%.2f)"
}

/**
  * Represents a two-dimensional point with a centroid ID attached.
  */
case class TaggedPoint(x: Double, y: Double, centroidId: Int) extends PointLike {

  /**
    * Creates a [[Point]] from this instance.
    *
    * @return the [[Point]]
    */
  def toPoint = Point(x, y)

}

/**
  * Represents a two-dimensional point with a centroid ID and a counter attached.
  */
case class TaggedPointCounter(x: Double, y: Double, centroidId: Int, count: Int = 1) extends PointLike {

  def this(point: PointLike, centroidId: Int, count: Int = 1) = this(point.x, point.y, centroidId, count)

  /**
    * Adds coordinates and counts of two instances.
    *
    * @param that the other instance
    * @return the sum
    */
  def +(that: TaggedPointCounter) = TaggedPointCounter(this.x + that.x, this.y + that.y, this.centroidId, this.count + that.count)

  /**
    * Calculates the average of all added instances.
    *
    * @return a [[TaggedPoint]] reflecting the average
    */
  def average = TaggedPoint(x / count, y / count, centroidId)

}
