package WriteNode

import java.io.InputStream
import java.net.InetAddress
import java.util.Properties
import java.util.concurrent.{ExecutorService, Executors}

import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.netty.NettyRpcEnvFactory
import net.neoremind.kraps.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEndpointRef, RpcEnv, RpcEnvClientConfig, RpcEnvServerConfig}
import org.neo4j.driver.v1.{AuthTokens, Driver, GraphDatabase}

//import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

object WriteNode {
  def main(args: Array[String]): Unit = {
    val host = InetAddress.getLocalHost.getHostAddress
    val config = RpcEnvServerConfig(new RpcConf(), "WriteNode", host, 6668)
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
    val coordinatorEndpoint: RpcEndpoint = new WriteNode(rpcEnv)
    rpcEnv.setupEndpoint("WriteNode", coordinatorEndpoint)
    rpcEnv.awaitTermination()
  }
}

class WriteNode(override val rpcEnv: RpcEnv) extends RpcEndpoint {
  val properties = new Properties()
  val settings: InputStream = WriteNode.getClass.getClassLoader.getResourceAsStream("settings.properties")
  properties.load(settings)

  val threadPool: ExecutorService = Executors.newFixedThreadPool(100)
  val hostIp:String = InetAddress.getLocalHost.getHostAddress
  val localDriver:Driver = GraphDatabase.driver(s"bolt://localhost:7687",
    AuthTokens.basic(properties.getProperty("account"), properties.getProperty("password")))

  val driverList: ArrayBuffer[Driver] = ArrayBuffer[Driver]()
  driverList += localDriver

  val rpcRefList:ArrayBuffer[RpcEndpointRef] = ArrayBuffer[RpcEndpointRef]()

  override def onStart(): Unit = {
    println("Coordinator is initializing....")
    init()
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {

    case WriteCypher(cypher: String) => {
      //      TODO: check the number of online nodes are the same as zk's nodes?
      threadPool.execute(new CypherWriter(driverList, cypher, context))
    }

    case ReadCypher(cypher: String) => {
      //TODO: now use a write node as read node, latter not use write node
      threadPool.execute(new CypherReader(localDriver, cypher, context))
    }
  }


  def init(): Unit ={
    try {
      val client = new ZkClient(properties.getProperty("connectString"), properties.getProperty("sessionTimeout").toInt)
      client.getConnect()
      val hosts = client.getChildren

      val rpcConf = new RpcConf()
      for (i <- 0 until hosts.size()) {
        val ip = hosts.get(i)
        if (ip != hostIp){
          println(ip)
          val driver = GraphDatabase.driver(s"bolt://$ip:7687", AuthTokens.basic(properties.getProperty("account"), properties.getProperty("password")))
          driverList += driver
          val config = RpcEnvClientConfig(rpcConf, hostIp)
          val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
          val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress(ip, 6668),
            "ReadNode")
          rpcRefList += endPointRef
        }
      }
      println("Initializing SUCCESS!!!")
    } catch {
      case e: Exception => {
        System.out.println("Please start your Zookeeper Server!!!" + e.getMessage)
        rpcEnv.shutdown()
      }
    }
  }

}

case class ReadCypher(cypher: String)

case class WriteCypher(cypher:String)
