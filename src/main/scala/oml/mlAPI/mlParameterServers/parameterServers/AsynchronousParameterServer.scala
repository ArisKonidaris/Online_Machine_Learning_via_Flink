package oml.mlAPI.mlParameterServers.parameterServers

import oml.StarTopologyAPI.annotations.{InitOp, MergeOp, QueryOp, ProcessOp}
import oml.StarTopologyAPI.futures.{PromiseResponse, Response}
import oml.mlAPI.math.DenseVector
import oml.mlAPI.mlParameterServers.PullPush
import oml.mlAPI.mlworkers.interfaces.{MLWorkerRemote, Querier}
import oml.mlAPI.parameters.ParameterDescriptor
import breeze.linalg.{DenseVector => BreezeDenseVector}

class AsynchronousParameterServer extends MLParameterServer[MLWorkerRemote, Querier] with PullPush {

  var parameters: BreezeDenseVector[Double] = _

  var promises: Long = _

  /** Initialization method of the Parameter server node. */
  @InitOp
  def init(): Unit = promises = 0L

  /**
    * The consumption method of user messages. Right know this is an empty method.
    *
    * @param data A data tuple for the Parameter Server.
    */
  @ProcessOp
  def receiveTuple[T <: Serializable](data: T): Unit = {

  }

  /** A method called when merging multiple Parameter Servers. Right know this is an empty method.
    *
    * @param parameterServers The parameter servers to merge this one with.
    * @return An array of [[AsynchronousParameterServer]] instances.
    */
  @MergeOp
  def merge(parameterServers: Array[AsynchronousParameterServer]): AsynchronousParameterServer = {
    this
  }

  /** This method responds to a query for the Parameter Server. Right know this is an empty method.
    *
    * @param predicates Any predicate that is necessary for the calculation of the query.
    */
  @QueryOp
  def query(queryId: Long, queryTarget: Int, predicates: Array[java.io.Serializable]): Unit = {
  }

  override def pullModel: Response[ParameterDescriptor] = {
    if (parameters == null) {
      promises += 1
      val promise = new PromiseResponse[ParameterDescriptor]()
      makePromise(promises, promise)
      promise
    } else sendModel()
  }

  override def pushModel(modelDescriptor: ParameterDescriptor): Response[ParameterDescriptor] = {
    updateGlobalState(modelDescriptor)
    sendModel()
  }


  def updateGlobalState(remoteModelDescriptor: ParameterDescriptor): Unit = {
    val remoteVector = BreezeDenseVector(remoteModelDescriptor.getParams.asInstanceOf[DenseVector].data)
    incrementNumberOfFittedData(remoteModelDescriptor.getFitted)
    try {
      parameters += (remoteVector * (1.0 / (1.0 * getNumberOfSpokes)))
    } catch {
      case _: Throwable =>
        parameters = remoteVector
        parametersDescription = remoteModelDescriptor
        checkForPromises()
    }
  }

  def sendModel(): Response[ParameterDescriptor] = {
    Response.of(
      parametersDescription.copy(params = DenseVector.denseVectorConverter.convert(parameters), fitted = fitted)
    )
  }

  def checkForPromises(): Unit = {
    if (promises > 0)
      while (promises > 0) {
        fulfillPromise(promises, sendModel())
        promises -= 1
      }
  }

}
