package net.openid.conformance.authzen.condition;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Checks the endpoints of the PDP metadata document as published, when it also carries
 * signed_metadata whose values take precedence. A PEP that ignores signed metadata (Section 9.1.3)
 * uses these values, so every endpoint present must be an https URL (Section 9.1.1).
 *
 * <p>Only policy_decision_point is required here. Whether a REQUIRED parameter such as
 * access_evaluation_endpoint may appear in the signed metadata alone is left open by Section 9.1.3;
 * {@link WarnPublishedMetadataOmitsAccessEvaluationEndpoint} reports its absence, and
 * {@link CheckPDPServerConfiguration} requires it once the signed values are applied.
 */
public class CheckPublishedPDPMetadataEndpoints extends CheckPDPServerConfiguration {

	@Override
	protected List<String> getRequiredEndpoints() {
		return ImmutableList.of("policy_decision_point");
	}

	@Override
	protected List<String> getOptionalEndpoints() {
		return ImmutableList.<String>builder()
			.add("access_evaluation_endpoint")
			.addAll(super.getOptionalEndpoints())
			.build();
	}
}
