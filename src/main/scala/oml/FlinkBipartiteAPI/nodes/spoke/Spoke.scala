package oml.FlinkBipartiteAPI.nodes.spoke

import org.apache.flink.api.common.functions.Function

/**
  * The basic trait for a remote spoke request a Star Network topology
  * A remote spoke must have a method that defines its flink_worker_id.
  * This flink_worker_id is used by Flink for correctly partitioning the Hub's
  * stream to the appropriate remote spoke.
  *
  */
trait Spoke extends Function with Serializable
