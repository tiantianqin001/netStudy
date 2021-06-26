import bean.ServerInfo;

public class Client {
    public static void main(String [] args){
        //搜索服务端的ip 地址 然后去链接
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("获取到服务端信息"+info);
        if (info!=null){

        }
    }
}
