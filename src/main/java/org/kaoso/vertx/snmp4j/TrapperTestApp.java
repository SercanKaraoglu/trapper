package org.kaoso.vertx.snmp4j;

import java.net.HttpURLConnection;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class TrapperTestApp extends Verticle {
    @Override
    public void start() {
        this.container.config().getString("snmpserver");
        this.container.deployVerticle(Module.class.getName());
        final RouteMatcher routeMatcher = new RouteMatcher();
        routeMatcher.post("/snmp",
                          postRequest -> {
                              postRequest.bodyHandler(buffer -> {
                                  final String jsonString = buffer.getString(0, buffer.length(), "UTF-8");
                                  final JsonObject message = new JsonObject(jsonString);
                                  this.vertx.eventBus()
                                            .send("org.kaoso.vertx.snmp4j",
                                                  message,
                                                  (final Message<JsonObject> response) -> {
                                                      final String status = response.body().getString("status");
                                                      postRequest.response()
                                                                 .setStatusCode(status.equals("ok") ? java.net.HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_INTERNAL_ERROR)
                                                                 .setStatusMessage(status)
                                                                 .end();
                                        });
                              });
                          });
        this.vertx.createHttpServer().requestHandler(routeMatcher).listen(8080);
    }
}
