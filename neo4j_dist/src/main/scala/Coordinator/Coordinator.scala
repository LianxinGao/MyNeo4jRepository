package Coordinator

import java.io.InputStream
import java.net.InetAddress
import java.util.Properties
import java.util.concurrent.{ExecutorService, Executors}

import net.neoremind.kraps.RpcConf
import net.neoremind.kraps.rpc.netty.NettyRpcEnvFactory
import net.neoremind.kraps.rpc.{RpcAddress, RpcCallContext, RpcEndpoint, RpcEndpointRef, RpcEnv, RpcEnvClientConfig, RpcEnvServerConfig}

//import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

object Coordinator {
  def main(args: Array[String]): Unit = {
    val host = InetAddress.getLocalHost.getHostAddress
    val config = RpcEnvServerConfig(new RpcConf(), "Coordinator", host, 6668)
    val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
    val coordinatorEndpoint: RpcEndpoint = new Coordinator(rpcEnv)
    rpcEnv.setupEndpoint("Coordinator", coordinatorEndpoint)
    rpcEnv.awaitTermination()
  }
}

class Coordinator(override val rpcEnv: RpcEnv) extends RpcEndpoint {
  val properties = new Properties()
  val settings: InputStream = Coordinator.getClass.getClassLoader.getResourceAsStream("settings.properties")
  properties.load(settings)

  val writeNodeIp = properties.getProperty("writeNodeIp")
  val threadPool: ExecutorService = Executors.newFixedThreadPool(100)
  val hostIp: String = InetAddress.getLocalHost.getHostAddress
  val rpcRefList: ArrayBuffer[RpcEndpointRef] = ArrayBuffer[RpcEndpointRef]()
  var writeNodeRef: RpcEndpointRef = _

  override def onStart(): Unit = {
    println("Coordinator is initializing....")
    try {
      val client = new ZkClient(properties.getProperty("connectString"), properties.getProperty("sessionTimeout").toInt, hostIp)
      client.getConnect()
      val hosts = client.getReadNodeChildren
      println(hosts.size())
      val rpcConf = new RpcConf()
      println(hosts)

      // init read node
      for (i <- 0 until hosts.size()) {
        val ip = hosts.get(i)
        val config = RpcEnvClientConfig(rpcConf, hostIp)
        val rpcEnv: RpcEnv = NettyRpcEnvFactory.create(config)
        if (ip =="192.168.49.10"){
          val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress(ip, 6668),
            "WriteNode")
          rpcRefList += endPointRef
        }else{
          val endPointRef: RpcEndpointRef = rpcEnv.setupEndpointRef(RpcAddress(ip, 6668),
            "ReadNode")
          rpcRefList += endPointRef
        }
      }

      // init write node
      val config = RpcEnvClientConfig(rpcConf, hostIp)
      val rpcEnv2: RpcEnv = NettyRpcEnvFactory.create(config)
      writeNodeRef = rpcEnv2.setupEndpointRef(RpcAddress(writeNodeIp, 6668), "WriteNode")

      println("Initializing SUCCESS!!!")

    } catch {
      case e: Exception => {
        System.out.println("Please start your Zookeeper Server!!!" + e.getMessage)
        rpcEnv.shutdown()
      }
    }
  }

  override def receiveAndReply(context: RpcCallContext): PartialFunction[Any, Unit] = {

    case ExecuteCypher(cypher: String) => {
      threadPool.execute(new ParseAndSend(cypher, context, rpcRefList, writeNodeRef))
    }
  }
}

case class ExecuteCypher(cypher:String)

