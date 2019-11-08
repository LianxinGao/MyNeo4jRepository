package WriteNode

import net.neoremind.kraps.rpc.{RpcCallContext}
import org.neo4j.driver.v1.{Driver, Record}

class CypherReader(localDriver: Driver, cypher: String, context: RpcCallContext) extends Runnable{

  override def run(): Unit = {
    var str = ""
    val session = localDriver.session()
    val res = session.run(cypher)

    while (res.hasNext) {
      val record: Record = res.next()
      str += record.toString + "\n"
    }
    context.reply(str)
    session.close()
  }
}
