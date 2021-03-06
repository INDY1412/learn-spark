# 建構基於 Spark 的推薦引擎

## 推薦引擎

推薦引擎背後的想法是預測人們可能喜歡的物品，並通過探詢物品之間的關聯關係來輔助這個過程。
- 基於用戶的推薦：告訴用戶有「哪些電影他們可能會喜歡」
- 基於商品的推薦：告訴用戶有「哪些電影跟某一電影類似」

用戶/電影 | 電影A | 電影B | 電影C | 電影D
----------|-------|-------|-------|-------
用戶A     | V     |       | V     | V
用戶B     | V     |       | V     | 
用戶C     | V     | V     |       |

### 基於用戶的推薦
- 用戶A 看過 電影A, C, D
- 用戶B 看過 電影A, C
- 把用戶A, B 當作相似的
- 推薦電影D 給用戶A

用戶/電影 | 電影A | 電影B | 電影C | 電影D
----------|-------|-------|-------|-------
用戶A     | V     |       | V     | V
用戶B     | V     |       | V     | 推薦

如果兩個用戶表現出相似的偏好（對相同物品的偏好大致相同），就認為他們的興趣類似。要對其中一個用戶推薦一個未知物品，可選取若干與其類似的用戶，根據他們的喜好計算出各個物品的得分，再以得分推薦物品。

### 基於商品的推薦
- 用戶A 看過電影A，也看了電影C
- 用戶B 看過電影A，也看了電影C
- 推測電影A, C 有關聯性
- 用戶C 看過電影A，還沒看過電影C
- 推薦電影C 給用戶C

用戶/電影 | 電影A | 電影B | 電影C | 電影D
----------|-------|-------|-------|-------
用戶A     | V     |       | V     | V
用戶B     | V     |       | V     | 
用戶C     | V     | V     | 推薦  |

相似用戶評級相同的那些物品被認為更相近，藉此得到物品之間的相似度。使用用戶接觸過的物品表示這個用戶，然後找出那些與已知物品相似的其他物品，將這些物品推薦給用戶。

### 推薦引擎常見場景
- 偏個人喜好 - 人們根據個人喜好選擇物品。利用群體智慧，根據其他類似喜好用戶的訊息幫助用戶發現所需物品。
- 可選項眾多 - 可選項越多，用戶越難找到想要物品。通過向用戶推薦相關物品，其中有些可能用戶事先不知道，能幫助用戶發現新物品。

### 推薦模型種類
- 基於內容的過濾 (需事先定義用戶與物品的屬性，工作量大、有隱私權疑慮)
- 協同過濾
- 排名模型

## 矩陣分解 (Matrix factorization)
- 顯式矩陣 - 要處理那些數據是由用戶所提供的偏好數據，例如用戶對電影的評價
- 隱式矩陣 - 用戶對物品的偏好不會直接給出，而是隱含在用戶與物品的互動之中，例如用戶把某商品加入購物車

大部份情況下，單個用戶只會接觸少部分物品，使得該矩陣只有少部分的數據非零，這種矩陣也稱為稀疏矩陣 (sparse matrices)。對於這種矩陣的建模，採用矩陣分解方式，具體方法是找出兩個低維度的矩陣，使得他們的乘積類似原始矩陣。
- 原始矩陣: [U x I]
- 因子矩陣: [U x k], [k x I], k 小於 U, I

要計算給定用戶對於某物品的預期評價，從用戶因子矩陣和物品因子矩陣分別選取相對應的行(用戶因子向量)與列(物品因子向量)，計算兩者的點積。

![](pictures/predicted-rating.png)

> 用戶因子向量: user's factor vector, 物品因子向量: item's factor vector

對於物品間的相似性，使用最鄰近模型中用到的相似衡量方法。利用物品因子向量，將相似度計算轉換為對兩物品因子向量之間相似度的計算。

![](pictures/computing-similarity.png)

### ALS (Alternaing Least Square)
是一種求解矩陣分解問題的優化方法。實現原理：每一次迭代，固定用戶因子矩陣或是物品因子矩陣的一個，然後用固定的這個矩陣以及評級數據來更新另一個矩陣。之後被更新的矩陣固定住，在更新另一個矩陣。迭代直到模型收斂。

## 使用用戶偏好資料來建立推薦模型
source: [src/ex-4](src/ex-4)

```shell
$ spark-submit --class RecommendationApp --jars /learn-spark/jars/jblas-1.2.4.jar target/scala-2.11/recommendation-app_2.11-1.0.jar
```

### 從 MovieLens 100K 提取特徵
```scala
val rawData = sc.textFile("../ml-100k/u.data")
val rawRatings = rawData.map(_.split("\t").take(3))
val ratings = rawRatings.map{ case Array(user, product, rating) => Rating(user.toInt, product.toInt, rating.toDouble) }
```

### 訓練推薦模型
```scala
val model = ALS.train(ratings, 50, 10, 0.01)
```
- rank: 對應 ALS 模型中的因子個數，在低價近似矩陣中的隱含特徵個數
- iterations: 對應運行時的迭代次數。ALS 確保每次迭代都能降低評級矩陣的重建誤差，但一般經少數次迭代便能收斂為一個比較合理的好模型。
- lambda: 控制模型的正則化過程。越高正則化越嚴厲。

```scala
model.userFeatures.count      //> 943
model.productFeatures.count   //> 1682
```
- `userFeatures`: 用戶因子矩陣 (user-factor matrix)
- `productFreatures`: 物品因子矩陣 (item-factor matrix)

## 使用模型為用戶進行推薦、找出指定物品的類似物品
source: [src/ex-4](src/ex-4)

### 用戶推薦
用戶推薦是指向給定用戶推薦物品。

```scala
model.predict(798, 123)
```
- `MatrixFactorizationModel` 提供 `predict` 函數，計算給定用戶對給定物品的預期評分

```scala
model.recommendProducts(789, 10)
```
- `MatrixFactorizationModel` 提供 `recommendProducts` 函數，為給定用戶生成前 K 個推薦物品

### 物品推薦
```scala
def cosineSimilarity(vec1: DoubleMatrix, vec2: DoubleMatrix): Double = {
  vec1.dot(vec2) / (vec1.norm2() * vec2.norm2())
}
```
- 計算兩個向量之間的餘弦相似度。餘弦相似度指兩個向量在 n 為空間裡，兩者夾角的角度。
- 相似度介於 -1 ~ 1。1: 完全相同；0 互不相干；-1 完全不同

```scala
val itemId = 567
val itemFactor = model.productFeatures.lookup(itemId).head
val itemVector = new DoubleMatrix(itemFactor)
println("cosineSimilarity=" + cosineSimilarity(itemVector, itemVector))
```
- 採用 jblas 線性代數庫，創建 DoubleMatrix (提供向量點積、像量範數的函數)
- `cosineSimilarity` 計算向量跟自己的餘弦相似度

```scala
val sims = model.productFeatures.map{ case (id, factor) =>
  val factorVector = new DoubleMatrix(factor)
  val sim = cosineSimilarity(factorVector, itemVector)
  (id, sim)
}
```
- 計算 productFeatures “每個物品向量” 與 “物品#567向量” 的餘弦相似度

```scala
val sortedSims = sims.top(10)(Ordering.by[(Int, Double), Double] { case (id, similarity) => similarity })
```
- 找出與物品#567最相似的前10個物品
  - 找 `sims` 前10個
  - 比較方法 `Ordering.by`，輸入 (id, similarity)，比較 similarity (降冪排序)
- 等同於 `sims.top(10)(Ordering.by[(Int, Double), Double](_._2))`

> 使用 `top()` 分散式排序有比 `sims.sortBy(_._2).take(10)` 更高效嗎？

> 更多 `Ordering.by` 的細節，參考 [比較集合內的元素](https://github.com/hugolu/learn-scala/blob/master/myNote/ordered-ordering.md)

## 應用標準的評估指標來評估該模型的預測能力
source: [src/ex-4](src/ex-4)

### 均方差 (Mean Squared Error, MSE)
- 用於衡量 "用戶-物品" 憑藉矩陣的重建誤差
- 適合評估顯式數據集的評級

#### 評估平方誤差 (Squared Error)
```scala
val moviesForUser = ratings.keyBy(_.user).lookup(789)
val actualRating = moviesForUser.take(1)(0)
val predictedRating = model.predict(789, actualRating.product)
val squaredError = math.pow(predictedRating - actualRating.rating, 2.0)
```
- 找出使用者#789平價過的電影，取一個
- 使用同一部電影預測評分
- 計算兩者平方誤差

#### 評估 MSE & RMSE
```scala
val usersProducts = ratings.map{ case Rating(user, product, rating) => (user, product) }
val predictions = model.predict(usersProducts).map{ case Rating(user, product, rating) => ((user, product), rating) }
val ratingsAndPredictions = ratings.map{ case Rating(user, product, rating) => ((user, product), rating) }.join(predictions)

val MSE = ratingsAndPredictions.map{ case ((user, product), (actual, predicted)) => math.pow((actual - predicted), 2) }.reduce(_ + _) / ratingsAndPredictions.count
val RMSE = math.sqrt(MSE)
```
- `predictions` 預測所有使用者對評價過物品的評價
- `ratingsAndPredictions` 合併實際評價與預測評價
- `MSE` 計算均方誤差 (Mean Squared Error)
- `RMSE` 計算均方根誤差 (Root Mean Squared Error)

#### 使用 MLlib 提供的方法評估 MSE, RMSE
```scala
val predictedAndTrue = ratingsAndPredictions.map { case ((user, product), (actual, predicted)) => (actual, predicted) }
val regressionMetrics = new RegressionMetrics(predictedAndTrue)
regressionMetrics.meanSquaredError      //> 0.08354004842106058
regressionMetrics.rootMeanSquaredError  //> 0.2890329538669606
```

### K值平均準確率 (Average Percision at K metric, APK)
- 用於衡量針對某個查詢所返回的 “前K個” 物件的相關性
- 適合評估隱式數據集的推薦

```scala
def avgPrecisionK(actual: Seq[Int], predicted: Seq[Int], k: Int): Double = {
  val predK = predicted.take(k)
  var score = 0.0
  var numHits = 0.0
  for ((p, i) <- predK.zipWithIndex) {
    if (actual.contains(p)) {
      numHits += 1.0
      score += numHits / (i.toDouble + 1.0)
    }
  }

  if (actual.isEmpty) {
    1.0
  } else {
    score / scala.math.min(actual.size, k).toDouble
  }
}
```
- 計算猜中的準確度

#### 評估 APK
試圖衡量模型對用戶感興趣和會去接觸的物品的預測能力
```scala
val topKRecs = model.recommendProducts(798, 10)
val actualMovies = moviesForUser.map(_.product)
val predictedMovies = topKRecs.map(_.product)
val apk10 = avgPrecisionK(actualMovies, predictedMovies, 10)  //> 0 (通常很低)
```
- 找出對用戶#789推薦的前十部電影
- 找出用戶#789看過的電影
- 計算推薦與實際看過的前十部電影的預測平均精準度

#### 評估 MAPK
計算對每一個用戶的APK得分，在求平均

```scala
val itemFactors = model.productFeatures.map{ case (id, factor) => factor }.collect()
val itemMatrix = new DoubleMatrix(itemFactors)
val imBroadcast = sc.broadcast(itemMatrix)
val allRecs = model.userFeatures.map{ case (userId, array) =>
  val userVector = new DoubleMatrix(array)
  val scores = imBroadcast.value.mmul(userVector)
  val sortedWithId = scores.data.zipWithIndex.sortBy(-_._1)
  val recommendedIds = sortedWithId.map(_._2 + 1).toSeq
  (userId, recommendedIds)
}
val userMovies = ratings.map{ case Rating(user, product, rating) => (user, product) }.groupBy(_._1).map{ case (k, v) => (k, v.map(_._2).toSeq) }

val MAPK10 = allRecs.join(userMovies).map{ case (userId, (predicted, actual)) =>
  avgPrecisionK(actual, predicted, 10)
}.reduce(_ + _) / allRecs.count       //> 0.06676980760490839

val MAPK2000 = allRecs.join(userMovies).map{ case (userId, (predicted, actual)) =>
  avgPrecisionK(actual, predicted, 2000)
}.reduce(_ + _) / allRecs.count       //> 0.19051800057432963
```
- `allRecs: org.apache.spark.rdd.RDD[(Int, Seq[Int])]`
- `userMovies: org.apache.spark.rdd.RDD[(Int, Seq[Int])]`
- `MAPK#` 加總所有用戶選擇與推薦物品的預測平均精準度，然後除以用戶數目

#### 透過 MLlib 提供的方法評估 MAPK
```scala
val predictedAndTrueForRanking = allRecs.join(userMovies).map{ case (userId, (predicted, actual)) =>
  (predicted.toArray, actual.toArray)
}
val rankingMetrics = new RankingMetrics(predictedAndTrueForRanking)

rankingMetrics.meanAveragePrecision   //> 0.1905180005743294
```
- `predictedAndTrueForRanking: org.apache.spark.rdd.RDD[(Array[Int], Array[Int])]`

## 另一種玩法
### 原來計算 MAP 的方式
```scala
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.jblas.DoubleMatrix
import org.apache.spark.mllib.evaluation.RankingMetrics

val ratings = sc.textFile("../ml-100k/u.data").map(_.split("\t").take(3)).map{case Array(u,p,r) => Rating(u.toInt, p.toInt, r.toDouble)}

val model = ALS.train(ratings, 50, 10, 0.01)

val userFactors = model.userFeatures.map{case (id, factor) => factor}.collect
val userMatrix = new DoubleMatrix(userFactors)
println(userMatrix.rows, userMatrix.columns)  //> (943,50)

val itemFactors = model.productFeatures.map{case (id, factor) => factor}.collect
val itemMatrix = new DoubleMatrix(itemFactors)
println(itemMatrix.rows, itemMatrix.columns)  //> (1682,50)

val imBroadcast = sc.broadcast(itemMatrix)
val allRecs = model.userFeatures.map{ case (userId, array) =>
  val userVector = new DoubleMatrix(array)
  val scores = imBroadcast.value.mmul(userVector)
  val sortedWithId = scores.data.zipWithIndex.sortBy(-_._1)
  val recommendedIds = sortedWithId.map(_._2 + 1).toSeq
  (userId, recommendedIds)
}

val userMovies = ratings.map{ case Rating(user, product, rating) => (user, product)}.groupByKey

val predictedAndTrueRanking = allRecs.join(userMovies).map{ case (userId, (predicted, actual)) => (predicted.toArray, actual.toArray) }

val rankingMetrics = new RankingMetrics(predictedAndTrueRanking)
rankingMetrics.meanAveragePrecision           //> res2: Double = 0.19042544203054956
```

### 另一種計算 MAP 的方式
不同於前者分別預測每個用戶選擇的電影，下面直接計算 user factor matrix 與 item factor matrix 相乘，然後根據 userMovies 得到真實與預測的電影排名。

```scala
import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.jblas.DoubleMatrix
import org.apache.spark.mllib.evaluation.RankingMetrics

val ratings = sc.textFile("../ml-100k/u.data").map(_.split("\t").take(3)).map{case Array(u,p,r) => Rating(u.toInt, p.toInt, r.toDouble)}

val model = ALS.train(ratings, 50, 10, 0.01)

val userFactors = model.userFeatures.map{case (id, factor) => factor}.collect
val userMatrix = new DoubleMatrix(userFactors)
println(userMatrix.rows, userMatrix.columns)  //> (943,50)

val itemFactors = model.productFeatures.map{case (id, factor) => factor}.collect
val itemMatrix = new DoubleMatrix(itemFactors)
println(itemMatrix.rows, itemMatrix.columns)  //> (1682,50)

val scoreMatrix = userMatrix.mmul(itemMatrix.transpose)
println(scoreMatrix.rows, scoreMatrix.columns)//> (943,1682)

val userMovies = ratings.map{ case Rating(user, product, rating) => (user, product)}.groupByKey

val predictedAndTrueRanking = userMovies.map{ case (userId, actual) =>
  val scores = scoreMatrix.getRow(userId-1)
  val sortedWithIdx = scores.data.zipWithIndex.sortBy(-_._1)
  val recommendedIds = sortedWithIdx.map(_._2 + 1).toSeq
  (recommendedIds.toArray, actual.toArray)
}

val rankingMetrics = new RankingMetrics(predictedAndTrueRanking)
rankingMetrics.meanAveragePrecision           //> res3: Double = 0.1921245427658284
```
