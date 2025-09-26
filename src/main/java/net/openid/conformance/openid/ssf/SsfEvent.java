package net.openid.conformance.openid.ssf;

import java.util.Map;
import java.util.Set;

public record SsfEvent(String type, Map<String, Object> data, Set<String> requirements) {
}
