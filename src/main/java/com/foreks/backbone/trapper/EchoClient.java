package com.foreks.backbone.trapper;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.Verticle;

public class EchoClient extends Verticle {

    @Override
    public void start() {
        this.vertx.createNetClient().connect(1234, "localhost", new AsyncResultHandler<NetSocket>() {
            @Override
            public void handle(final AsyncResult<NetSocket> asyncResult) {
                if (asyncResult.succeeded()) {
                    final NetSocket socket = asyncResult.result();
                    socket.dataHandler(new Handler<Buffer>() {
                        @Override
                        public void handle(final Buffer buffer) {
                            System.out.println("Net client receiving: " + buffer);
                        }
                    });

                    // Now send some data
                    for (int i = 0; i < 10; i++) {
                        final String str = "hello" + i + "\n";
                        System.out.print("Net client sending: " + str);
                        socket.write(new Buffer(str));
                    }

                } else {
                    asyncResult.cause().printStackTrace();
                }
            }
        });
    }
}
