package net.openid.conformance.authzen.condition;

import java.util.Set;

/**
 * Action Search responses: each result is an Action (Section 5.3), which has name and properties.
 */
public class WarnAuthzenActionSearchResponseUnknownMembers extends AbstractWarnAuthzenSearchResponseUnknownMembers {

	@Override
	protected Set<String> getEntityMembers() {
		return Set.of("name", "properties");
	}
}
