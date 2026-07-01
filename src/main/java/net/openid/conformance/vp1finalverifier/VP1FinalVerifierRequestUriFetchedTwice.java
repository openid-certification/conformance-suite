package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-request-uri-fetched-twice",
	displayName = "OID4VP-1.0-FINAL Verifier: request_uri fetched more than once",
	summary = """
		Tests that the verifier serves its request_uri when it is fetched more than once. OID4VP 1.0 \
		Final does not require request_uri to be single-use, so a wallet may re-fetch the Request \
		Object (for example after a retry, a UI reload, or background re-validation). After the normal \
		first fetch, this test fetches and processes the request_uri a second time and expects the \
		verifier to serve the Request Object again. The test is not applicable when the verifier sends \
		the authorization request by value (request_method=url_query).""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
@VariantNotApplicable(parameter = VP1FinalVerifierRequestMethod.class, values = {"url_query"})
public class VP1FinalVerifierRequestUriFetchedTwice extends AbstractVP1FinalVerifierTest {

	@Override
	protected void fetchAndProcessRequestUri() {
		// Normal first fetch + validation, exactly as the happy flow does; this feeds the rest of the
		// verifier flow.
		super.fetchAndProcessRequestUri();

		// Fetch and process the request_uri a second time and confirm the verifier still serves the
		// Request Object (it must not treat request_uri as single-use). A single-use verifier that
		// returns a non-2xx response, or a body that no longer parses as the Request Object, fails
		// this second fetch.
		//
		// This is logged as a plain entry rather than a nested startBlock/endBlock: the event log uses
		// a single flat block id (see TestInstanceEventLog), so a nested block here would clobber the
		// enclosing "authorization endpoint" block opened by handleAuthorizationEndpointRequest().
		eventLog.log(getName(), "Fetching and processing the request_uri a second time to confirm the "
			+ "verifier does not treat it as single-use");
		super.fetchAndProcessRequestUri();
	}
}
