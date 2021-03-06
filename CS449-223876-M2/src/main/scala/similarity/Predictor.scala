package similarity

import org.rogach.scallop._
import org.json4s.jackson.Serialization
import org.apache.spark.rdd.RDD

import org.apache.spark.sql.SparkSession
import org.apache.log4j.Logger
import org.apache.log4j.Level

//class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
//  val train = opt[String](required = true)
//  val test = opt[String](required = true)
//  val json = opt[String]()
//  verify()
//}

import mileStone2Functions._

object Predictor extends App {
  // Remove these lines if encountering/debugging Spark
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)
  val spark = SparkSession.builder()
    .master("local[1]")
    .getOrCreate()
  spark.sparkContext.setLogLevel("ERROR")

  println("")
  println("******************************************************")

  var conf = new Conf(args)
//  println("Loading training data from: " + conf.train())
//  val trainFile = spark.sparkContext.textFile(conf.train())
//  val train = trainFile.map(l => {
//      val cols = l.split("\t").map(_.trim)
//      Rating(cols(0).toInt, cols(1).toInt, cols(2).toDouble)
//  })
//  assert(train.count == 80000, "Invalid training data")
//
//  println("Loading test data from: " + conf.test())
//  val testFile = spark.sparkContext.textFile(conf.test())
//  val test = testFile.map(l => {
//      val cols = l.split("\t").map(_.trim)
//      Rating(cols(0).toInt, cols(1).toInt, cols(2).toDouble)
//  })
//  assert(test.count == 20000, "Invalid test data")

//  val (train,test) = loadData(conf,spark)

  // Save answers as JSON
  def printToFile(content: String,
                  location: String = "./answers.json") =
    Some(new java.io.PrintWriter(location)).foreach{
      f => try{
        f.write(content)
      } finally{ f.close }
  }

  val maeBaseline = 0.7669

  println("*** Start computing ***")
  val begin = System.nanoTime()
  println( "*** Compute Cosine Similarity ***" )
  val exercise = exercise2(conf,spark)
  println("*** Elapsed Time ***")
  print((System.nanoTime()-begin)*1E-9)
  println(" s")

  conf.json.toOption match {
    case None => ;
    case Some(jsonFile) => {
      var json = "";
      {
        // Limiting the scope of implicit formats with {}
        implicit val formats = org.json4s.DefaultFormats
        val answers: Map[String, Any] = Map(
          "Q2.3.1" -> Map(
            "CosineBasedMae" -> exercise.maeCosineSim, // Datatype of answer: Double
            "CosineMinusBaselineDifference" -> round(exercise.maeCosineSim - maeBaseline,4) // Datatype of answer: Double
          ),

          "Q2.3.2" -> Map(
            "JaccardMae" -> exercise.maeJaccardSim, // Datatype of answer: Double
            "JaccardMinusCosineDifference" -> round(exercise.maeJaccardSim - exercise.maeCosineSim ,4) // Datatype of answer: Double
          ),

          "Q2.3.3" -> Map(
            // Provide the formula that computes the number of similarity computations
            // as a function of U in the report.
            "NumberOfSimilarityComputationsForU1BaseDataset" -> exercise.similarityMetrics.SuvComputation // Datatype of answer: Int
          ),

          "Q2.3.4" -> Map(
            "CosineSimilarityStatistics" -> Map(
              "min" -> exercise.similarityMetrics.similarityStatistics.min,  // Datatype of answer: Double
              "max" -> exercise.similarityMetrics.similarityStatistics.max, // Datatype of answer: Double
              "average" -> exercise.similarityMetrics.similarityStatistics.avg, // Datatype of answer: Double
              "stddev" -> exercise.similarityMetrics.similarityStatistics.std, // Datatype of answer: Double
            )
          ),

          "Q2.3.5" -> Map(
            // Provide the formula that computes the amount of memory for storing all S(u,v)
            // as a function of U in the report.
            "TotalBytesToStoreNonZeroSimilarityComputationsForU1BaseDataset" ->

            exercise.similarityMetrics.memory.nonZero// Datatype of answer: Int
          ),

          "Q2.3.6" -> Map(
            "DurationInMicrosecForComputingPredictions" -> Map(
              "min" -> exercise.predTimeStat.min,  // Datatype of answer: Double
              "max" -> exercise.predTimeStat.max, // Datatype of answer: Double
              "average" -> exercise.predTimeStat.avg, // Datatype of answer: Double
              "stddev" -> exercise.predTimeStat.std // Datatype of answer: Double
            )
            // Discuss about the time difference between the similarity method and the methods
            // from milestone 1 in the report.
          ),

          "Q2.3.7" -> Map(
            "DurationInMicrosecForComputingSimilarities" -> Map(
              "min" -> exercise.suvTimeStat.min,  // Datatype of answer: Double
              "max" -> exercise.suvTimeStat.max, // Datatype of answer: Double
              "average" -> exercise.suvTimeStat.avg, // Datatype of answer: Double
              "stddev" -> exercise.suvTimeStat.std // Datatype of answer: Double

            ),
            "AverageTimeInMicrosecPerSuv" -> exercise.suvTimeStat.avg /exercise.similarityMetrics.size, // Datatype of answer: Double
            "RatioBetweenTimeToComputeSimilarityOverTimeToPredict" -> exercise.suvTimeStat.avg
            / exercise.predTimeStat.avg // Datatype of answer: Double
          )
         )
        json = Serialization.writePretty(answers)
      }

      println(json)
      println("Saving answers in: " + jsonFile)
      printToFile(json, jsonFile)
    }
  }

  println("")
  spark.close()
}
