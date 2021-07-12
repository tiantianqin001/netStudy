package net.tiantian.handler;

import clink.utils.CloseUtils;
import core.Connector;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {

    private SocketChannel socketChannel;
    private final Connector connector;
    private ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    private final ClientWriteHandler clientWriteHandler;
    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;
        connector = new Connector() {
            @Override
            public void onChannelClosed(SocketChannel channel) {
                exitBySelf();
            }
            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);
                clientHandlerCallback.onNewMessageArrived(ClientHandler.this,str);
            }

        };
        connector.setUp(socketChannel);

        Selector writeSelector= Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.clientHandlerCallback = clientHandlerCallback;

        clientWriteHandler = new ClientWriteHandler(writeSelector);
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);
    }


    public String getClientInfo() {
        return clientInfo;
    }

    public void send(String msg) {

        clientWriteHandler.send(msg);
    }

    private void exitBySelf() {
        exit();
        //关闭自己
        clientHandlerCallback.onSelfClosed(this);
    }


    public interface ClientHandlerCallback {
        //客服端关闭自己

        void onSelfClosed(ClientHandler handler);

        //接收消息
        void onNewMessageArrived(ClientHandler handler, String msg);
    }
    public void exit() {
        CloseUtils.close(connector);
        clientWriteHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出：" + clientInfo);
    }
    class ClientWriteHandler {

        private boolean done = false;
        private ExecutorService executorService;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        public ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        public void send(String msg) {

            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(msg));
        }

        public void exit() {
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private String msg;

            public WriteRunnable(String msg) {
                this.msg = msg+'\n';

                System.out.println("服务端发送消息....11111111111......."+msg);
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());
                // 反转操作, 重点
                byteBuffer.flip();
                while (!done && byteBuffer.hasRemaining()){
                    try {
                        int len = socketChannel.write(byteBuffer);
                        System.out.println("服务端发送消息....3333333333......."+len);
                        // len = 0 合法
                        if (len < 0) {
                            System.out.println("客户端已无法发送数据！");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

    }

}
