package net.tiantian.handler;

import clink.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ClientHandler {

    private Socket socket;
    private ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;
    private final ClientReadHandler clientReadHandler;
    private final ClientWriteHandler clientWriteHandler;


    public ClientHandler(Socket socket, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socket = socket;
        this.clientHandlerCallback = clientHandlerCallback;
        clientReadHandler = new ClientReadHandler(socket.getInputStream());
        clientWriteHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientInfo = "A[" + socket.getInetAddress().getHostAddress()
                + "] P[" + socket.getPort() + "]";
        System.out.println("新客户端连接：" + clientInfo);
    }


    public String getClientInfo() {
        return clientInfo;
    }


    public void readToPrint() {
        clientReadHandler.start();

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

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private InputStream inputStream;

        public ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;

        }

        @Override
        public void run() {
            super.run();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                System.out.println("客服端读取数据 ,,,,,,,,我不会打出");
                do {

                    String str = bufferedReader.readLine();
                    System.out.println("客户端已无法读取数据！" + str);
                    if (str == null) {

                        // 退出当前客户端
                        ClientHandler.this.exitBySelf();
                        break;
                    }

                    //发送消息给客服端
                    clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);

                } while (!done);
            } catch (Exception e) {
                if (!done) {
                    System.out.println("连接异常断开" + e.getMessage());
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }
        void exit() {
            done = true;
            CloseUtils.close(inputStream);

        }

    }


    public void exit() {
        clientReadHandler.exit();
        clientWriteHandler.exit();
        CloseUtils.close(socket);
        System.out.println("客户端已退出：" + socket.getInetAddress() +
                " P:" + socket.getPort());
    }


    class ClientWriteHandler {

        private boolean done = false;
        private ExecutorService executorService;
        private final PrintStream printStream;


        public ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

      public   void send(String msg) {
          if(done){
              return;
          }
            executorService.execute(new WriteRunnable(msg));
        }

        public void exit() {
            done=true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private String msg;

            public WriteRunnable(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.fillInStackTrace();
                }


            }
        }

    }

}
