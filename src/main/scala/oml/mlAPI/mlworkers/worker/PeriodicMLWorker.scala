package oml.mlAPI.mlworkers.worker

import oml.math.Point
import oml.message.workerMessage
import oml.mlAPI.mlworkers.MLWorkerRemote
import oml.parameters.LearningParameters

import scala.collection.mutable.ListBuffer

case class PeriodicMLWorker() extends MLWorker with MLWorkerRemote {

  /** Initialization method of the ML worker
    *
    * @param data A data point for the initialization to be based on.
    * @return An [[MLWorker]] object
    */
  override def init(data: Point): Unit = {
    setProcessData(true)
    ml_pipeline.init(data)
    if (ID.split("_")(0).toInt == 0) setProcessData(true)
  }

  /** A method called each type the new global
    * model arrives from the parameter server.
    */
  override def updateModel(model: LearningParameters): Unit = {
    setGlobalModel(model)
    setLearnerParams(global_model.getCopy)
    setProcessedData(0)
    setProcessData(true)
    fitFromBuffer()
  }

  /** The consumption of a data point by the ML worker.
    *
    * @param data A data point to be fitted to the ML pipeline
    */
  def receiveTuple(data: Point): Unit = {
    if (process_data && training_set.isEmpty) {
      ml_pipeline.fit(data)
      processed_data += 1
    } else {
      training_set.append(data)
    }
    fitFromBuffer()
  }

  /** Train the ML Pipeline from the data point buffer
    */
  private def fitFromBuffer(): Unit = {
    if (merged) {
      pull()
      training_set.completeMerge()
      setMerged(false)
    } else if (process_data) {
      val batch_size: Int = mini_batch_size * mini_batches
      while (processed_data < batch_size && training_set.nonEmpty) {
        val batch_len: Int = Math.min(batch_size - processed_data, training_set.length)
        ml_pipeline.fit(training_set.getDataSet.slice(0, batch_len))
        training_set.getDataSet.remove(0, batch_len)
        processed_data += batch_len
      }
      if (processed_data >= mini_batch_size * mini_batches) {
        setProcessData(false)
        messageQueue.enqueue(workerMessage(nodeID = ID.split("_")(1).toInt,
          workerId = ID.split("_")(0).toInt,
          parameters = getDeltaVector,
          request = 1))
      }
    }
  }

  /** A verbose calculation of the score of the ML pipeline.
    *
    * @param test_set The test set that the score should be calculated on.
    * @return A human readable text for observing the training of the ML method.
    */
  override def score(test_set: ListBuffer[Point]): Unit = {
    println(s"$ID, ${
      ml_pipeline.score(test_set) match {
        case Some(score) => score
        case None => "Can't calculate score"
      }
    }, ${training_set.length}, ${test_set.length}")
  }

}
