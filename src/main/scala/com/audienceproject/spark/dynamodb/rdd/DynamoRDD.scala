/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  *
  * Copyright © 2018 AudienceProject. All rights reserved.
  */
package com.audienceproject.spark.dynamodb.rdd

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types.StructType
import org.apache.spark.{Partition, SparkContext, TaskContext}
import com.audienceproject.spark.dynamodb.connector.TableConnector

class DynamoRDD(sc: SparkContext,
                schema: StructType,
                scanPartitions: Seq[ScanPartition],
                requiredColumns: Seq[String] = Seq.empty,
                filters: Seq[Filter] = Seq.empty)
    extends RDD[Row](sc, Nil) {

    override def compute(split: Partition, context: TaskContext): Iterator[Row] = {
        val scanPartition = split.asInstanceOf[ScanPartition]
        scanPartition.scanTable(requiredColumns, filters)
    }

    override protected def getPartitions: Array[Partition] = scanPartitions.toArray

}

class DynamoJsonRDD(sc: SparkContext,
                scanPartitions: Seq[ScanPartition],
                requiredColumns: Seq[String] = Seq.empty,
                filters: Seq[Filter] = Seq.empty)
    extends RDD[String](sc, Nil) {

    override def compute(split: Partition, context: TaskContext): Iterator[String] = {
        val scanPartition = split.asInstanceOf[ScanPartition]
        scanPartition.scanTableAsJson(requiredColumns, filters)
    }

    override protected def getPartitions: Array[Partition] = scanPartitions.toArray

}

object DynamoRDD {
    def tableScanRdd(sc: SparkContext, tableName: String, region: String, numPartitions: Int) = {
        val connector = new TableConnector(tableName, numPartitions, Map("region" -> region))
        val scanPartitions = List.range(0, numPartitions).map(partition =>
            new ScanPartition(new StructType(), partition, connector))
        new DynamoJsonRDD(sc, scanPartitions)
    }
}
