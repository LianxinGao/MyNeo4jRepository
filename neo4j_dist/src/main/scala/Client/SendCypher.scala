package Client

import java.net.InetAddress
import scala.concurrent.ExecutionContext.Implicits.global
import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.{RpcAddress, RpcEndpointRef, RpcEnv, RpcEnvClientConfig}
import net.neoremind.kraps.rpc.netty.NettyRpcEnvFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object SendCypher {
  def main(args: Array[String]): Unit = {
    val hostIp = InetAddress.getLocalHost.getHostAddress
    val rpcConf = new RpcConf()
    val config = RpcEnvClientConfig(rpcConf, hostIp)
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)

    //point to the Coordinator's IP
    val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress("192.168.49.9", 6668),
      "Coordinator")

    val cypher = "match (n:Test) where n.name='Neo4j_1319' return n"


    val future: Future[String] = endPointRef.ask[String](ExecuteCypher(cypher))
    future.onComplete {
      case scala.util.Success(value) => {}
      case scala.util.Failure(error) => {}
    }
    val res = Await.result(future, Duration.apply("30s"))
    println(res)

  }
}

case class ExecuteCypher(cypher: String)
