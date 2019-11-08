package ReadNode;

import org.apache.zookeeper.*;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class RegisterNode {
    private String connectString;
    private int sessionTimeout;

    private ZooKeeper zk = null;
    private String parentNode = "/servers";
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public RegisterNode(String connectString, int sessionTimeout) {
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
    }

    public void getConnect() {
        try {
            zk = new ZooKeeper(connectString, sessionTimeout, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    countDownLatch.countDown();
                }
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    registerServer();
                }
            });
            countDownLatch.await();
            System.out.println("zookeeper connection success");
        } catch (Exception e) {
            System.out.println("on getConnection error: " + e.getMessage());
        }
    }

    // register server
    public void registerServer() {
        try {
            String hostname = InetAddress.getLocalHost().getHostAddress();
            if (zk.exists(parentNode, false) == null) {
                zk.create(parentNode, "servers".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String create = zk.create(parentNode + "/server", hostname.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(hostname + " is online " + create);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // make sure this node is online
    public void business() throws Exception {
        Thread.sleep(Long.MAX_VALUE);
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        InputStream resourceAsStream = RegisterNode.class.getClassLoader().getResourceAsStream("settings.properties");
        properties.load(resourceAsStream);

        RegisterNode server = new RegisterNode(properties.getProperty("connectString"), Integer.valueOf(properties.getProperty("sessionTimeout")));
        server.getConnect();
        server.registerServer();
        server.business();
    }
}
