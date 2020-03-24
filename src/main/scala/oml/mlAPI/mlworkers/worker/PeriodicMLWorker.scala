package oml.mlAPI.mlworkers.worker

import oml.StarTopologyAPI.annotations.{MergeOp, ReceiveTuple}
import oml.mlAPI.math.Point
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.parameters.{LearningParameters, ParameterDescriptor}

case class PeriodicMLWorker() extends MLWorker[PullPush] with MLWorkerRemote {

  /** Initialize the worker.
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object.
    */
  def init(data: Point): Unit = {
    parameterServersBroadcastProxy.pullModel.
  }

  /**
    * The consumption of a data point by the Machine Learning worker.
    *
    * @param data A data point to be fitted to the model.
    */
  @ReceiveTuple
  def receiveTuple(data: Point): Unit = {
    ml_pipeline.fit(data)
    processed_data += 1
    if (processed_data >= mini_batch_size * mini_batches) {
      val deltaVector = getDeltaVector
      deltaVector.set_fitted(processed_data)
      parameterServersBroadcastProxy.pushModel(deltaVector.toSparseVector).to(receiveGlobalModel)
      setProcessedData(0)
    }
  }

  /** A method called when merging two ML workers.
    *
    * @param worker The ML worker to merge this one with.
    * @return An [[PeriodicMLWorker]] object
    */
  @MergeOp
  def merge(worker: PeriodicMLWorker): PeriodicMLWorker = {
    setProcessedData(0)
    setMiniBatchSize(worker.getMiniBatchSize)
    setMiniBatches(worker.getMiniBatches)
    setMLPipeline(ml_pipeline.merge(worker.getMLPipeline))
    setGlobalModel(worker.getGlobalModel)
    this
  }

  /** A method called each type the new global
    * model arrives from the parameter server.
    */
  override def receiveGlobalModel(model: ParameterDescriptor): Unit = updateModel({

    val modelClass: Class[_] = Class.forName(model.getParamClass)

    ml_pipeline.getLearner.getParameters match {
      case Some(localModel) =>
        require(localModel.getClass.equals(modelClass), s"Parameters do not match. The local model is of type" +
          s" ${localModel.getClass}, but the received model is of type $modelClass")
        val mdl: LearningParameters = marshal.getUnmarshal(model)
        return
      case None =>
    }

    ml_pipeline.getLearner.getParameters.get
  })

}
