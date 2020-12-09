package meetup

import io.prestosql.spi.Plugin
import io.prestosql.spi.connector.ConnectorFactory

import java.lang.{Iterable => JIterable}

class MeetupPlugin extends Plugin
    with PrintlnLogging {
  override def getConnectorFactories: JIterable[ConnectorFactory] = {
    log("getConnectorFactories")
    import scala.jdk.CollectionConverters._
    Seq[ConnectorFactory](new MeetupConnectorFactory).asJava
  }
}
