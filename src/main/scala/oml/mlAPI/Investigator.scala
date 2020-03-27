package oml.mlAPI

import oml.POJOs.Prediction
import oml.StarTopologyAPI.RemoteOp

trait Investigator {

  @RemoteOp(1)
  def sendPrediction(prediction: Prediction): Unit

}
