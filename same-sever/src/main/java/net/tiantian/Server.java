package net.tiantian;

import foo.constants.TCPConstants;

public class Server {
    public static void main(String []args){
        //首先开启服务
        TCPServer tcpServer=new TCPServer(TCPConstants.PORT_SERVER);
        tcpServer.start();

        //发送服务端的ip和端口给客服端
        UDPProvider.start(TCPConstants.PORT_SERVER);


    }
}
