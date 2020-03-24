package oml.FlinkBipartiteAPI.utils

object DefaultJobParameters {
  val defaultJobName: String = "OML_job_1"
  val defaultParallelism: String = "36"
  val defaultInputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/lin_class_mil_e10.txt"
  val defaultOutputFile: String = "hdfs://clu01.softnet.tuc.gr:8020/user/vkonidaris/output"
}
