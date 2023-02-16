package io.kzonix.cetus

import com.typesafe.scalalogging.LazyLogging
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Main extends App with LazyLogging {
  logger.info("Hello world")
  logger.warn("Hello world")
  logger.error("Hello world", new RuntimeException("Oops..."))
}
