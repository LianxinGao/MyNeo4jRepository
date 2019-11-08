package Coordinator;

import net.neoremind.kraps.rpc.RpcEndpointRef;
import net.neoremind.kraps.rpc.RpcEnv;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZkClient {
    private String connectString;
    private int sessionTimeout;
    private String hostIp;
    private ZooKeeper zk = null;
    private String parentNode = "/servers";

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public ZkClient(String connectString, int sessionTimeout, String hostIp) {
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
        this.hostIp = hostIp;
    }

    public void getConnect() {
        try {
            zk = new ZooKeeper(connectString, sessionTimeout, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    countDownLatch.countDown();
                }
                if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    System.out.println("hosts update!!!");
                }
                try {
                    getReadNodeChildren();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            countDownLatch.await();
            System.out.println("zookeeper connection success");
        } catch (Exception e) {
            System.out.println("getConnect..." + e.getMessage());
        }
    }

    public ArrayList<String> getReadNodeChildren() throws KeeperException, InterruptedException {
        List<String> children = zk.getChildren(parentNode, true);
        ArrayList<String> hosts = new ArrayList<>();
        for (String child : children) {
            byte[] data = zk.getData(parentNode + "/" + child, false, null);
            hosts.add(new String(data));
        }
        return hosts;
    }

}
