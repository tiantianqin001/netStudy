import bean.ServerInfo;
import clink.utils.CloseUtils;

import java.io.*;
import java.net.*;

public class TCPClient {

    private Socket socket;
    private ReadHandler readHandler;

    private PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream=new PrintStream(socket.getOutputStream());
    }

    //开始连接
    public static TCPClient startWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(1000 * 90);
        //开始链接
        socket.connect(new InetSocketAddress(Inet4Address.
                getByName(info.getAddress()), info.getPort()));
        System.out.println("已经连上了服务器，开始了下面的操作");
        System.out.println("客服端的消息......ip=" + socket.getLocalAddress()
                + "。。。。端口==" + socket.getLocalPort());
        System.out.println("获取服务端的信息...ip=" + socket.getInetAddress() + ".....端口=" + socket.getPort());
        //开始接收消息，也就是读消息
        ReadHandler readHandler = new ReadHandler(socket.getInputStream());
        readHandler.start();
        return new TCPClient(socket,readHandler);
    }
    //发送数据到服务端
    public void send(String str) {
        printStream.println(str);
    }

    public void exit() {
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    private static class ReadHandler extends Thread {
        private InputStream inputStream;
        private static String str;
        private boolean done = false;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            BufferedReader sockInput = new BufferedReader(new InputStreamReader(inputStream));
            try {
                do {
                    try {
                        str = sockInput.readLine();
                    } catch (IOException e) {
                        continue;
                    }
                    if (str == null){
                        System.out.println("链接已经关闭，不能读取数据");
                        break;
                    }
                    //打印出读取的数据
                    System.out.println(str);

                } while (!done);
            }catch (Exception e){
                if (!done) {
                    System.out.println("连接异常断开：" + e.getMessage());
                }
            }finally {
                //关闭链接
                CloseUtils.close(inputStream);
            }
        }

        void exit(){
            done=true;
            CloseUtils.close(inputStream);
        }
    }
}
