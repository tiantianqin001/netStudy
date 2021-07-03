package net.tiantian;

import net.tiantian.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback{
    private int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;
    public TCPServer(int port) {
        // 转发线程池
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
        this.port = port;
    }

    public boolean start() {
        //开始一个线程接受客服端
        ClientListener clientListener= null;
        try {
            clientListener = new ClientListener(port);
            mListener=clientListener;
            clientListener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onSelfClosed(ClientHandler handler) {
        //关闭客服端
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler handler, String msg) {
        //客服端接收到消息
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.class){
                for (ClientHandler clientHandler : clientHandlerList) {
                    if (clientHandler.equals(handler)){
                        continue;
                    }
                    clientHandler.send(msg);
                } 
            }
        });

    }

    public void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }

            clientHandlerList.clear();
        }

        // 停止线程池
        forwardingThreadPoolExecutor.shutdownNow();
    }

    private class ClientListener extends Thread{
        private int port;
        private final ServerSocket server;
        private boolean done = false;
        private Socket socket;

        public ClientListener(int port) throws IOException {

            this.port = port;
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + " P:" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            System.out.println("服务器准备就绪～");

            do {
                try {
                    socket = server.accept();
                    ClientHandler clientHandler=new ClientHandler(socket,TCPServer.this);
                    // 读取数据并打印
                    clientHandler.readToPrint();

                    //添加所有的 客服端
                    synchronized (TCPServer.class){

                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }while (!done);
            System.out.println("服务器已关闭！");

        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
