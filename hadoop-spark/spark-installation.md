# 安裝 Spark

## Spark Cluster 架構圖

![](http://spark.apache.org/docs/latest/img/cluster-overview.png)

- DriverProgram: 程式設計寫的 spark code，程式中定義 spark context 作為應用程式的入口
- SparkContext: 透過 Cluster Manager 管理 Worker Nodes，每個 Work Node 執行計算任務

Cluster Manager 可執行的模式
- Local machine: 程式只要 import Spark 程式庫就能只在本機執行
- Spark standalone cluster: 由 Spark 提供的 cluster 管理模式，執行平行運算，直接存取 Local Disk 或 HDFS
- Hadoop YARN: Hadoop 2.0 新架構中更高效的資源管理核心。Spark 在 YARN 上執行，讓 YARN 幫他進行多台機器的資源管理
- Cloud: 針對更大規模的計算

> YARN: Yet Another Resource Negotiator

## Scala 安裝

Scala 特性:
- 可編譯成 Java bytecode 在 JVM 上執行
- 可使用 Java library
- 函數式語言
- 物件導向語言

```shell
$ wget http://www.scala-lang.org/files/archive/scala-2.11.8.tgz
$ tar zxf scala-2.11.8.tgz
$ sudo mv scala-2.11.8 /usr/local/scala
$ vi ~/.bashrc
$ source ~/.bashrc
$ scala -version
```

.bashrc 加入以下內容
```
# scala variables
export SCALA_HOME=/usr/local/scala
export PATH=$PATH:$SCALA_HOME/bin
```

## Spark 安裝
```shell
$ wget http://apache.fayea.com/spark/spark-2.0.0/spark-2.0.0-bin-hadoop2.6.tgz
$ tar zxf spark-2.0.0-bin-hadoop2.6.tgz
$ sudo mv spark-2.0.0-bin-hadoop2.6 /usr/local/spark
$ vi ~/.bashrc
$ source ~/.bashrc
```

.bashrc 加入以下內容
```
# spark variables
export SPARK_HOME=/usr/local/spark
export PATH=$PATH:$SPARK_HOME/bin
```

## 設定 spark-shell 顯示訊息
```shell
$ cd /usr/loca/spark/conf
$ cp log4j.properties.template log4j.properties
$ vi log4j.properties
```

log4j.properties:
```
#log4j.rootCategory=INFO, console
log4j.rootCategory=WARN, console
```

## 啟動 Hadoop
```shell
$ start-all.sh
```

## spark-shell (Local machine)

### 啟動 spark-shell
```shell
$ spark-shell --master local[4]　# 本機執行，N個執行緒
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.0.0
      /_/

Using Scala version 2.11.8 (OpenJDK 64-Bit Server VM, Java 1.7.0_101)
Type in expressions to have them evaluated.
Type :help for more information.
```
> 注意：這版 Spark-2.0.0 使用 Scala-2.11.8，之後使用 sbt 編譯程式時 scalaVersion 要設定為 2.11.8。

### 讀取本機檔案
```
scala> val testFile=sc.textFile("file:/usr/local/spark/README.md")
testFile: org.apache.spark.rdd.RDD[String] = MapPartitionsRDD[1] at textFile at <console>:21
```

### 讀取 HDFS 檔案
```
scala> val textFile=sc.textFile("hdfs://master:9000/user/hduser/test/README.txt")
textFile: org.apache.spark.rdd.RDD[String] = MapPartitionsRDD[3] at textFile at <console>:21
```
> `master:9000` 設定在 core-site.xml 中

## spark-shell (Hadoop YARN)

### 啟動 spark-shell
```shell
$ SPARK_JAR=/usr/local/spark/lib/spark-assembly-1.4.0-hadoop2.6.0.jar HADOOP_CONF_DIR=/usr/local/hadoop/etc/hadoop/ MASTER=yarn-client /usr/local/spark/bin/spark-shell
```
- `SPARK_JAR=...` spark jar path
- `HADOOP_CONF_DIR=...` hadoop config path
- `MASTER=...` yarn client
- `/usr/local/spark/bin/spark-shell` spark client full path

## 建置 Spark standalone cluster 執行環境
> 我的機器不夠力，不能跑四個 VM，實驗略過

## spark-shell (Spark standalone)
> 我的機器不夠力，不能跑四個 VM，實驗略過
