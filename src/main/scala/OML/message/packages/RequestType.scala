package OML.message.packages

/**
  * The basic trait for the request type of a control message.
  */
sealed trait RequestType extends Enumeration with Serializable

/**
  * A trait indicating a user's request.
  */
sealed trait UserRequest extends RequestType

/**
  * A trait indicating a Parameter Server's request.
  */
sealed trait PSRequest extends RequestType

case object CreatePipeline extends UserRequest

case object UpdatePipeline extends UserRequest

case object DeletePipeline extends UserRequest

case object UpdatePipelinePS extends PSRequest
