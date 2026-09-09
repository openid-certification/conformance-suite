package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs204Or404;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs403;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureGrantedScopeIsReadOnly;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.OIDSSFRestrictClientScopeToRead;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFDeleteStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-create-stream-with-read-only-token",
	displayName = "Attempt to create a Stream with a read-only access token.",
	summary = """
		This test verifies that the transmitter enforces OAuth scopes on the Stream Management
		API. The CAEP Interop Profile (2.7.3) grants Create Stream only to the 'ssf.manage'
		scope, and (2.7.2) requires the transmitter to verify that the authorization represented
		by the access token is sufficient and to return an RFC 6750 3.1 error otherwise.
		The testsuite expects to observe the following interactions:
		 * obtain an access token for the 'ssf.read' scope only; if the authorization server
		   grants 'ssf.manage' anyway (RFC 6749 section 3.3 allows it) the test stops, since
		   the scope enforcement cannot be exercised with such a token
		 * attempt to create a stream with that read-only token
		 * transmitter rejects the request with a 403 response
		 * the 403 response should carry a Bearer 'WWW-Authenticate' challenge (RFC 6750
		   section 3, insufficient_scope per section 3.1; reported as a warning if absent,
		   since CAEP Interop 2.7.2 only cites RFC 6750 section 3.1)
		 * if the transmitter created the stream regardless, it is deleted again with a
		   full-scope token
		""",
	profile = "OIDSSF"
)
// Requires OAuth-issued tokens with scopes; a pre-shared static token has no scope dimension.
@VariantNotApplicable(parameter = SsfAuthMode.class, values = "static")
public class OIDSSFStreamControlNegativeTestCreateStreamWithReadOnlyToken extends AbstractStreamControlErrorTest {

	/**
	 * Whether the next token request is narrowed to {@code ssf.read}. Cleared for the
	 * full-scope token used to delete a stream the transmitter created regardless.
	 */
	private boolean restrictScopeToRead = true;

	@Override
	protected void prepareTransmitterAccess() {
		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Prepare read-only Transmitter Access", () -> {
			obtainTransmitterAccessToken();
			OIDSSFRestrictClientScopeToRead.undo(env);
			// RFC 6749 3.3 lets the authorization server ignore the requested scope; with a
			// token that also carries 'ssf.manage' the transmitter's refusal cannot be tested.
			callAndStopOnFailure(OIDSSFEnsureGrantedScopeIsReadOnly.class, "CAEPIOP-2.7.3");
		});

		env.putString("ssf", "delivery_method", deliveryMode.getAlias());
	}

	@Override
	protected void onClientConfigurationObtained() {
		// must run after the client configuration load - earlier changes to the client's
		// scope would be overwritten by GetStaticClientConfiguration
		if (restrictScopeToRead) {
			callAndStopOnFailure(OIDSSFRestrictClientScopeToRead.class, "CAEPIOP-2.7.3");
		}
	}

	@Override
	protected void beforeTestTransmitter() {
		// the read-only token cannot delete streams, so skip the usual cleanup call
	}

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Attempt to create a Stream Configuration with a read-only access token", () -> {

			call(sequence(OIDSSFCreateStreamConditionSequence.class));
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs403.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "CAEPIOP-2.7.3");
			// WARNING: the WWW-Authenticate MUST is RFC 6750 section 3; CAEPIOP 2.7.2 only
			// cites section 3.1 (the error codes), so the profile's normative chain to the
			// header is imprecise - see the condition's javadoc. The 403 above is the
			// FAILURE-level check.
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.WARNING, "CAEPIOP-2.7.2", "RFC6750-3", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});

		Integer createStatus = env.getInteger("resource_endpoint_response_full", "status");
		if (createStatus != null && createStatus == 201) {
			// The FAILURE is already recorded above; don't leave the stream behind on the
			// transmitter, where it would collide with the next module's create request.
			eventLog.runBlock("Delete the stream the transmitter created for a read-only token", () -> {
				restrictScopeToRead = false;
				obtainTransmitterAccessToken();
				callAndContinueOnFailure(OIDSSFDeleteStreamConfigCall.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.1.5");
				call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
				callAndContinueOnFailure(EnsureHttpStatusCodeIs204Or404.class, Condition.ConditionResult.INFO, "OIDSSF-8.1.1.5");
				call(exec().unmapKey("endpoint_response"));
			});
		}
	}
}
