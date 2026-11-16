package net.openid.conformance.authzen.condition;

import java.util.Set;

/**
 * Subject and Resource Search responses: each result is a Subject (Section 5.1) or a Resource
 * (Section 5.2), both of which have type, id and properties.
 */
public class WarnAuthzenEntitySearchResponseUnknownMembers extends AbstractWarnAuthzenSearchResponseUnknownMembers {

	@Override
	protected Set<String> getEntityMembers() {
		return Set.of("type", "id", "properties");
	}
}
