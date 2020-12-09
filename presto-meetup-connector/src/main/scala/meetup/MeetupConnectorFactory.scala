package meetup

import io.prestosql.spi.connector.{Connector, ConnectorContext, ConnectorFactory, ConnectorHandleResolver}

import java.util

class MeetupConnectorFactory extends ConnectorFactory
    with PrintlnLogging {
  override def getName: String = MeetupConnectorFactory.NAME

  override def create(
      catalogName: String,
      config: util.Map[String, String],
      context: ConnectorContext): Connector = {
    log(s"MeetupConnectorFactory.create($catalogName, $config)")
    new MeetupConnector
  }

  override def getHandleResolver: ConnectorHandleResolver = {
    log(s"MeetupConnectorFactory.getHandleResolver")
    new ConnectorHandleResolver {}
  }
}

object MeetupConnectorFactory {
  // The name this connector is known by
  // and used as connector.name while registering a catalog
  val NAME = "meetup"
}
