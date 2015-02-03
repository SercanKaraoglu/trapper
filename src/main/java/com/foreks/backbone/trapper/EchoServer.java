package com.foreks.backbone.trapper;

import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Verticle;

public class EchoServer extends Verticle {

    @Override
    public void start() {
        this.vertx.eventBus().registerHandler("deneme", h -> {
            h.reply("Sercan the white niggaz");
        });

        this.vertx.createNetServer().connectHandler(new Handler<NetSocket>() {
            @Override
            public void handle(final NetSocket socket) {
                Pump.createPump(socket, socket).start();
            }
        }).listen(1234);
    }
}
