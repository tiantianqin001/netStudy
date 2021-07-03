package net.tiantian;

import clink.utils.ByteUtils;
import foo.constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UDPProvider {
    private static int port;
    private static Provider PROVIDER_INSTANCE;

    public static void start(int port) {

        UDPProvider.port = port;
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    public static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private boolean done = false;
        private DatagramSocket ds = null;
        // 存储消息的Buffer
        final byte[] buffer = new byte[128];

        public Provider(String sn) {
            this.sn = sn.getBytes();
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPProvider Started.");
            //这个主要是把ip 地址告诉客服端
            try {
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                while (!done) {
                    ds.receive(datagramPacket);
                    //获取客服端的信息
                    String clientIp = datagramPacket.getAddress().getHostAddress();
                    int clientPort = datagramPacket.getPort();
                    int clientDataLen = datagramPacket.getLength();
                    byte[] clientData = datagramPacket.getData();
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4)
                            && ByteUtils.startsWith(clientData, UDPConstants.HEADER);

                    System.out.println("UDPProvider receive form ip:" + clientIp
                            + "\tport:" + clientPort + "\tdataValid:" + isValid);

                    if (!isValid) {
                        continue;
                    }
                    // 解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePort = (((clientData[index++]) << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index] & 0xff)));


                    // 判断合法性
                    if (cmd == 1 && responsePort > 0) {
                        // 构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        // 直接根据发送者构建一份回送信息
                        DatagramPacket responsePacket = new DatagramPacket(byteBuffer.array(),
                                len,
                                datagramPacket.getAddress(),
                                responsePort);
                        ds.send(responsePacket);
                        System.out.println("UDPProvider response to:" + clientIp + "\tport:" + responsePort + "\tdataLen:" + len);
                    } else {
                        System.out.println("UDPProvider receive cmd nonsupport; cmd:" + cmd + "\tport:" + port);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }
        /**
         * 提供结束
         */
        void exit() {
            done = true;
            close();
        }
    }
}
