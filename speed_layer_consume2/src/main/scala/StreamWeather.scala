import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes

object StreamWeather {
  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  val hbaseConf: Configuration = HBaseConfiguration.create()
  hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
  hbaseConf.set("hbase.zookeeper.quorum", "localhost")

  val hbaseConnection = ConnectionFactory.createConnection(hbaseConf)
  val table = hbaseConnection.getTable(TableName.valueOf("elnelson_covid_data"))
  
  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println(s"""
        |Usage: StreamFlights <brokers> 
        |  <brokers> is a list of one or more Kafka brokers
        | 
        """.stripMargin)
      System.exit(1)
    }

    val Array(brokers) = args

    // Create context with 2 second batch interval
    val sparkConf = new SparkConf().setAppName("StreamCases")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    // Create direct kafka stream with brokers and topics
    val topicsSet = Set("elnelson-covid2")
    // Create direct kafka stream with brokers and topics
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> brokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
/*      "fetch.max.bytes" -> "10000",*/
      "fetch.min.bytes" -> "1000",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc, PreferConsistent,
      Subscribe[String, String](topicsSet, kafkaParams)
    )

    val serializedRecords = stream.map(_.value);
    val reports = serializedRecords.map(rec => mapper.readValue(rec, classOf[LiveReport]))

    // How to write to an HBase table
    val batchStats = reports.map(wr => {
      val fipsdate = wr.fips + "-" + wr.date
      println(fipsdate)
      val put = new Put(Bytes.toBytes(fipsdate))
      put.addColumn(Bytes.toBytes("fipsdate"), Bytes.toBytes("day"), Bytes.toBytes(wr.date))
      put.addColumn(Bytes.toBytes("fipsdate"), Bytes.toBytes("county"), Bytes.toBytes(wr.county))
      put.addColumn(Bytes.toBytes("fipsdate"), Bytes.toBytes("state"), Bytes.toBytes(wr.state))
      put.addColumn(Bytes.toBytes("fipsdate"), Bytes.toBytes("cases"), Bytes.toBytes(wr.cases))
      put.addColumn(Bytes.toBytes("fipsdate"), Bytes.toBytes("deaths"), Bytes.toBytes(wr.deaths))
      table.put(put)
    })
    batchStats.print()
    
    // Start the computation
    ssc.start()
    ssc.awaitTermination()
  }

}
