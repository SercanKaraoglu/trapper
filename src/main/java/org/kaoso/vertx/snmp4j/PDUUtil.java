package org.kaoso.vertx.snmp4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.PDU;
import org.snmp4j.asn1.BER;

public class PDUUtil {
	private static final List<Field> PDU_TYPE_FIELDS = PDUUtil
			.getPduTypeFields();
	public static List<String> getPduTypeNames() {
		List<String> list = new ArrayList<>();
		for (Field field : getPduTypeFields()) {
			list.add(field.getName());
		}
		return list;
	}

	public static List<Field> getPduTypeFields() {
		List<Field> list = new ArrayList<>();
		Field[] fields = PDU.class.getDeclaredFields();
		for (Field field : fields) {
			if (isPduTypeField(field)) {
				list.add(field);
			}
		}
		return list;
	}

	public static int getPduTypeValue(String name)
			throws IllegalArgumentException, IllegalAccessException {
		return getPduTypeValue(name, PDU_TYPE_FIELDS);
	}
	
	public static int getPduTypeValue(String name, List<Field> fields)
			throws IllegalArgumentException, IllegalAccessException {
		return getPduTypeField(name, fields).getInt(null);
	}

	public static Field getPduTypeField(String name) {
		return getPduTypeField(name, PDU_TYPE_FIELDS);
	}
	
	public static Field getPduTypeField(String name, List<Field> fields) {
		name = name.toUpperCase();
		for (Field field : fields) {
			if (name.equals(field.getName())) {
				return field;
			}
		}
		return null;
	}
	
	public static boolean isPduTypeValue(int value) {
		int code = BER.ASN_CONTEXT | BER.ASN_CONSTRUCTOR;
		return (value & code) == code;
	}
	
	public static boolean isPduTypeField(String name) {
		return isPduTypeField(name, PDU_TYPE_FIELDS);
	}
	
	public static boolean isPduTypeField(String name, List<Field> fields) {
		return getPduTypeField(name, fields) != null;
	}

	private static boolean isPduTypeField(Field field) {
		if (!Modifier.isStatic(field.getModifiers())) {
			return false;
		}
		if (field.getType() != int.class) {
			return false;
		}
		try {
			return isPduTypeValue((Integer) field.get(null));
		} catch (Exception e) {
		}
		return false;
	}
}
