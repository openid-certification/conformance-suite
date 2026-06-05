package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-subject-missing-id",
	displayName = "Authzen Resource Search API - Section 10.1.1: Subject missing id -- expect HTTP 400",
	summary = "Section 10.1.1 missing required sub-field. Resource Search subject omits `id`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchSubjectMissingIdTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchSubjectMissingIdTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
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
		return "{ \"results\": [] }";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}
}
