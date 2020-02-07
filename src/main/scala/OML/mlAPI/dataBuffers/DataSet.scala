package OML.mlAPI.dataBuffers

import scala.collection.mutable.ListBuffer

trait DataSet[T] extends Serializable {

  /** The data set structure. */
  var data_set: ListBuffer[T]

  /** The capacity of the data set data structure.
    * This is done to prevent potential overflows. */
  var max_size: Int

  /** The number of times this worker has been merged with other ones */
  var merges: Int

  /** Setters */
  def getDataSet: ListBuffer[T] = data_set

  def getMaxSize: Int = max_size

  def getMerges: Int = merges

  /** Getters */
  def setDataSet(data_set: ListBuffer[T]): Unit = this.data_set = data_set

  def setMaxSize(max_size: Int): Unit = this.max_size = max_size

  def setMerges(merges: Int): Unit = this.merges = merges

  /** A method that returns true if the data set is empty */
  def isEmpty: Boolean = data_set.isEmpty

  /** A method that returns true if the data set is non empty */
  def nonEmpty: Boolean = data_set.nonEmpty

  /** A method that signals the end of the merging procedure of DadaSet objects */
  def completeMerge(): Unit = merges = 0

  /** If the data set becomes too large, the oldest
    * data point is discarded to prevent memory overhead.
    */
  def overflowCheck(): Unit

  /** Append a data point to the data set */
  def append(data: T): Unit

  /** Insert a data point into the data set */
  def insert(index: Int, data: T): Unit

  /** The length of the data set */
  def length: Int = data_set.length

  /** Clears the data set */
  def clear(): Unit

  def merge(dataSet: DataSet[T]): DataSet[T]

}
