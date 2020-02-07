package OML.nodes.site

import org.apache.flink.api.common.functions.Function

/**
  * The basic trait for a remote site in a Star Network topology
  * A remote site must have a method that defines its id.
  * This id is used by Flink for correctly partitioning the Coordinator's
  * stream to the appropriate remote site.
  *
  */
trait Site extends Function with Serializable {
  def setSiteID(siteID: Int): Unit
}
