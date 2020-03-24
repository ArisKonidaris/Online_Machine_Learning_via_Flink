package oml.FlinkBipartiteAPI.nodes.site

import org.apache.flink.api.common.functions.Function

/**
  * The basic trait for a remote site request a Star Network topology
  * A remote site must have a method that defines its flink_worker_id.
  * This flink_worker_id is used by Flink for correctly partitioning the Coordinator's
  * stream to the appropriate remote site.
  *
  */
trait Site extends Function with Serializable
