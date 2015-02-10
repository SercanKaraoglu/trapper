package com.foreks.backbone.service.snmp;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class SnmpRestServiceVerticle extends Verticle {
    @Override
    public void start() {
        final String address = this.container.config().getString("address", "com.foreks.snmp.agent");
        final Integer port = this.container.config().getInteger("port", 8080);
        final RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.post("/snmp",
                          postRequest -> {
                              postRequest.bodyHandler(buffer -> {
                                  final String jsonString = buffer.getString(0, buffer.length(), "UTF-8");
                                  final JsonObject message = new JsonObject(jsonString);
                                  this.vertx.eventBus().send(address,
                                                             message,
                                                             (final Message<JsonObject> response) -> {
                                                                 final String status = response.body().getString("status");
                                                                 postRequest.response().putHeader("Content-Type", "application/json");
                                                                 postRequest.response()
                                                                            .setStatusCode(status.equals("ok") ? HTTP_OK : HTTP_INTERNAL_ERROR)
                                                                            .end(response.body().toString(), "UTF-8");
                                                             });
                              });
                          });
        this.vertx.createHttpServer().requestHandler(routeMatcher).listen(port);
    }
}
