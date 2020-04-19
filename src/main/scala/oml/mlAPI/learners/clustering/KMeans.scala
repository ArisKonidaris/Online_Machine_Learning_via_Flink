package oml.mlAPI.learners.clustering

import oml.mlAPI.math.Breeze._
import oml.mlAPI.math.{DenseVector, Point, UnlabeledPoint, Vector}
import oml.FlinkBipartiteAPI.POJOs
import oml.mlAPI.learners.{Learner, OnlineLearner}
import oml.mlAPI.parameters.{Bucket, EuclideanVector, LearningParameters, ParameterDescriptor, VectorList}
import oml.mlAPI.scores.Scores

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Inspired from
  * http://www.cs.princeton.edu/courses/archive/fall08/cos436/Duda/C/sk_means.htm
  */
case class KMeans() extends OnlineLearner with Clusterer with Serializable {

  private var counts: ListBuffer[Long] = _

  private var centroids: VectorList = _

  private var nClusters: Int = 2

  private var initMethod: String = "random"

  private var trainingMethod: String = "forgetful"

  private var graceInit: Int = 10

  private var step: Double = 0.01

  private var initFeatures: ListBuffer[Point] = ListBuffer[Point]

  override def initialize_model(data: Point): Unit = {
    require(data.isInstanceOf[UnlabeledPoint])
    initFeatures.append(data.asInstanceOf[UnlabeledPoint])
    if (initFeatures.size >= graceInit * nClusters) initCentroids()
  }

  override def predict(data: Point): Option[Double] = {
    val dist: Array[Double] = distribution(data)
    if (!dist.isEmpty)
      Some(dist.zipWithIndex.min._2.toDouble)
    else
      None
  }

  override def fit(data: Point): Unit = {
    fitLoss(data)
    ()
  }

  override def fitLoss(data: Point): Double = {
    var loss: Double = 0D
    val dist: Array[Double] = distribution(data)
    if (!dist.isEmpty) {
      val prediction: Int = dist.zipWithIndex.min._2
      loss = Math.pow(dist(prediction), 2)

      val update = {
        if (trainingMethod.equals("sequential")) {
          counts(prediction) += 1
          (1.0 / counts(prediction)) * (data.getVector.asDenseBreeze - centroids.vectors(prediction).vector)
        } else step * (data.getVector.asDenseBreeze - centroids.vectors(prediction).vector)
      }
      centroids.vectors(prediction).vector += update
    } else initialize_model(data)
    loss
  }

  override def score(test_set: ListBuffer[Point]): Double = Scores.inertia(test_set, this)

  override def distribution(data: Point): Array[Double] = {
    if (this.counts != null && this.centroids != null) {
      (for (centroid: EuclideanVector <- centroids.vectors)
        yield breeze.linalg.functions.euclideanDistance(data.vector.asBreeze, centroid.vector)).toArray
    } else Array[Double]()
  }

  private def initCentroids(): Unit = {

    val initialCentroids: ListBuffer[EuclideanVector] = ListBuffer[EuclideanVector]

    // Init counts.
    if (trainingMethod.equals("sequential"))
      counts = ListBuffer[Long]((for (_ <- 0 until nClusters) yield 0L) : _ *)

    if (initMethod.equals("random")) {
      val randomIndexes: ListBuffer[Int] = ListBuffer[Int]
      while (randomIndexes.size < nClusters) {
        val r = Random.nextInt(initFeatures.size)
        if (!randomIndexes.contains(r)) randomIndexes.append(r)
      }
      for (randomIndex: Int <- randomIndexes)
        initialCentroids.append(EuclideanVector(initFeatures.remove(randomIndex).vector.asDenseBreeze))
    } else {
      // Choose the first centroid uniformly at random.
      initialCentroids.append(EuclideanVector(initFeatures.remove(Random.nextInt(initFeatures.size)).vector.asDenseBreeze))

      // Choose the rest of the centroids.
      for (_ <- 1 until nClusters) {

        // Calculate sum of D(x)^2.
        val dxs: Array[Double] = {
          var sum: Int = 0
          val dx: ListBuffer[Double] = ListBuffer[Double]
          for (initFeature <- initFeatures) {
            sum += Math.pow(distribution(initFeature).min, 2)
            dx.append(sum)
          }
          dx.toArray
        }

        val r: Double = Random.nextDouble() * dxs(dxs.length - 1)

        @scala.annotation.tailrec
        def addNewCentroid(index: Int): EuclideanVector = {
          require(index >= 0 && index <= dxs.length - 1)
          if (dxs(index) >= r)
            EuclideanVector(initFeatures.remove(index).vector.asDenseBreeze)
          else
            addNewCentroid(index + 1)
        }
        initialCentroids.append(addNewCentroid(0))

      }
    }
    centroids = VectorList(initialCentroids)
    for (point <- initFeatures) fit(point)
    initFeatures.clear()
  }

  override def getParameters: Option[LearningParameters] = Option(centroids)

  override def setParameters(params: LearningParameters): Learner = {
    assert(params.isInstanceOf[VectorList])
    centroids = params.asInstanceOf[VectorList]
    this
  }

  def setCounts(counts: ListBuffer[Long]): Unit = this.counts = counts

  def setNClusters(nClusters: Int): Unit = this.nClusters = nClusters

  def setInitMethod(initMethod: String): Unit = {
    if (initMethod.equals("random") || initMethod.equals("kmeans++"))
      this.initMethod = initMethod
    else
      throw new RuntimeException(s"Invalid argument $initMethod for initMethod " +
        s"hyper parameter of KMeans clusterer. Valid values: \"random\", \"kmeans++\"..")
  }

  def setTrainingMethod(trainingMethod: String): Unit = {
    if (initMethod.equals("forgetful") || initMethod.equals("sequential"))
      this.trainingMethod = trainingMethod
    else
      throw new RuntimeException(s"Invalid argument $trainingMethod for trainingMethod " +
        s"hyper parameter of KMeans clusterer. Valid values: \"forgetful\", \"sequential\".")
  }

  def setGraceInit(graceInit: Int): Unit = {
    if (graceInit > 0)
      this.graceInit = graceInit
    else
      throw new RuntimeException(s"Invalid argument $graceInit for graceInit " +
        s"hyper parameter of KMeans clusterer. Valid values: The positive integers.")
  }

  def setStep(step: Double): Unit = {
    if (step > 0.0 && step < 1.0)
      this.step = step
    else
      throw new RuntimeException(s"Invalid argument $step for step " +
        s"hyper parameter of KMeans clusterer. Valid values: The real (0.0, 1.0) interval.")
  }

  def setInitFeatures(initFeatures: ListBuffer[Point]): Unit = this.initFeatures = initFeatures

  override def setParametersFromMap(parameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((parameter, value) <- parameterMap) {
      parameter match {
        case "counts" =>
          try {
            val newCounts = value.asInstanceOf[java.util.List[Double]].asScala.map(x => x.toLong)
            if (counts == null || nClusters == newCounts.length)
              counts = ListBuffer[Long](newCounts : _ *)
            else
              throw new RuntimeException("Invalid size of new counts for the KMeans clusterer.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the counts of the KMeans clusterer.")
              e.printStackTrace()
          }
        case "centroids" =>
          try {
            val vl: ListBuffer[EuclideanVector] = ListBuffer[EuclideanVector]
            for (v: java.util.List[Double] <- value.asInstanceOf[java.util.List[java.util.List[Double]]].asScala)
              vl.append(new EuclideanVector(v.asScala.toArray))
            val newCentroids = VectorList(vl)
            if (centroids == null || centroids.vectors.length == newCentroids.vectors.length)
              centroids = newCentroids
            else
              throw new RuntimeException("Invalid number of centroids for the KMeans clusterer.")
          } catch {
            case e: Exception =>
              println("Error while trying to update the centroids of the KMeans clusterer.")
              e.printStackTrace()
          }
        case "initFeatures" =>
          try {
            val pl: ListBuffer[Point] = ListBuffer[Point]
            for (v: java.util.List[Double] <- value.asInstanceOf[java.util.List[java.util.List[Double]]].asScala)
              pl.append(UnlabeledPoint(DenseVector(v.asScala.toArray)))
            initFeatures = pl
          } catch {
            case e: Exception =>
              println("Error while trying to update the initFeatures of the KMeans clusterer.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def setHyperParametersFromMap(hyperParameterMap: mutable.Map[String, AnyRef]): Learner = {
    for ((hyperparameter, value) <- hyperParameterMap) {
      hyperparameter match {
        case "nClusters" =>
          try {
            setNClusters(value.asInstanceOf[Double].toInt)
          } catch {
            case e: Exception =>
              println("Error while trying to update the nClusters hyper parameter of KMeans clusterer.")
              e.printStackTrace()
          }
        case "initMethod" =>
          try {
            setInitMethod(value.asInstanceOf[String])
          } catch {
            case e: Exception =>
              println("Error while trying to update the initMethod hyper parameter of KMeans clusterer.")
              e.printStackTrace()
          }
        case "trainingMethod" =>
          try {
            setTrainingMethod(value.asInstanceOf[String])
          } catch {
            case e: Exception =>
              println("Error while trying to update the trainingMethod hyper parameter of KMeans clusterer.")
              e.printStackTrace()
          }
        case "graceInit" =>
          try {
            setGraceInit(value.asInstanceOf[Double].toInt)
          } catch {
            case e: Exception =>
              println("Error while trying to update the graceInit hyper parameter of KMeans clusterer.")
              e.printStackTrace()
          }
        case "step" =>
          try {
            setStep(value.asInstanceOf[Double])
          } catch {
            case e: Exception =>
              println("Error while trying to update the step hyper parameter of KMeans clusterer.")
              e.printStackTrace()
          }
        case _ =>
      }
    }
    this
  }

  override def toString = s"KMeans ${this.hashCode}"

  override def generateParameters: ParameterDescriptor => LearningParameters = new VectorList().generateParameters

  override def getSerializedParams: (LearningParameters , Boolean, Bucket) => (Array[Int], Vector) =
    new VectorList().generateSerializedParams

  override def generatePOJOLearner: POJOs.Learner = {
    new POJOs.Learner("KMeans",
      Map[String, AnyRef](
        ("nClusters", nClusters.asInstanceOf[AnyRef]),
        ("initMethod", initMethod.asInstanceOf[AnyRef]),
        ("trainingMethod", trainingMethod.asInstanceOf[AnyRef]),
        ("graceInit", graceInit.asInstanceOf[AnyRef]),
        ("step", step.asInstanceOf[AnyRef])
      ).asJava,
      Map[String, AnyRef](
        ("counts", if(counts == null) null else counts.toArray.asInstanceOf[AnyRef]),
        ("centroids",
          if(centroids == null)
            null
          else
            (for (centroid <- centroids.vectors) yield centroid.flatten.data).toArray.asInstanceOf[AnyRef]
        ),
        ("initFeatures",
          if(initFeatures == null)
            null
          else
            (for (initFeat <- initFeatures) yield initFeat.getVector.asDenseBreeze.data).toArray.asInstanceOf[AnyRef]
        )
      ).asJava
    )
  }
}
