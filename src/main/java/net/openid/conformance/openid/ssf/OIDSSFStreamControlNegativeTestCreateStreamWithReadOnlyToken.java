package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs403;
import net.openid.conformance.openid.ssf.conditions.OIDSSFEnsureWwwAuthenticateHeaderPresent;
import net.openid.conformance.openid.ssf.conditions.OIDSSFRestrictClientScopeToRead;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFCreateStreamConditionSequence;
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
		 * obtain an access token for the 'ssf.read' scope only
		 * attempt to create a stream with that read-only token
		 * transmitter rejects the request with a 403 response
		 * the 403 response carries a Bearer 'WWW-Authenticate' challenge (insufficient_scope)
		""",
	profile = "OIDSSF"
)
// Requires OAuth-issued tokens with scopes; a pre-shared static token has no scope dimension.
@VariantNotApplicable(parameter = SsfAuthMode.class, values = "static")
public class OIDSSFStreamControlNegativeTestCreateStreamWithReadOnlyToken extends AbstractStreamControlErrorTest {

	@Override
	protected void prepareTransmitterAccess() {
		eventLog.runBlock("Fetch Transmitter Metadata", this::fetchTransmitterMetadata);

		eventLog.runBlock("Prepare read-only Transmitter Access", () -> {
			callAndStopOnFailure(OIDSSFRestrictClientScopeToRead.class, "CAEPIOP-2.7.3");
			obtainTransmitterAccessToken();
			OIDSSFRestrictClientScopeToRead.undo(env);
		});

		env.putString("ssf", "delivery_method", deliveryMode.getAlias());
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
			callAndContinueOnFailure(OIDSSFEnsureWwwAuthenticateHeaderPresent.class, Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2", "RFC6750-3.1");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
