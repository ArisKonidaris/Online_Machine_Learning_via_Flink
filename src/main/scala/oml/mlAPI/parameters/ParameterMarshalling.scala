package oml.mlAPI.parameters

class ParameterMarshalling(protected var marshal: (LearningParameters, Boolean, Range) => ParameterDescriptor,
                           protected var unmarshal: ParameterDescriptor => LearningParameters) {

  def this() = this(_, _)

  def getMarshal: (LearningParameters, Boolean, Range) => ParameterDescriptor = marshal

  def getUnmarshal: ParameterDescriptor => LearningParameters = unmarshal

  def setMarshal(marshal: (LearningParameters, Boolean, Range) => ParameterDescriptor): Unit = this.marshal = marshal

  def setUnmarshal(unmarshal: ParameterDescriptor => LearningParameters): Unit = this.unmarshal = unmarshal

}
