package OML.utils.parsers

import org.apache.flink.api.common.functions.Function

/**
  * Basic trait for the Parsing Module of the Interactive Online Machine Learning Job
  */
trait MLParser extends Function with Serializable
