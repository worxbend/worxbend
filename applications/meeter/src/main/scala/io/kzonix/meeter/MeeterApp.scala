package io.kzonix.meeter

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.http.*
import zio.http.model.Cookie.Type.request
import zio.json.*
import zio.json.DeriveJsonEncoder
import zio.json.JsonEncoder
import zio.json.SnakeCase
import zio.json.jsonMemberNames
import zio.kafka.consumer.Consumer
import zio.kafka.consumer.ConsumerSettings
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings
import zio.logging.*
import zio.logging.LogAnnotation
import zio.logging.LogAnnotation.*
import zio.metrics.jvm.DefaultJvmMetrics

import scala.collection.immutable

import java.time.Instant
import java.util.UUID

import com.typesafe.config.ConfigFactory

object MeeterApp extends ZIOAppDefault:

  private val Workflow                          = LogAnnotation[WorkflowInfo](
    "workflow",
    (_, p) => p,
    p => p.toJson,
  )
  private val Uuid                              = LogAnnotation[UUID](
    "uuid",
    (_, i) => i,
    _.toString,
  )
  private val InstanceId: LogAnnotation[String] = LogAnnotation[String](
    "instance_id",
    (l, r) => l + r,
    r => r,
  )

  @jsonMemberNames(CamelCase)
  case class WorkflowInfo(
      workflowId: String,
      tenantId:   String,
    )
  object WorkflowInfo:
    implicit val encoder: JsonEncoder[WorkflowInfo] = DeriveJsonEncoder.gen[WorkflowInfo]
  private val config                            = ConfigFactory.load()
  private val configProvider                    = TypesafeConfigProvider.fromTypesafeConfig(config)

  override val bootstrap: ZLayer[Any, Any, Any] =
    Runtime.removeDefaultLoggers
      >>> Runtime.setConfigProvider(configProvider)
      >>> consoleJsonLogger()
      >>> DefaultJvmMetrics.live
      >>> logMetrics

  override def run: ZIO[Any, Any, Any] =
    (for {
      _ <- ZIO.logInfo("Hello ZIO")
    } yield ()).provide(
      consumerLayer
      // ZLayer.Debug.tree,
    )

  def producerRegistry(clusters: Map[String, String]) = {
    val producers                                              = clusters.map {
      case (cluster, bootstrapServers) =>
        producerLayer(bootstrapServers.split(',').toList)
          .map(p => (cluster, p))
    }
    val registry: ZIO[Scope, Throwable, Map[String, Producer]] = ZIO.collectAll(producers).map(_.toMap)
    ZIO.scoped(registry)

  }

  def producerLayer(bootstrapServers: List[String]) =
    Producer.make(
      settings = ProducerSettings(bootstrapServers)
    )

  def consumerLayer: ZLayer[Any, Throwable, Consumer] =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:9092")).withGroupId("group")
      )
    )
