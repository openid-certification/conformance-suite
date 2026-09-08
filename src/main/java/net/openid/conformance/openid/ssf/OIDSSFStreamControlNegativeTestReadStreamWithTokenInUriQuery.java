package net.openid.conformance.openid.ssf;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIsAnyOf;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFMoveAccessTokenToUriQueryOverride;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFReadStreamConfigCall;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "openid-ssf-stream-control-error-token-in-uri-query",
	displayName = "Attempt to read Stream Configuration with the access token in the URI query.",
	summary = """
		This test verifies that the transmitter refuses an access token presented as an
		'access_token' URI query parameter. The CAEP Interop Profile (2.7.2) says a transmitter
		MUST NOT accept access tokens via the URI query parameter mechanism of RFC 6750 2.3;
		a transmitter that honors the query parameter would leak tokens into logs and referrers.
		The testsuite expects to observe the following interactions:
		 * read an existing stream configuration, sending a valid access token only as an
		   'access_token' URI query parameter and no Authorization header
		 * transmitter rejects the request with a 400 or 401 response
		""",
	profile = "OIDSSF"
)
// Only meaningful with OAuth-issued tokens; static auth is not certifiable under the profile.
@VariantNotApplicable(parameter = SsfAuthMode.class, values = "static")
public class OIDSSFStreamControlNegativeTestReadStreamWithTokenInUriQuery extends AbstractStreamControlErrorTest {

	@Override
	protected void testTransmitter() {

		eventLog.runBlock("Attempt to read a Stream Configuration with the access token in the URI query", () -> {

			callAndStopOnFailure(OIDSSFMoveAccessTokenToUriQueryOverride.class, "CAEPIOP-2.7.2", "RFC6750-2.3");
			callAndContinueOnFailure(OIDSSFReadStreamConfigCall.class, Condition.ConditionResult.FAILURE, "OIDSSF-8.1.1.2");
			OIDSSFMoveAccessTokenToUriQueryOverride.undo(env);
			call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
			// the request carries no usable credentials, so 401 is expected; 400 is accepted
			// for transmitters that treat the unexpected query parameter as a bad request
			callAndContinueOnFailure(new EnsureHttpStatusCodeIsAnyOf(400, 401), Condition.ConditionResult.FAILURE, "CAEPIOP-2.7.2");
			call(exec().unmapKey("endpoint_response"));
		});
	}
}
