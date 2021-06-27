import bean.ServerInfo;
import clink.utils.ByteUtils;
import foo.constants.UDPConstants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UDPSearcher {
    private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        System.out.println("UDPSearcher  start");
        //客户端发送广播和接受到服务端的ip和端口

        //创建成功获取到消息的栅栏
        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadcast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //到这里就搜索完成了
        System.out.println("搜索完成");
        if (listener == null){
            return null;
        }
        List<ServerInfo> devices = listener.getServerAndClose();
        if (devices!=null && devices.size()>0){
            return devices.get(0);
        }
        return null;
    }

    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("UDPSearcher start listen.");
        CountDownLatch startLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, receiveLatch, startLatch);
        listener.start();
        startLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearcher sendBroadcast started.");
        DatagramSocket datagramSocket = new DatagramSocket();

        // 构建一份请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);

        //构建头部
        byteBuffer.put(UDPConstants.HEADER);
        //构建cmd
        byteBuffer.putShort((short) 1);
        //构建客户端发送的端口信息  这里主要是服务端我客服端监听这个端口
        byteBuffer.putInt(UDPSearcher.LISTEN_PORT);
        DatagramPacket datagramPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // 广播地址
        datagramPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        // 设置服务器端口  30202
        datagramPacket.setPort(UDPConstants.PORT_SERVER);
        //发送
        datagramSocket.send(datagramPacket);

        datagramSocket.close();
        System.out.println("UDPSearcher sendBroadcast end");


    }


    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch receiveLatch;
        private final CountDownLatch startLatch;
        private final byte[] buffer = new byte[128];
        private boolean done = false;
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private DatagramSocket ds;


        public Listener(int listenPort, CountDownLatch receiveLatch, CountDownLatch startLatch) {
            this.listenPort = listenPort;
            this.receiveLatch = receiveLatch;
            this.startLatch = startLatch;
        }

        @Override
        public void run() {
            super.run();
            //开始启动线程
            startLatch.countDown();

            try {
                //监听端口服务端发消息主要是这个端口
                ds = new DatagramSocket(listenPort);
                // 构建接收实体
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done) {

                    ds.receive(receivePack);

                    //获取服务端的ip
                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();

                    int length = receivePack.getLength();
                    byte[] packetData = receivePack.getData();
                    //验证是不是满足条件
                    boolean isValid = length > minLen && ByteUtils.startsWith(packetData,
                            UDPConstants.HEADER);
                    if (!isValid) {
                        continue;
                    }
                    //获取到服务端的ip和端口
                    System.out.println("UDPSearcher receive form ip:" + ip
                            + "\tport:" + port + "\tdataValid:" + isValid);
                    //还要验证cmd 和端口
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer,
                            UDPConstants.HEADER.length, length);
                    short cmd = byteBuffer.getShort();
                    int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort < 0) {
                        System.out.println("UDPSearcher receive cmd:" + cmd + "\tserverPort:" + serverPort);
                        continue;
                    }
                    String sn = new String(buffer, minLen, length - minLen);
                    ServerInfo info = new ServerInfo(serverPort, ip, sn);
                    serverInfoList.add(info);
                    receiveLatch.countDown();
                }
            } catch (Exception e) {
            } finally {
                close();
            }
            System.out.println("UDPSearcher listener finished.");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        public List<ServerInfo> getServerAndClose() {
            done=true;
            close();
            return serverInfoList;
        }
    }
}
