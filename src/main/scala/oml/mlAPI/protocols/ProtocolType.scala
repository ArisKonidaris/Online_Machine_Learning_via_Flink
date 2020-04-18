package oml.mlAPI.protocols

/** The basic trait for the distributed training protocol.
  */
sealed trait ProtocolType extends Enumeration with Serializable

/** The trait for a the static averaging family of distributed training protocols.
  *
  * Averaging Protocols implementing this trait should have static/periodic
  * communication mechanisms. Two examples are the well known Synchronous and
  * Asynchronous distributed training using the parameter server paradigm.
  */
sealed trait StaticAveragingProtocol extends ProtocolType

/** The trait for a dynamic averaging family of distributed training protocols.
  *
  * Averaging Protocols implementing this trait should have non-static/dynamic/
  * adaptive communication mechanisms. An example could be the Dynamic Averaging
  * protocol of vectors.Kamp et al..
  *
  * @see <a href = http://michaelkamp.org/wp-content/uploads/2018/07/commEffDeepLearning_extended.pdf>
  *      Efficient Decentralized Deep Learning by Dynamic Model Averaging</a>
  */
sealed trait DynamicAveragingProtocol extends ProtocolType

case object AsynchronousAveraging extends StaticAveragingProtocol

case object SynchronousAveraging extends StaticAveragingProtocol

case object DynamicAveraging extends DynamicAveragingProtocol

case object FGMAveraging extends DynamicAveragingProtocol