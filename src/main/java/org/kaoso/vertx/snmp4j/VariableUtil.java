package org.kaoso.vertx.snmp4j;

import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.AssignableFromByteArray;
import org.snmp4j.smi.AssignableFromIntArray;
import org.snmp4j.smi.AssignableFromInteger;
import org.snmp4j.smi.AssignableFromLong;
import org.snmp4j.smi.AssignableFromString;
import org.snmp4j.smi.Variable;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

public class VariableUtil {
	public static Variable asVariable(JsonObject object) {
		int syntax;
		Object type = object.getValue("type");
		if (type instanceof String) {
			syntax = AbstractVariable.getSyntaxFromString((String) type);
		} else if (type instanceof Number) {
			syntax = ((Number) type).intValue();
		} else {
			return null;
		}
		Variable variable = null;
		try {
			variable = AbstractVariable.createFromSyntax(syntax);
		} catch (Exception e) {
			return variable;
		}
		Object value = object.getValue("value");
		if (value == null) {
			return variable;
		}
		if (variable instanceof AssignableFromByteArray) {
			if (value instanceof String) {
				setValue((AssignableFromByteArray) variable, (String) value);
			} else if (value instanceof JsonArray) {
				setValue((AssignableFromByteArray) variable, (JsonArray) value);
			}
		} else if (variable instanceof AssignableFromIntArray) {
			if (value instanceof String) {
				setValue((AssignableFromIntArray) variable, (String) value);
			} else if (value instanceof JsonArray) {
				setValue((AssignableFromIntArray) variable, (JsonArray) value);
			}
		} else if (variable instanceof AssignableFromInteger) {
			setValue((AssignableFromInteger) variable, value);
		} else if (variable instanceof AssignableFromLong) {
			setValue((AssignableFromLong) variable, value);
		} else if (variable instanceof AssignableFromString) {
			if (value instanceof String) {
				setValue((AssignableFromString) variable, (String) value);
			} else if (value instanceof JsonArray) {
				setValue((AssignableFromString) variable, (JsonArray) value);
			}
		}
		return variable;
	}

	public static boolean setValue(AssignableFromByteArray variable,
			String value) {
		byte[] bytes = Base64.decode((String) value);
		if (bytes != null && bytes.length > 0) {
			try {
				((AssignableFromByteArray) variable).setValue(bytes);
				return true;
			} catch (Exception e) {
				if (variable instanceof AssignableFromString) {
					return setValue((AssignableFromString) variable, value);
				}
			}
		}
		return false;
	}

	public static boolean setValue(AssignableFromByteArray variable,
			JsonArray value) {
		try {
			byte[] bytes = asByteArray(value);
			variable.setValue(bytes);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromString variable, String value) {
		try {
			variable.setValue(value);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromString variable,
			JsonArray value) {
		try {
			variable.setValue(asString(value));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromIntArray variable, String value) {
		try {
			variable.setValue(asIntArray(value));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromIntArray variable,
			String value, String regex) {
		try {
			variable.setValue(asIntArray(value, regex));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromIntArray variable,
			JsonArray value) {
		try {
			int[] ints = asIntArray(value);
			variable.setValue(ints);
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromInteger variable, Object value) {
		try {
			variable.setValue(asInteger(value));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean setValue(AssignableFromLong variable, Object value) {
		try {
			variable.setValue(asLong(value));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public static byte[] asByteArray(JsonArray array) {
		byte[] bytes = new byte[array.size()];
		for (int i = 0; i < bytes.length; i++) {
			Integer n = asInteger(array.get(i));
			if (n != null) {
				bytes[i] = n.byteValue();
			}
		}
		return bytes;
	}

	public static int[] asIntArray(String value) {
		int s = -1, e = -1;
		for (int i = 0; i < value.length(); i++) {
			if (s == -1) {
				s = e = !Character.isDigit(value.charAt(i)) ? i : s;
			} else if (Character.isDigit(value.charAt(i))) {
				e = i;
				break;
			}
		}
		if (s == e) {
			return new int[] { asInteger(value) };
		}
		return asIntArray(value, value.substring(s, e));
	}

	public static int[] asIntArray(String value, String regex) {
		String[] items = value.split(regex);
		int[] ints = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			ints[i] = asInteger(items[i]);
		}
		return ints;
	}

	public static int[] asIntArray(JsonArray array) {
		int[] ints = new int[array.size()];
		for (int i = 0; i < ints.length; i++) {
			Integer n = asInteger(array.get(i));
			if (n != null) {
				ints[i] = n.intValue();
			}
		}
		return ints;
	}

	public static Integer asInteger(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		String[] items = value.toString().split("@");
		int radix = Integer.valueOf(items.length > 1 ? items[1] : "10");
		return Integer.valueOf(items[0], radix);
	}

	public static Long asLong(Object value) {
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		String[] items = value.toString().split("@");
		int radix = Integer.valueOf(items.length > 1 ? items[1] : "10");
		return Long.valueOf(items[0], radix);
	}

	public static String asString(JsonArray value) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < value.size(); i++) {
			Object object = value.get(i);
			if (object == null) {
				builder.append((char) 0);
			} else if (object instanceof String) {
				builder.append((String) object);
			} else if (object instanceof Number) {
				builder.append(((Number) object).intValue());
			}
		}
		return builder.toString();
	}
}
