package oml.mlAPI.mlworkers.worker

import oml.FlinkBipartiteAPI.POJOs.QueryResponse
import oml.StarTopologyAPI.annotations.{InitOp, QueryOp, ProcessOp}
import oml.mlAPI.math.{DenseVector, Point, SparseVector}
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlworkers.interfaces.{MLWorkerRemote, Querier}
import oml.mlAPI.parameters.ParameterDescriptor

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

case class MLPeriodicWorker() extends MLWorker[PullPush, Querier] with MLWorkerRemote {

  /** Initialization method of the Machine Learning worker node. */
  @InitOp
  def init(): Unit = {
    if (getNodeId != 0) pull()
  }

  /**
    * The consumption of a data point by the Machine Learning worker.
    *
    * @param data A data point to be fitted to the model.
    */
  @ProcessOp
  def receiveTuple(data: Point): Unit = {
    ml_pipeline.fit(data)
    processed_data += 1
    if (processed_data >= mini_batch_size * mini_batches) push()
  }

  /** A method called each type the new global model
    * (or a slice of it) arrives from the parameter server.
    */
  override def updateModel(mDesc: ParameterDescriptor): Unit = {
    if (getNumberOfHubs == 1) {
      global_model = ml_pipeline.getLearner.generateParameters(mDesc)
      ml_pipeline.getLearner.setParameters(global_model.getCopy)
      ml_pipeline.setFittedData(mDesc.getFitted)
      processed_data = 0
    } else {
      parameterTree.put((mDesc.getBucket.getStart.toInt, mDesc.getBucket.getEnd.toInt), mDesc.getParams)
      if (ml_pipeline.getFittedData < mDesc.getFitted) ml_pipeline.setFittedData(mDesc.getFitted)
      if (parameterTree.size == getNumberOfHubs) {
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
        processed_data = 0
      }
    }
  }



  /** This method responds to a query for the Machine Learning worker.
    *
    * @param test_set The test set that the predictive performance of the model should be calculated on.
    */
  @QueryOp
  def query(queryId: Long, queryTarget: Int, test_set: Array[java.io.Serializable]): Unit = {
    val pj = ml_pipeline.generatePOJO(ListBuffer(test_set: _ *).asInstanceOf[ListBuffer[Point]])
    getQuerier.sendQueryResponse(new QueryResponse(queryId, queryTarget, pj._1.asJava, pj._2, protocol, pj._3, pj._4))
  }

  def push(): Unit = {
    for ((slice: ParameterDescriptor, index: Int) <- ModelMarshalling.zipWithIndex)
      getProxy(index).pushModel(slice).toSync(updateModel)
  }

  def pull(): Unit = {
    for (i <- 0 until getNumberOfHubs)
      getProxy(i).pullModel.toSync(updateModel)
  }

}
