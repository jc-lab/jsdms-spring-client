package kr.jclab.jsdms.spring.client.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import kr.jclab.jsdms.spring.client.JsDMSSpringClientProperties;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ZookeeperTargetMonitor extends TargetMonitor implements IZkChildListener, IZkDataListener {
    protected String zpathRoot;

    public ZookeeperTargetMonitor(ServiceImpl service, JsDMSSpringClientProperties.TargetProperties properties, String gitRepoName) throws JSchException, IOException {
        super(service, properties, gitRepoName);
    }

    @Override
    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
        ServiceImpl service = this.service;
        registerWatcherToChilds(parentPath, currentChilds);

        if(parentPath.equalsIgnoreCase(this.zpathRoot)) {
            service.execute(() -> {
                for (String child : currentChilds) {
                    String cpath = parentPath + "/" + child;
                    Object cdata = this.service.getZkClient().readData(cpath);
                    onNodeDataChanged(cpath, cdata);
                }
            });
        }
    }

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {
        onNodeDataChanged(dataPath, data);
    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {
    }

    public void start(String zpath) {
        final ZkClient zkClient = service.getZkClient();

        this.zpathRoot = zpath;

        service.execute(() ->{
            zkClient.createPersistent(zpath, true);
            registerWatcherToChilds(this.zpathRoot, zkClient.getChildren(this.zpathRoot));
            zkClient.subscribeChildChanges(this.zpathRoot, this);
        });
    }

    private void registerWatcherToChilds(String parentPath, List<String> childs) {
        ZkClient zkClient = service.getZkClient();
        for(String child : childs) {
            String zpath = parentPath + "/" + child;
            zkClient.subscribeDataChanges(zpath, this);
        }
    }

    private void onNodeDataChanged(String path, Object data) {
        if(!(data instanceof byte[]))
            return ;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> jsonDoc = objectMapper.readValue((byte[])data, Map.class);
            String before = (String)jsonDoc.get("before");
            String after = (String)jsonDoc.get("after");

            if(before != null && (after == null || !before.equalsIgnoreCase(after))) {
                File fpath = new File(path);
                String branchName = fpath.getName();
                service.asyncPullBranch(this, branchName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
