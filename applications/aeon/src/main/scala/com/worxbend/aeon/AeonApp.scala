package com.worxbend.aeon

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1DeleteOptions
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1Job
import io.kubernetes.client.openapi.models.V1JobSpec
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.util.Config
import io.kubernetes.client.util.Yaml

import scala.jdk.CollectionConverters.*
object AeonApp extends App {

  private val client: ApiClient = Config.defaultClient()

  Configuration.setDefaultApiClient(client)

  val api        = new CoreV1Api()
  val batchV1Api = new BatchV1Api()

  val podsListReq: CoreV1Api#APIlistPodForAllNamespacesRequest = api.listPodForAllNamespaces()
  val podList: V1PodList                                       = podsListReq.execute()

  val pods: List[V1Pod] = podList.getItems.asScala.toList

  println(s"Found ${ pods.size } pods")
  pods.foreach(pod => println(pod.getMetadata.getName))
  println("Done")

  println("Creating a job")

  val job = new V1Job()

  val template = Yaml.load("""
      |apiVersion: batch/v1
      |kind: Job
      |metadata:
      |  name: pi
      |spec:
      |  ttlSecondsAfterFinished: 3600
      |  backoffLimit: 5
      |  activeDeadlineSeconds: 100
      |  template:
      |    spec:
      |      containers:
      |      - name: pi
      |        image: perl:5.34.0
      |        command: ["perl",  "-Mbignum=bpi", "-wle", "print bpi(200000)"]
      |      restartPolicy: OnFailure
      |""".stripMargin)

  template match
    case job: V1Job =>
      try {
        val job1 = batchV1Api.readNamespacedJob("pi", "default").execute()
        val pi   = batchV1Api.deleteNamespacedJob("pi", "default")
        pi.orphanDependents(true)
        pi.execute()
      }
      catch {
        case e: io.kubernetes.client.openapi.ApiException => println(e.getMessage)
      }

      Thread.sleep(5000)
      job
        .getSpec
        .getTemplate
        .getSpec
        .getContainers
        .get(0)
        .addEnvItem(new V1EnvVar().name("SOME_ENV").value("some-value"))
      val request = batchV1Api.createNamespacedJob("default", job)
      request.execute()

      println("Job created")

      Thread.sleep(10000)

      val existingJob = batchV1Api.readNamespacedJob("pi", "default").execute()
      println(s"Job ${ existingJob.getMetadata.getName } created at ${ existingJob.getMetadata.getCreationTimestamp }")
      println(existingJob.getStatus.getReady)
      println(existingJob.getStatus.getActive)
      println(s"${existingJob.getStatus.getSucceeded} succeeded")
      println(existingJob.getStatus.getFailed)
      println(existingJob.getStatus.getStartTime)
      println(existingJob.getStatus.getCompletionTime)
      println(existingJob.getStatus.getConditions)

    case _ => println("Loaded definition is not a job")

}
