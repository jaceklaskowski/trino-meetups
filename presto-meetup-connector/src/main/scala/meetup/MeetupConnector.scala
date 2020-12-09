package meetup

import io.prestosql.spi.connector.{Connector, ConnectorMetadata, ConnectorTransactionHandle}
import io.prestosql.spi.transaction.IsolationLevel

class MeetupConnector extends Connector
    with PrintlnLogging {

  override def beginTransaction(
      isolationLevel: IsolationLevel,
      readOnly: Boolean): ConnectorTransactionHandle = {
    log(s"MeetupConnector.beginTransaction($isolationLevel, readOnly=$readOnly)")
    val handle = new ConnectorTransactionHandle {}
    log(s"...handle=$handle")
    handle
  }

  override def getMetadata(
      transactionHandle: ConnectorTransactionHandle): ConnectorMetadata = {
    log(s"MeetupConnector.getMetadata($transactionHandle)")
    new ConnectorMetadata {}
  }
}
