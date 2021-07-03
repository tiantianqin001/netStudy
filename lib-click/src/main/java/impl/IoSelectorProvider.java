package impl;

import core.IoProvider;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class IoSelectorProvider implements IoProvider {

    private final Selector readSelector;
    private final Selector writeSelector;
    // 是否处于某个过程
    private final AtomicBoolean inRegInput =new AtomicBoolean(false);
    private final AtomicBoolean inRegOutput =new AtomicBoolean(false);

    private final AtomicBoolean isClosed= new AtomicBoolean(false);


    public IoSelectorProvider() throws IOException {
        readSelector=  Selector.open();
        writeSelector=  Selector.open();

        // 开始输出输入的监听
        startRead();
        startWrite();
    }

    private void startWrite() {

    }

    private void startRead() {
        Thread thread =new Thread("Clink IoSelectorProvider ReadSelector Thread"){
            @Override
            public void run() {
                super.run();
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return false;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return false;
    }

    @Override
    public void unReisterInput(SocketChannel channel) {

    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {

    }
}
