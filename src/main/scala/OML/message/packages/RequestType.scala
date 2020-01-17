package OML.message.packages

/**
  * The basic trait for the request type of a control message.
  */
trait RequestType extends Serializable

/**
  * A trait indicating a user's request.
  */
trait UserRequest extends RequestType

/**
  * A trait indicating a Parameter Server's request.
  */
trait PSRequest extends RequestType

case object CreatePipeline extends UserRequest

case object UpdatePipeline extends UserRequest

case object DeletePipeline extends UserRequest

case object UpdatePipelinePS extends PSRequest
