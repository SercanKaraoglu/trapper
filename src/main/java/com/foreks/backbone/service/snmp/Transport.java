package com.foreks.backbone.service.snmp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.TransportMappings;
import org.vertx.java.core.json.JsonObject;

public class Transport {
    private static final List<Field> CONFIGURABLE_FIELDS = getConfigurableFields();
    private boolean                  getEnabled;
    private boolean                  getNextEnabled;
    private boolean                  responseEnabled;
    private boolean                  setEnabled;
    private boolean                  v1trapEnabled       = true;
    private boolean                  getBulkEnabled;
    private boolean                  informEnabled       = true;
    private boolean                  trapEnabled         = true;
    private boolean                  notificationEnabled = true;
    private boolean                  reportEnabled;
    private TransportMapping         mapping;

    public static Transport create(final JsonObject config) throws IOException {
        final Transport transport = new Transport();
        final Address address = GenericAddress.parse(config.getString("address", "udp:localhost/161"));
        final TransportMapping mapping = createTransportMapping(address);
        transport.setMapping(mapping);
        for (final String name : config.getFieldNames()) {
            transport.setEnabled(name, config.getBoolean(name));
        }
        return transport;
    }

    public static TransportMapping createTransportMapping(final Address address) throws IOException {
        return createTransportMapping(address, true);
    }

    public static TransportMapping createTransportMapping(final Address address, final boolean asyncMsgProcessingSupported) throws IOException {
        final TransportMapping transport = TransportMappings.getInstance().createTransportMapping(address);
        if (transport instanceof AbstractTransportMapping) {
            ((AbstractTransportMapping) transport).setAsyncMsgProcessingSupported(asyncMsgProcessingSupported);
        }
        return transport;
    }

    public static Method getConfigurableGetter(final String name) throws NoSuchMethodException, SecurityException {
        return getGetter(Transport.class, name, "is");
    }

    public static Method getGetter(final Class<?> clazz, String name, String prefix) throws NoSuchMethodException, SecurityException {
        prefix = prefix != null ? prefix : "get";
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        name = prefix + name;
        return clazz.getDeclaredMethod(name);
    }

    public static Method getConfigurableSetter(final String name) throws NoSuchMethodException, SecurityException {
        return getSetter(Transport.class, name, boolean.class);
    }

    public static Method getSetter(final Class<?> clazz, String name, final Class<?> type) throws NoSuchMethodException, SecurityException {
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        name = "set" + name;
        return clazz.getDeclaredMethod(name, type);
    }

    public static List<Field> getConfigurableFields() {
        final List<Field> list = new ArrayList<>();
        for (final Field field : Transport.class.getDeclaredFields()) {
            if (isConfigurableField(field)) {
                list.add(field);
            }
        }
        return list;
    }

    public static Field getConfigurableField(final String name) {
        for (final Field field : CONFIGURABLE_FIELDS) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    public static Field getConfigurableField(final String name, final boolean ignore) {
        if (!ignore) {
            return getConfigurableField(name);
        }
        for (final Field field : CONFIGURABLE_FIELDS) {
            if (name.equalsIgnoreCase(field.getName())) {
                return field;
            }
        }
        return null;
    }

    public static boolean isConfigurableField(final String name) {
        return getConfigurableField(name) != null;
    }

    private static boolean isConfigurableField(final Field field) {
        final int modifiers = field.getModifiers();
        return field.getName().endsWith("Enabled")
                && Modifier.isPrivate(modifiers)
                && !Modifier.isStatic(modifiers)
                && field.getType() == boolean.class;
    }

    public TransportMapping getMapping() {
        return this.mapping;
    }

    public boolean isEnabled(final String name) {
        final Field field = getConfigurableField(name + "Enabled", true);
        if (field == null) {
            return false;
        }
        try {
            return (boolean) field.get(this);
        } catch (final Exception e) {
        }
        return false;
    }

    public void setEnabled(final String name, final boolean enabled) {
        final Field field = getConfigurableField(name + "Enabled", true);
        if (field != null) {
            try {
                field.set(this, enabled);
            } catch (final Exception e) {
            }
        }
    }

    public void setMapping(final TransportMapping mapping) {
        this.mapping = mapping;
    }

    public boolean isGetEnabled() {
        return this.getEnabled;
    }

    public void setGetEnabled(final boolean getEnabled) {
        this.getEnabled = getEnabled;
    }

    public boolean isGetNextEnabled() {
        return this.getNextEnabled;
    }

    public void setGetNextEnabled(final boolean getNextEnabled) {
        this.getNextEnabled = getNextEnabled;
    }

    public boolean isResponseEnabled() {
        return this.responseEnabled;
    }

    public void setResponseEnabled(final boolean responseEnabled) {
        this.responseEnabled = responseEnabled;
    }

    public boolean isSetEnabled() {
        return this.setEnabled;
    }

    public void setSetEnabled(final boolean setEnabled) {
        this.setEnabled = setEnabled;
    }

    public boolean isV1trapEnabled() {
        return this.v1trapEnabled;
    }

    public void setV1trapEnabled(final boolean v1trapEnabled) {
        this.v1trapEnabled = v1trapEnabled;
    }

    public boolean isGetBulkEnabled() {
        return this.getBulkEnabled;
    }

    public void setGetBulkEnabled(final boolean getBulkEnabled) {
        this.getBulkEnabled = getBulkEnabled;
    }

    public boolean isInformEnabled() {
        return this.informEnabled;
    }

    public void setInformEnabled(final boolean informEnabled) {
        this.informEnabled = informEnabled;
    }

    public boolean isTrapEnabled() {
        return this.trapEnabled;
    }

    public void setTrapEnabled(final boolean trapEnabled) {
        this.trapEnabled = trapEnabled;
        this.notificationEnabled = trapEnabled;
    }

    public boolean isNotificationEnabled() {
        return this.notificationEnabled;
    }

    public void setNotificationEnabled(final boolean notificationEnabled) {
        this.trapEnabled = notificationEnabled;
        this.notificationEnabled = notificationEnabled;
    }

    public boolean isReportEnabled() {
        return this.reportEnabled;
    }

    public void setReportEnabled(final boolean reportEnabled) {
        this.reportEnabled = reportEnabled;
    }
}
