package oml.mlAPI

import oml.POJOs.Prediction
import oml.StarTopologyAPI.RemoteOp

trait Querier {

  @RemoteOp(1)
  def sendPrediction(prediction: Prediction): Unit

}
