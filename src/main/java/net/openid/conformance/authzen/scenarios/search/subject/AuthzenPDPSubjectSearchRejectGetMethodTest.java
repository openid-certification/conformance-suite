package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-reject-get-method",
	displayName = "Authzen Subject Search API - Section 10.1: Reject GET method",
	summary = "Per Section 10.1, subject search requests are made via HTTPS POST. The PDP MUST reject a GET request to the subject search endpoint with an HTTP 4xx error (typically 405 Method Not Allowed).",
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchRejectGetMethodTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
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
		return java.util.Set.of(400, 405);
	}
}
