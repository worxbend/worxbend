import scala.language.postfixOps

object ProjectUtils {

  object ProjectNames {

    def service(serviceName: String): String = normalizedName(
      "service",
      serviceName
    )

    def app(serviceName: String): String =
      normalizedName(
        "app",
        serviceName
      )

    def lib(libraryName: String): String =
      normalizedName(
        "impl",
        libraryName
      )

    def api(libraryName: String): String =
      normalizedName(
        "api",
        libraryName
      )

    private def normalizedName(typeName: String = "app", name: String): String =
      s"$name-$typeName"
  }

  object ProjectPaths {
    // build.sbt relative root directory
    private val root = "./"

    private def normalizedPath(args: Seq[String]): String = s"${ args.mkString("/") }"

    trait Project {
      protected def basePath: String

      protected def projectMainPath: String

      def api(args: Seq[String]): String =
        root + normalizedPath(
          List(
            basePath,
            projectMainPath
          ) ::: (args toList)
        ) + "-api"

      def impl(args: Seq[String]): String =
        root + normalizedPath(
          List(
            basePath,
            projectMainPath
          ) ::: (args toList)
        ) + "-impl"

      def lib(args: Seq[String]): String =
        root + normalizedPath(
          List(
            basePath,
            projectMainPath
          ) ::: (args toList)
        )

      def service(args: Seq[String]): String =
        root + normalizedPath(
          List(
            basePath,
            projectMainPath
          ) ::: (args toList)
        ) + "-service"

      def app(args: Seq[String]): String =
        root + normalizedPath(
          List(
            basePath,
            projectMainPath
          ) ::: (args toList)
        ) + "-app"

    }

    trait GeneralComponent extends Project {
      protected override def basePath: String = "components"
    }

    trait Application extends Project {
      protected override def basePath: String = "applications"
    }

    object Components {

      object Common extends GeneralComponent {
        protected override def projectMainPath: String = "common"
      }

      object Akka extends GeneralComponent {
        protected override val projectMainPath: String = "akka"
      }

      object Zio extends GeneralComponent {
        protected override def projectMainPath: String = "zio"
      }

      object Http4s extends GeneralComponent {
        protected override def projectMainPath: String = "http4s"
      }

    }

    object Applications {

      object Common extends Application {
        protected override def projectMainPath: String = "common"
      }

      object Sandbox extends Application {
        protected override def projectMainPath: String = "sandbox"
      }

      object Root extends Application {
        protected override def projectMainPath: String = "./"
      }

    }

  }

}
