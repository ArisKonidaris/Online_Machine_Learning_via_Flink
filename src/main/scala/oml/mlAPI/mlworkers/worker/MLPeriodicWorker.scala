package oml.mlAPI.mlworkers.worker

import oml.FlinkBipartiteAPI.POJOs.QueryResponse
import oml.StarTopologyAPI.annotations.{MergeOp, QueryOp, ReceiveTuple}
import oml.mlAPI.math.{DenseVector, Point, SparseVector}
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlworkers.interfaces.MLWorkerRemote
import oml.mlAPI.parameters.ParameterDescriptor

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

case class MLPeriodicWorker() extends MLWorker[PullPush] with MLWorkerRemote {

  /**
    * The consumption of a data point by the Machine Learning worker.
    *
    * @param data A data point to be fitted to the model.
    */
  @ReceiveTuple
  def receiveTuple(data: Point): Unit = {
    ml_pipeline.fit(data)
    processed_data += 1
    if (processed_data >= mini_batch_size * mini_batches)
      for ((ps, slice) <- parameterServerProxies.asScala zip ModelMarshalling)
        ps._2.pushModel(slice).toSync(updateModel)
  }

  /** A method called each type the new global model
    * (or a slice of it) arrives from the parameter server.
    */
  override def updateModel(mDesc: ParameterDescriptor): Unit = {
    if (parameterServerProxies.size() == 1) {
      global_model = ml_pipeline.getLearner.generateParameters(mDesc)
      ml_pipeline.getLearner.setParameters(global_model.getCopy)
      ml_pipeline.setFittedData(mDesc.getFitted)
    } else {
      parameterTree.put((mDesc.getBucket.getStart.toInt, mDesc.getBucket.getEnd.toInt), mDesc.getParams)
      if (ml_pipeline.getFittedData < mDesc.getFitted) ml_pipeline.setFittedData(mDesc.getFitted)
      if (parameterTree.size == parameterServerProxies.size()) {
        mDesc.setParams(
          DenseVector(
            parameterTree.values
              .map(
                {
                  case dense: DenseVector => dense
                  case sparse: SparseVector => sparse.toDenseVector
                })
              .fold(Array[Double]())(
                (accum, vector) => accum.asInstanceOf[Array[Double]] ++ vector.asInstanceOf[DenseVector].data)
              .asInstanceOf[Array[Double]]
          )
        )
        global_model = ml_pipeline.getLearner.generateParameters(mDesc)
        ml_pipeline.getLearner.setParameters(global_model.getCopy)
      }
    }
  }

  /** A method called when merging two ML workers.
    *
    * @param worker The ML worker to merge this one with.
    * @return An [[MLPeriodicWorker]] object
    */
  @MergeOp
  def merge(worker: MLPeriodicWorker): MLPeriodicWorker = {
    setProcessedData(0)
    setMiniBatchSize(worker.getMiniBatchSize)
    setMiniBatches(worker.getMiniBatches)
    setMLPipeline(ml_pipeline.merge(worker.getMLPipeline))
    setGlobalModel(worker.getGlobalModel)
    this
  }

  /** This method responds to a query for the ML pipeline.
    *
    * @param test_set The test set that the predictive performance of the model should be calculated on.
    * @return A human readable text for observing the training of the ML method.
    */
  @QueryOp
  def query(queryId: Long, queryTarget: Int, test_set: Array[java.io.Serializable]): Unit = {
    val pj = ml_pipeline.generatePOJO(ListBuffer(test_set: _ *).asInstanceOf[ListBuffer[Point]])
    querier.sendQueryResponse(new QueryResponse(queryId, queryTarget, pj._1.asJava, pj._2, protocol, pj._3, pj._4))
  }

}
