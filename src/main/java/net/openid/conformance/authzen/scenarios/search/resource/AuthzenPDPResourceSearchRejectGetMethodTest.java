package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-reject-get-method",
	displayName = "AuthZEN Resource Search API - Section 10.1: Reject GET method",
	summary = "Per Section 10.1, resource search requests are made via HTTPS POST. The PDP is expected to reject a GET request to the resource search endpoint with HTTP 400, 404 or 405 (typically 405 Method Not Allowed). Section 10.1 places the POST requirement on the request rather than on the PDP, so accepting the request is reported as a warning.",
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchRejectGetMethodTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return "{}";
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getRequestHttpMethod() {
		return "GET";
	}

	@Override
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		// Accept the same 4xx set as the reject-PUT tests: a PDP whose route is
		// registered for POST only may 404 an unknown verb just as plausibly for
		// GET as for PUT, so the two negative tests stay symmetric.
		return java.util.Set.of(400, 404, 405);
	}

	@Override
	protected ConditionResult getUnexpectedHttpStatusSeverity() {
		return ConditionResult.WARNING;
	}
}
