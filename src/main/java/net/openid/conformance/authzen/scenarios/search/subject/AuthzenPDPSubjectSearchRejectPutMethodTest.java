package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-reject-put-method",
	displayName = "AuthZEN Subject Search API - Section 10.1: Reject PUT method",
	summary = "Per Section 10.1, subject search requests are made via HTTPS POST. The PDP is expected to reject a PUT request to the subject search endpoint with HTTP 400, 404 or 405 (typically 405 Method Not Allowed). Section 10.1 places the POST requirement on the request rather than on the PDP, so accepting the request is reported as a warning.",
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchRejectPutMethodTest extends AbstractAuthzenPDPSubjectSearchTest {

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
		return "PUT";
	}

	@Override
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400, 404, 405);
	}

	@Override
	protected ConditionResult getUnexpectedHttpStatusSeverity() {
		return ConditionResult.WARNING;
	}
}
