package core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
  boolean registerInput(SocketChannel channel,HandleInputCallback callback);

  boolean registerOutput(SocketChannel channel,HandleOutputCallback callback);

  void unReisterInput(SocketChannel channel);
  void unRegisterOutput(SocketChannel channel);

  abstract class HandleInputCallback implements Runnable{
      @Override
      public void run() {
          canProviderInput();
      }
      protected abstract void canProviderInput();
  }
  abstract class HandleOutputCallback implements Runnable{
      private Object attach;

      public final void setAttach(Object attach) {
          this.attach = attach;
      }
      @Override
      public void run() {
          canProviderOutput(attach);
      }

      protected abstract void canProviderOutput(Object attach);
  }
}
