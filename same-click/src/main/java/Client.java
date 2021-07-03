import bean.ServerInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Client {
    public static void main(String [] args){
        //搜索服务端的ip 地址 然后去链接
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("获取到服务端信息"+info);
        if (info!=null){

          TCPClient tcpClient=null;

            try {
                tcpClient = TCPClient.startWith(info);
                if (tcpClient ==null)return;
                //开始写数据 也就是发送数据给服务端
                write(tcpClient);

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("我不能发送数据了，我是客服端");
            }finally {
                //最后要退出
                if (tcpClient!=null){
                    tcpClient.exit();
                }

            }
        }
    }

    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();

            System.out.println("客服端发送消息"+str);

            // 发送到服务器
            tcpClient.send(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }
}
