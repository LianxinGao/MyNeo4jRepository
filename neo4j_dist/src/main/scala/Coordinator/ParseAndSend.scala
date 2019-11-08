package Coordinator

import net.neoremind.kraps.rpc.{RpcCallContext, RpcEndpointRef}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Random

class ParseAndSend(cypher: String, context: RpcCallContext, rpcRefList: ArrayBuffer[RpcEndpointRef], writeNodeRef: RpcEndpointRef) extends Runnable with Serializable {
  var future: Future[String] = _

  override def run(): Unit = {
    val isWrite = parseQueryType(cypher)
    if (isWrite) {
      future = writeNodeRef.ask[String](WriteCypher(cypher))
    }
    else {
      val num = Random.nextInt(rpcRefList.size)
      val ref = rpcRefList(num)
      future = ref.ask[String](ReadCypher(cypher))
    }

    future.onComplete {
      case scala.util.Success(value) => context.reply(value)
      case scala.util.Failure(e) => context.reply(e.getMessage)
    }
  }

  def parseQueryType(cypher: String): Boolean = {
    val res = cypher.toLowerCase
    if (res.contains("create") || res.contains("delete") || res.contains("merge") || res.contains("set")) {
      true
    }
    else false
  }
}

case class ReadCypher(cypher: String)

case class WriteCypher(cypher: String)

