package org.kaoso.vertx.snmp4j;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.DefaultPDUFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

public class Module extends BusModBase implements CommandResponder, Handler<Message<JsonObject>> {
    public static final String        IPV4_REGEXP = "^\\s*[0-2]?\\d?(?<!2[6-9])\\d(?<!25[6-9])(\\.[0-2]?\\d?(?<!2[6-9])\\d(?<!25[6-9])){3}\\s*$";
    private static final List<Module> modules     = new Vector<>();

    static {
        initSecurityModels();
    }

    private static void initSecurityModels() {
        // SecurityModels models = SecurityModels.getInstance();
        // OctetString id = new OctetString(MPv3.createLocalEngineID());
        // models.addSecurityModel(new USM(SecurityProtocols.getInstance(), id, 0));
        // models.addSecurityModel(new TSM(id, false));
    }

    private static List<Module> getModulesByConfig(final JsonObject config) {
        final List<Module> list = new ArrayList<>();
        for (final Module module : modules) {
            if (config.equals(module.config)) {
                list.add(module);
            }
        }
        return list;
    }

    private static List<Module> getModulesBySnmp(final Snmp snmp) {
        final List<Module> list = new ArrayList<>();
        for (final Module module : modules) {
            if (snmp.equals(module.snmp)) {
                list.add(module);
            }
        }
        return list;
    }

    private Snmp                                    snmp;
    private final Map<TransportMapping, JsonObject> mappings = new HashMap<>();

    @Override
    public void start() {
        super.start();
        synchronized (modules) {
            final List<Module> exists = getModulesByConfig(this.config);
            if (exists.isEmpty()) {
                try {
                    this.snmp = createSnmp();
                    this.snmp.listen();
                } catch (final IOException e) {
                    if (!isQuiet()) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                for (final Module module : exists) {
                    if (module.snmp != null) {
                        this.snmp = module.snmp;
                    }
                }
                this.snmp = exists.get(0).snmp;
            }
            modules.add(this);
        }
        this.vertx.eventBus().registerHandler("org.kaoso.vertx.snmp4j", this);
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (modules) {
            modules.remove(this);
            if (getModulesBySnmp(this.snmp).isEmpty()) {
                try {
                    this.snmp.close();
                } catch (final Exception e) {
                    if (!isQuiet()) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        this.vertx.eventBus().unregisterHandler("org.kaoso.vertx.snmp4j", this);
    }

    public Snmp getSnmp() {
        return this.snmp;
    }

    private boolean isQuiet() {
        return this.config.getBoolean("quiet", true);
    }

    private Snmp createSnmp() throws IOException {
        final TransportMapping transport = createTransportMapping(this.config);
        final Snmp snmp = new Snmp(transport);
        this.mappings.put(transport, this.config);
        initTransports(snmp);
        return snmp;
    }

    private TransportMapping createTransportMapping(final JsonObject config) throws IOException {
        final Address address = GenericAddress.parse(config.getString("address", "udp:localhost/10161"));
        final TransportMapping transport = TransportMappings.getInstance().createTransportMapping(address);
        try {
            final Class<?> clazz = transport.getClass();
            final Lookup lookup = MethodHandles.lookup();
            lookup.findSetter(clazz, "asyncMsgProcessingSupported", Boolean.class).invoke(transport, true);
        } catch (final Throwable e) {
        }
        return transport;
    }

    private void initTransports(final Snmp snmp) throws IOException {
        final Iterator<Object> it = this.config.getArray("transports", new JsonArray()).iterator();
        while (it.hasNext()) {
            Object object = it.next();
            if (object instanceof String) {
                final JsonObject json = new JsonObject();
                json.putString("address", object.toString());
                object = json;
            }
            if (object instanceof JsonObject) {
                final JsonObject config = (JsonObject) object;
                final TransportMapping transport = createTransportMapping(config);
                if (transport != null) {
                    this.mappings.put(transport, config);
                }
            }
        }
    }

    @Override
    public void processPdu(final CommandResponderEvent event) {
        if (event.getPDU().getType() == PDU.NOTIFICATION) {
            event.setProcessed(true);
        }
    }

    @Override
    public void handle(final Message<JsonObject> message) {
        final int type = getActionType(message);
        if (type == Integer.MIN_VALUE) {
            return;
        }
        final Target target = createTarget(message);
        if (target == null) {
            return;
        }
        final PDU pdu = createPdu(message, target.getVersion());
        pdu.setType(type);
        if (pdu.getType() == PDU.GETBULK) {
            pdu.setMaxRepetitions(message.body().getInteger("maxRepetition", this.config.getInteger("maxRepetition", 5)));
        }
        final ResponseListener listener = new ResponseListener() {

            @Override
            public void onResponse(final ResponseEvent event) {
                final Snmp snmp = (Snmp) event.getSource();
                snmp.cancel(event.getRequest(), this);
                final PDU response = event.getResponse();
                final Exception error = event.getError();
                if (error != null) {
                    sendError(message, error.getMessage());
                    return;
                }
                if (response == null) {
                    sendError(message, "None response");
                    return;
                }
                // to decode octet string and summarize variable binding field
                final PDU myPdu = new PDU(response) {
                    @Override
                    public Vector getVariableBindings() {
                        final Vector<Map> variableBindings1 = new Vector<Map>();
                        final Vector<VariableBinding> variableBindings2 = super.getVariableBindings();
                        for (final VariableBinding varBind2 : variableBindings2) {
                            final Map map = new HashMap();
                            map.put(varBind2.getOid().toString(), varBind2.getVariable().toString());
                            variableBindings1.add(map);
                        }
                        return variableBindings1;
                    }
                };

                sendOK(message, new JsonObject(Json.encode(myPdu)));
            }
        };
        try {
            this.snmp.send(pdu, target, null, listener);
        } catch (final IOException e) {
            sendError(message, e.getMessage());
        }
    }

    private int getActionType(final Message<JsonObject> message) {
        int type = Integer.MIN_VALUE;
        final String action = message.body().getString("action");
        if (action != null) {
            type = PDU.getTypeFromString(action.toUpperCase());
        }
        if (type == Integer.MIN_VALUE) {
            sendError(message, "Illegal action");
        }
        return type;
    }

    private Target createTarget(final Message<JsonObject> message) {
        Target target = null;
        final JsonObject config = getTargetConfig(message);
        if (config == null) {
            return target;
        }
        final Address address = GenericAddress.parse(config.getString("address"));
        final OctetString securityName = new OctetString(config.getString("securityName", config.getString("community", "public")));
        final int retries = config.getInteger("retries", 0);
        final int timeout = config.getInteger("timeout", 5000);
        int version = SnmpConstants.version2c;
        version = config.getInteger("version", version);
        target = createTarget(address, securityName);
        target.setRetries(retries);
        target.setVersion(version);
        target.setTimeout(timeout);
        return target;
    }

    private JsonObject getTargetConfig(final Message<JsonObject> message) {
        Object target = message.body().getValue("target");
        if (target instanceof String) {
            final JsonObject json = new JsonObject();
            json.putString("address", (String) target);
            target = json;
        }
        if (!(target instanceof JsonObject)) {
            sendError(message, "Illegal target");
            return null;
        }
        return (JsonObject) target;
    }

    private Target createTarget(final Address address, final OctetString community) {
        return new CommunityTarget(address, community);
    }

    private PDU createPdu(final Message<JsonObject> message, final int version) {
        final PDU pdu = DefaultPDUFactory.createPDU(version);
        final JsonArray items = getItems(message);
        for (int i = 0; i < items.size(); i++) {
            final Object item = items.get(i);
            final String oid = asOidString(item);
            final VariableBinding vb = new VariableBinding(new OID(oid));
            if (item instanceof JsonObject) {
                final JsonObject variable = ((JsonObject) item).getValue("variable");
                if (variable != null) {
                    vb.setVariable(VariableUtil.asVariable(variable));
                }
            }
            pdu.add(vb);
        }
        return pdu;
    }

    private JsonArray getItems(final Message<JsonObject> message) {
        JsonArray items = null;
        final Object object = message.body().getValue("items");
        if (!(object instanceof JsonArray)) {
            final JsonArray array = new JsonArray();
            array.add(object);
            items = array;
        } else {
            items = (JsonArray) object;
        }
        return items;
    }

    private String asOidString(final Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof String) {
            return ((String) object).replaceAll("^\\s*\\.|\\s", "");
        }
        if (object instanceof Number) {
            return String.valueOf(((Number) object).intValue());
        }
        if (object instanceof JsonArray) {
            final JsonArray array = (JsonArray) object;
            final StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (builder.length() > 0) {
                    builder.append(".");
                }
                builder.append(asOidString(array.get(i)));
            }
            return asOidString(builder);
        }
        if (object instanceof JsonObject) {
            return asOidString(((JsonObject) object).getValue("oid"));
        }
        return asOidString(object.toString());
    }
}
