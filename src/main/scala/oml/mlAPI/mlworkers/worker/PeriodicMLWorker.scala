package oml.mlAPI.mlworkers.worker

import oml.StarTopologyAPI.annotations.{MergeOp, ReceiveTuple}
import oml.math.Point
import oml.mlAPI.mlParameterServers.ParamServer
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.math.Vector

case class PeriodicMLWorker() extends MLWorker[ParamServer] with MLWorkerRemote {

  /** The consumption of a data point by the ML worker.
    *
    * @param data A data point to be fitted to the ML pipeline
    */
  @ReceiveTuple
  def receiveTuple(data: Point): Unit = {
    ml_pipeline.fit(data)
    processed_data += 1
    if (processed_data >= mini_batch_size * mini_batches) {
      val deltaVector = getDeltaVector
      deltaVector.set_fitted(processed_data.asInstanceOf[Long])
      parameterServersBroadcastProxy.pushModel(deltaVector.toSparseVector).to(receiveGlobalModel)
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
  override def receiveGlobalModel(model: Vector): Unit = updateModel({
    val model_class = global_model.getClass
    model.asInstanceOf[model_class]
  })

}
