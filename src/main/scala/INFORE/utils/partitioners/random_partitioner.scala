package INFORE.utils.partitioners

import org.apache.flink.api.common.functions.Partitioner

object random_partitioner extends Partitioner[Int] {

  override def partition(key: Int, numPartitions: Int): Int = {
    require((key >= 0) && (key < numPartitions))

    val id: Int = key % numPartitions
    val worker_id: Int = if (id < 0) id + numPartitions else id

    assert(key == worker_id)
    worker_id
  }
}
