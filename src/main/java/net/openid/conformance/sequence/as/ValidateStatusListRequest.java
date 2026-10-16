package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.EnsureRevocationListRequestHasOnlyDefinedQueryParameters;
import net.openid.conformance.condition.client.EnsureIncomingRequestBodyIsEmpty;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsGet;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Checks how a Relying Party asked an emulated Status Provider for a Token Status List, or for
 * an identifier list - ISO/IEC 18013-5 12.3.6.3 has an MSO revocation list of either mechanism
 * implemented according to the Token Status List specification. The request is the one mapped to
 * {@code incoming_request}.
 *
 * <p>The checks are warnings: draft-ietf-oauth-status-list section 8.1 places its requirement to
 * serve the list in response to a GET on the Status Provider, so a Relying Party that asks in
 * some other way is interoperating poorly rather than breaking a requirement placed on it.
 */
public class ValidateStatusListRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(EnsureIncomingRequestMethodIsGet.class, ConditionResult.WARNING, "OTSL-8.1");
		callAndContinueOnFailure(EnsureRevocationListRequestHasOnlyDefinedQueryParameters.class, ConditionResult.WARNING, "OTSL-8.4");
		callAndContinueOnFailure(EnsureIncomingRequestBodyIsEmpty.class, ConditionResult.WARNING, "OTSL-8.1");
	}
}
