package net.openid.conformance.logging;

import java.util.HashMap;
import java.util.Map;

public class MapCopy {

	public static Map<String, Object> deepCopy(Map<String, Object> messageMap) {
		Map<String, Object> copy = new HashMap<>();
		copyElements(messageMap, copy);
		return copy;
	}

	private static void copyElements(Map<String, Object> target, Map<String, Object> copy) {
		for(Map.Entry<String, Object> entry: target.entrySet()) {
			if(entry.getValue() instanceof Map) {
				Map<String, Object> newMap = new HashMap<>();
				copy.put(entry.getKey(), newMap);
				copyElements((Map<String, Object>) entry.getValue(), newMap);
			} else {
				copy.put(entry.getKey(), entry.getValue());
			}
		}
	}

}
