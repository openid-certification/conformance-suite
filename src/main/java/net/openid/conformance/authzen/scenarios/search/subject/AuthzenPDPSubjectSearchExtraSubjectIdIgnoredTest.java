package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-extra-subject-id-ignored",
	displayName = "Authzen Subject Search API - Section 8.4.1: Extra subject.id ignored",
	summary = "Section 8.4.1 says subject.id SHOULD be omitted from Subject Search and SHOULD be ignored if sent. This test sends a stray `subject.id` alongside the required `subject.type`; PDP MUST still return the expected results.\n" + AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchExtraSubjectIdIgnoredTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "ignored-stray-id" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}
}
