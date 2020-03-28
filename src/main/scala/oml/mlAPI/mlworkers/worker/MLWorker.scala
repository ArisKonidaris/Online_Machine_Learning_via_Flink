package oml.mlAPI.mlworkers.worker

import java.io.Serializable
import java.util

import oml.FlinkBipartiteAPI.POJOs.Request
import oml.StarTopologyAPI.annotations.Inject
import oml.StarTopologyAPI.futures.PromiseResponse
import oml.mlAPI.math.{Point, Vector}
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlpipeline.MLPipeline
import oml.mlAPI.mlworkers.interfaces.Querier
import oml.mlAPI.parameters.{Bucket, LearningParameters, ParameterDescriptor}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** An abstract base class of an Online Machine Learning worker.
  *
  * @tparam T The interface of the parameter server proxy.
  */
abstract class MLWorker[T <: PullPush]() extends Serializable {

  /** The distributed training protocol. */
  protected var protocol: String = _

  /** The total number of data points fitted to the local Machine Learning pipeline since the last synchronization. */
  protected var processed_data: Long = 0

  /** The size of the mini batch, or else, the number of distinct data points
    * that are fitted to the Machine Learning pipeline in single fit operation.
    */
  protected var mini_batch_size: Int = 64

  /** The number of mini-batches fitted by the worker before checking
    * if it should push its parameters to the parameter server.
    */
  protected var mini_batches: Int = 4

  /** The local Machine Learning pipeline to train in on streaming data. */
  protected implicit var ml_pipeline: MLPipeline = new MLPipeline()

  /** The global model. */
  protected var global_model: LearningParameters = _

  /** The boundaries used to split the model into pieces. */
  protected var buckets: ListBuffer[Bucket] = _

  /** A TreeMap with the parameter splits. */
  var parameterTree: mutable.TreeMap[(Int, Int), Vector] = _

  /** This is the proxy to the querier. */
  @Inject
  protected var querier: Querier = _

  /**
    * A map of promises that this node has made to the disjoint nodes of the Bipartite Graph.
    */
  private val promises: util.Map[Long, PromiseResponse[Serializable]] = null


  /** The proxies to the parameter servers. */
  @Inject
  protected var parameterServerProxies: java.util.HashMap[Int, T] = _

  /** A broadcast proxy for the parameter servers. */
  @Inject
  protected var parameterServersBroadcastProxy: T = _

  // =================================== Getters ===================================================

  def getProtocol: String = protocol

  def getProcessedData: Long = processed_data

  def getMiniBatchSize: Int = mini_batch_size

  def getMiniBatches: Int = mini_batches

  def getMLPipeline: MLPipeline = ml_pipeline

  def getLearnerParams: Option[LearningParameters] = ml_pipeline.getLearner.getParameters

  def getGlobalModel: LearningParameters = global_model

  def getParameterServerProxies: java.util.HashMap[Int, T] = parameterServerProxies

  def getParameterServersBroadcastProxy: T = parameterServersBroadcastProxy

  // =================================== Setters ===================================================

  def setProtocol(protocol: String): Unit = this.protocol = protocol

  def setProcessedData(processed_data: Long): Unit = this.processed_data = processed_data

  def setMiniBatchSize(mini_batch_size: Int): Unit = this.mini_batch_size = mini_batch_size

  def setMiniBatches(mini_batches: Int): Unit = this.mini_batches = mini_batches

  def setMLPipeline(ml_pipeline: MLPipeline): Unit = this.ml_pipeline = ml_pipeline

  def setLearnerParams(params: LearningParameters): Unit = ml_pipeline.getLearner.setParameters(params)

  def setGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model

  def setDeepGlobalModel(global_model: LearningParameters): Unit = this.global_model = global_model.getCopy

  def setParameterServerProxies(psProxies: java.util.HashMap[Int, T]): Unit = this.parameterServerProxies = psProxies

  def setParameterServersBroadcastProxy(psbProxy: T): Unit = this.parameterServersBroadcastProxy = psbProxy

  // =================================== ML worker basic operations ================================

  /** This method configures an Online Machine Learning worker by using a creation Request. */
  def configureWorker(request: Request): MLWorker[T] = {

    // Setting the ML node parameters
    val config: mutable.Map[String, AnyRef] = request.getTraining_configuration.asScala

    if (config == null) throw new RuntimeException("Empty training configuration map.")

    if (config.contains("mini_batch_size")) {
      try {
        setMiniBatchSize(config("mini_batch_size").asInstanceOf[Int])
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }

    if (config.contains("mini_batches")) {
      try {
        setMiniBatches(config("mini_batches").asInstanceOf[Double].toInt)
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    }

    if (config.contains("protocol")) {
      try {
        setProtocol(config("protocol").asInstanceOf[String])
      } catch {
        case e: Throwable => e.printStackTrace()
      }
    } else setProtocol("Asynchronous")

    // Setting the ML pipeline
    ml_pipeline.configureMLPipeline(request)

    this
  }

  /** Clears the Machine Learning worker. */
  def clear(): MLWorker[T] = {
    processed_data = 0
    mini_batch_size = 64
    mini_batches = 4
    ml_pipeline.clear()
    global_model = null
    this
  }

  /** Initialization method of the ML pileline.
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object
    */
  def init(data: Point): Unit = {
    ml_pipeline.init(data)
  }

  /** A method that returns the delta/shift of the
    * parameters since the last received global model.
    */
  def getDeltaVector: LearningParameters = {
    try {
      getLearnerParams.get - getGlobalModel
    } catch {
      case _: Throwable => getLearnerParams.get
    }
  }

  /** A method for calculating the performance of the local model.
    *
    * @param test_set The test set to calculate the performance on.
    * @return A String representation of the performance of the model.
    */
  def getPerformance(test_set: ListBuffer[Point]): String = {
    ml_pipeline.score(test_set) match {
      case Some(score) => score + ""
      case None => "Can't calculate score"
    }
  }

  /** Converts the model into a Serializable POJO case class to be send over the Network. */
  def ModelMarshalling: Array[ParameterDescriptor] =
    ModelMarshalling(false)

  /** Converts the model into a Serializable POJO case class to be send over the Network. */
  def ModelMarshalling(sparse: Boolean): Array[ParameterDescriptor] = {
    try {
      val delta = getDeltaVector
      val marshaledModel = {
        (for (bucket <- buckets) yield {
          val (sizes, parameters) = delta.generateSerializedParams(delta, sparse, bucket)
          new ParameterDescriptor(sizes, parameters, bucket, processed_data)
        }).toArray
      }
      processed_data = 0
      marshaledModel
    } catch {
      case _: NullPointerException =>
        generateQuantiles()
        ModelMarshalling(sparse)
    }
  }

  /** This method creates the bucket for the splitting of the model. */
  def generateQuantiles(): Unit = {
    require(ml_pipeline.getLearner.getParameters.isDefined)

    val numberOfBuckets: Int = parameterServerProxies.size()
    val bucketSize: Int = ml_pipeline.getLearner.getParameters.get.getSize / numberOfBuckets
    val remainder: Int = ml_pipeline.getLearner.getParameters.get.getSize % numberOfBuckets

    @scala.annotation.tailrec
    def createRanges(index: Int, remainder: Int, quantiles: ListBuffer[Bucket]): ListBuffer[Bucket] = {
      if (index == numberOfBuckets) return quantiles
      if (index == 0)
        quantiles.append(Bucket(0, if (remainder > 0) bucketSize else bucketSize - 1))
      else {
        val previousQ = quantiles(index - 1).getEnd
        quantiles.append(Bucket(previousQ + 1, previousQ + {
          if (remainder > 0) bucketSize + 1 else bucketSize
        }))
      }
      createRanges(index + 1, if (remainder > 0) remainder - 1 else remainder, quantiles)
    }

    buckets = createRanges(0, remainder, ListBuffer[Bucket]())
  }

}
