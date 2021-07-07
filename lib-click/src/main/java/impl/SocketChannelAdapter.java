package impl;

import clink.utils.CloseUtils;
import core.IoArgs;
import core.IoProvider;
import core.Receiver;
import core.Sender;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketChannelAdapter implements Sender, Receiver, Closeable {

    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener onChannelStatusChangedListener;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener onChannelStatusChangedListener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.onChannelStatusChangedListener = onChannelStatusChangedListener;
        channel.configureBlocking(false);
    }


    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        this.receiveIoEventListener=listener;
        return ioProvider.registerInput(channel,inputCallback);
    }

    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        if (isClosed.get()){
            throw new IOException("Current channel is closed!");
        }
        this.sendIoEventListener=listener;
        outputCallback.setAttach(args);
        return ioProvider.registerOutput(channel,outputCallback);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            // 解除注册回调
            ioProvider.unReisterInput(channel);
            ioProvider.unRegisterOutput(channel);

            // 关闭
            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            onChannelStatusChangedListener.onChannelClosed(channel);
        }

    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

    private final IoProvider.HandleInputCallback inputCallback=new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if (isClosed.get()){
                return;
            }
            IoArgs args =new IoArgs();
            IoArgs.IoArgsEventListener listener = receiveIoEventListener;

            if (listener!=null){
                listener.onStarted(args);
            }
            //开始读数据
            try {
                if (args.read(channel)>0 && listener!=null){
                    //读取完成的回调
                    listener.onCompleted(args);

                }else {
                    throw new IOException("Cannot read any data!");
                }
            } catch (IOException e) {
               CloseUtils.close(SocketChannelAdapter.this);
            }


        }
    };

    private final IoProvider.HandleOutputCallback outputCallback=new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if (isClosed.get()) {
                return;
            }
            // TODO
            sendIoEventListener.onCompleted(null);
        }
    };
}
