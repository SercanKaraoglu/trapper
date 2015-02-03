package com.foreks.backbone.trapper;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

public class RestVerticle extends Verticle {
    @Override
    public void start() {
        this.container.config().getString("snmpserver");

        this.vertx.createHttpServer().requestHandler(req -> {

            this.vertx.eventBus().send("deneme", "dfasdfa", (final Message<String> rp) -> {
                req.response().end(rp.body());
            });
        }).listen(8080);

    }
}
