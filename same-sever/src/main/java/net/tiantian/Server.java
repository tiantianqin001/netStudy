package net.tiantian;

import core.IoContext;
import foo.constants.TCPConstants;
import impl.IoSelectorProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    public static void main(String []args) throws IOException {
        IoContext.setUp().ioProvider(new IoSelectorProvider())
                .start();


        //首先开启服务
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        tcpServer.start();

        //发送服务端的ip和端口给客服端
        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();


            tcpServer.broadcast(str);
        } while (!"00bye00".equalsIgnoreCase(str));
        UDPProvider.stop();
        tcpServer.stop();
    }
}
