package core;

import impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    private UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    public void setUp(SocketChannel socketChannel) throws IOException {
        this.channel=socketChannel;
        IoContext ioContext = IoContext.get();
        SocketChannelAdapter adapter=new SocketChannelAdapter(channel,ioContext.getIoProvider(),
                this);
        this.sender=adapter;
        this.receiver=adapter;

        readNextMessage();
    }
    //开始读下一条数据
    private void readNextMessage() {
        if (receiver !=null){
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private IoArgs.IoArgsEventListener echoReceiveListener=new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            System.out.println("服务端开始读取客服端的数据");
            // 打印
            onReceiveNewMessage(args.bufferString());

            // 读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String str) {
        System.out.println("获取消息。。。。。"+str);
        System.out.println(key.toString() + ":" + str);
    }
    @Override
    public void close() throws IOException {

    }
    @Override
    public void onChannelClosed(SocketChannel channel) {

    }
}
