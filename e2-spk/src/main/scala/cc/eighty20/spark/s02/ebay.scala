package cc.eighty20.spark.s02

import org.apache.spark.sql.SparkSession

object ebay00 {
  case class Auction(auctionid: String, bid: Float, bidtime: Float, bidder: String, bidderrate: Integer, openbid: Float, price: Float, item: String, daystolive: Integer)

  def main(args: Array[String]) {
    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val ebayText = spark.sparkContext.textFile("data/ebay.csv")
    val auction = ebayText.map(_.split(",")).map(p => Auction(p(0), p(1).toFloat, p(2).toFloat, p(3), p(4).toInt, p(5).toFloat, p(6).toFloat, p(7), p(8).toInt)).toDF()
    auction.createOrReplaceTempView("auction")

    // how many auctions?
    val cnt1 = auction.select("auctionid").distinct.count
    val cnt2 = spark.sql("SELECT DISTINCT auctionid FROM auction").count
    println(cnt1, cnt2)

    // how many bids for each auction?
    auction.select("auctionid", "item", "bid").groupBy("auctionid", "item").count.show
    spark.sql("SELECT auctionid, item, count(bid) as bid_count FROM auction GROUP BY auctionid, item").show

    // the statistics (min/max/mean) of each auction
    spark.sql("SELECT auctionid, MAX(price) as price_max, MIN(price) as price_min, AVG(price) as price_avg FROM auction GROUP BY item, auctionid").show

    // find out the bid whose price is more than $100
    spark.sql("SELECT auctionid, bid, price FROM auction WHERE price > 100").show
  }
}
