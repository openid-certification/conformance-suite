package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Section 9.2.2: the metadata response "contains a set of metadata parameters as defined in the
 * AuthZEN Policy Decision Point Metadata Registry", whose initial registrations (Section 12.1.3)
 * are the parameters below. A name outside them is often a misspelling, such as
 * supported_capabilities for capabilities. Reads the PDP metadata in {@code pdp}, after any
 * signed_metadata values have been applied, so it covers both.
 */
public class WarnAuthzenMetadataUnknownMembers extends AbstractWarnAuthzenUnknownMembers {

	private static final Set<String> REGISTERED_METADATA = Set.of(
		"policy_decision_point",
		"access_evaluation_endpoint",
		"access_evaluations_endpoint",
		"search_subject_endpoint",
		"search_resource_endpoint",
		"search_action_endpoint",
		"capabilities",
		"signed_metadata");

	@Override
	@PreEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {
		Map<String, Set<String>> unknown = newUnknownMembers();
		collectUnknownMembers(env.getObject("pdp"), "$", REGISTERED_METADATA, unknown);
		return reportUnknownMembers(env, unknown, "PDP metadata");
	}
}
