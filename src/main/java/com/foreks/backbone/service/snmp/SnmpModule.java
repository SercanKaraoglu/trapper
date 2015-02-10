package com.foreks.backbone.service.snmp;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class SnmpModule extends Verticle {
    @Override
    public void start() {
        super.start();
        final JsonObject config = this.container.config();
        final JsonObject restConfig = config.getObject("snmp-rest");
        if (null != restConfig) {
            this.container.deployVerticle(SnmpRestServiceVerticle.class.getName(), restConfig);
        }
        final JsonObject moduleConfig = config.getObject("snmp-module");
        if (null != moduleConfig) {
            this.container.deployVerticle(SnmpVerticle.class.getName(), moduleConfig, moduleConfig.getInteger("instance", 1));
        }
    }
}
