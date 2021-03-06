// Copyright 2017, Yahoo Holdings Inc.
// Licensed under the terms of the Apache License 2.0. Please see LICENSE file in project root for terms.
package com.yahoo.maha.service.factory

import com.yahoo.maha.service.MahaServiceConfig
import com.yahoo.maha.core.{EngineRequirement, OracleLiteralMapper, PartitionColumnRenderer}
import com.yahoo.maha.core.query.QueryGenerator
import com.yahoo.maha.core.query.druid.{DruidQueryGenerator, DruidQueryOptimizer}
import com.yahoo.maha.core.query.oracle.OracleQueryGenerator
import com.yahoo.maha.core.request._
import org.json4s.JValue

import scalaz.Validation
import _root_.scalaz._
import syntax.applicative._
import Validation.FlatMap._

/**
 * Created by pranavbhole on 25/05/17.
 */
class OracleQueryGeneratorFactory extends QueryGeneratorFactory {

  """
    |{
    |"partitionColumnRendererClass" : "DefaultPartitionColumnRendererFactory",
    |"partitionColumnRendererConfig" : [{"key": "value"}],
    |"literalMapperClass" : "OracleLiteralMapper",
    |"literalMapperConfig": {}
    |}
  """.stripMargin

  override def fromJson(configJson: org.json4s.JValue) : MahaServiceConfig.MahaConfigResult[QueryGenerator[_ <: EngineRequirement]] = {
    import org.json4s.scalaz.JsonScalaz._
    val partitionColumnRendererClassResult: MahaServiceConfig.MahaConfigResult[String] = fieldExtended[String]("partitionColumnRendererClass")(configJson)
    val partitionColumnRendererConfigResult: MahaServiceConfig.MahaConfigResult[JValue] = fieldExtended[JValue]("partitionColumnRendererConfig")(configJson)
    val literalMapperClassResult: MahaServiceConfig.MahaConfigResult[String] = fieldExtended[String]("literalMapperClass")(configJson)
    val literalMapperConfigResult: MahaServiceConfig.MahaConfigResult[JValue] = fieldExtended[JValue]("literalMapperConfig")(configJson)

    val partitionColumnRenderer: MahaServiceConfig.MahaConfigResult[PartitionColumnRenderer] = for {
      partitionColumnRendererClass <- partitionColumnRendererClassResult
      partitionColumnRendererFactory <- getFactory[PartitionColumnRendererFactory](partitionColumnRendererClass, this.closer)
      partitionColumnRendererConfig <- partitionColumnRendererConfigResult
      partitionColumnRenderer <- partitionColumnRendererFactory.fromJson(partitionColumnRendererConfig)
    } yield partitionColumnRenderer

    val literalMapper: MahaServiceConfig.MahaConfigResult[OracleLiteralMapper] = for {
      literalMapperClass <- literalMapperClassResult
      literalMapperFactory <- getFactory[OracleLiteralMapperFactory](literalMapperClass, this.closer)
      literalMapperConfig <- literalMapperConfigResult
      literalMapper <- literalMapperFactory.fromJson(literalMapperConfig)

    } yield literalMapper

    (partitionColumnRenderer |@| literalMapper) {
      (psr, lm) => new OracleQueryGenerator(psr, lm)
    }
  }

  override def supportedProperties: List[(String, Boolean)] = List.empty
}

class DruidQueryGeneratorFactory extends QueryGeneratorFactory {
  """
    |{
    |"queryOptimizerClass": "SyncDruidQueryOptimizer",
    |"queryOptimizerConfig" : {},
    |"defaultDimCardinality" : "40000",
    |"maximumMaxRows" : "5000",
    |"maximumTopNMaxRows" : "400",
    |"maximumMaxRowsAsync" : "100000"
    |}
  """.stripMargin

  override def fromJson(configJson: org.json4s.JValue) : MahaServiceConfig.MahaConfigResult[QueryGenerator[_ <: EngineRequirement]] = {
    import org.json4s.scalaz.JsonScalaz._
    val queryOptimizerClassResult: MahaServiceConfig.MahaConfigResult[String] = fieldExtended[String]("queryOptimizerClass")(configJson)
    val queryOptimizerConfigResult: MahaServiceConfig.MahaConfigResult[JValue] = fieldExtended[JValue]("queryOptimizerConfig")(configJson)
    val dimCardinalityResult: MahaServiceConfig.MahaConfigResult[Long] = fieldExtended[Long]("dimCardinality")(configJson)
    val maximumMaxRowsResult: MahaServiceConfig.MahaConfigResult[Int] = fieldExtended[Int]("maximumMaxRows")(configJson)
    val maximumTopNMaxRowsResult: MahaServiceConfig.MahaConfigResult[Int] = fieldExtended[Int]("maximumTopNMaxRows")(configJson)
    val maximumMaxRowsAsyncResult: MahaServiceConfig.MahaConfigResult[Int] = fieldExtended[Int]("maximumMaxRowsAsync")(configJson)

    val queryOptimizer: MahaServiceConfig.MahaConfigResult[DruidQueryOptimizer] = for {
      queryOptimizerClass <- queryOptimizerClassResult
      queryOptimizerFactory <- getFactory[DruidQueryOptimizerFactory](queryOptimizerClass, this.closer)
      queryOptimizerConfig <- queryOptimizerConfigResult
      queryOptimizer <- queryOptimizerFactory.fromJson(queryOptimizerConfig)
    } yield queryOptimizer

    (queryOptimizer |@| dimCardinalityResult |@| maximumMaxRowsResult |@| maximumTopNMaxRowsResult |@| maximumMaxRowsAsyncResult) {
      (a, b, c ,d, e) => new DruidQueryGenerator(a, b, c, d, e)
    }
  }

  override def supportedProperties: List[(String, Boolean)] = List.empty
}