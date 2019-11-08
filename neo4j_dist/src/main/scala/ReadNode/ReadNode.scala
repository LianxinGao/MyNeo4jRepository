package ReadNode

import java.io.InputStream
import java.net.InetAddress
import java.util.Properties
import java.util.concurrent.{ExecutorService, Executors}

import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.netty.NettyRpcEnvFactory
import net.neoremind.kraps.rpc.{RpcCallContext, RpcEndpoint, RpcEnv, RpcEnvServerConfig}
import org.neo4j.driver.v1.{AuthTokens, Driver, GraphDatabase}

//import scala.actors.threadpool.{ExecutorService, Executors}

object ReadNode {
  def main(args: Array[String]): Unit = {
    val host = InetAddress.getLocalHost.getHostAddress
    val config = RpcEnvServerConfig(new RpcConf(), "ReadNode", host, 6668)
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
    val coordinatorEndpoint: RpcEndpoint = new ReadNode(rpcEnv)
    rpcEnv.setupEndpoint("ReadNode", coordinatorEndpoint)
    rpcEnv.awaitTermination()
  }
}

class ReadNode(override val rpcEnv: RpcEnv) extends RpcEndpoint {
  val properties = new Properties()
  val settings: InputStream = ReadNode.getClass.getClassLoader.getResourceAsStream("settings.properties")
  properties.load(settings)

  val localDriver:Driver = GraphDatabase.driver(s"bolt://localhost:7687",
    AuthTokens.basic(properties.getProperty("account"), properties.getProperty("password")))

  val threadPool: ExecutorService = Executors.newFixedThreadPool(25)

  override def onStart(): Unit = {
    val register = new RegisterNode(properties.getProperty("connectString"), properties.getProperty("sessionTimeout").toInt)
    register.getConnect()
    register.registerServer()
    println("Coordinator is started....")

  }

  override def receiveAndReply(context_to_master: RpcCallContext): PartialFunction[Any, Unit] = {

    case ReadCypher(cypher: String) => {
      //TODO: now use a write node as read node, latter not use write node
      threadPool.execute(new CypherReader(localDriver, cypher, context_to_master))
    }
  }
}

case class ReadCypher(cypher:String)
