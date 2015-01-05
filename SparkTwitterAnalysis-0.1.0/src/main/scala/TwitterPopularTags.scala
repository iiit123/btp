import org.apache.spark.streaming.{Seconds, StreamingContext}
import StreamingContext._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming.twitter._

/**
 * Calculates popular hashtags (topics) over sliding 10 and 60 second windows from a Twitter
 * stream. The stream is instantiated with credentials and optionally filters supplied by the
 * command line arguments.
 *
 */
object TwitterPopularTags {

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println("Usage: sbt 'run <master> " + "consumerKey consumerSecret accessToken accessTokenSecret" +
        " [filter1] [filter2] ... [filter n]"+ "'")
      System.exit(1)
    }

    val (master, filters) = (args(0), args.slice(5, args.length))

    // Twitter Authentication credentials
    System.setProperty("twitter4j.oauth.consumerKey", "cL1Ip58GsRQahZlm3xpUDf7Vs")
    System.setProperty("twitter4j.oauth.consumerSecret","k38TXGzxjcmKBYWflhxYfwl6qG4IX2ukhAu5BlL6e275dZkbGe")
    System.setProperty("twitter4j.oauth.accessToken", "359883428-VrmQQftXxXsI2AVmtrhBfzXYIXVN74z8LYGIbSld")
    System.setProperty("twitter4j.oauth.accessTokenSecret", "n0LQ2zTKGgxpR93PkoLHxUm93MLXKKZgs1L2z3vzeTWPv")

    

    val ssc = new StreamingContext(master, "TwitterPopularTags", Seconds(2),
      System.getenv("SPARK_HOME"), StreamingContext.jarOfClass(this.getClass))
    val stream = TwitterUtils.createStream(ssc, None, filters)

    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))

    val topCounts60 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(60))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))

    val topCounts10 = hashTags.map((_, 1)).reduceByKeyAndWindow(_ + _, Seconds(10))
                     .map{case (topic, count) => (count, topic)}
                     .transform(_.sortByKey(false))


    // Print popular hashtags
    topCounts60.foreachRDD(rdd => {
      val topList = rdd.take(5)
      println("\nPopular topics in last 60 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    topCounts10.foreachRDD(rdd => {
      val topList = rdd.take(5)
      println("\nPopular topics in last 10 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (count, tag) => println("%s (%s tweets)".format(tag, count))}
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
