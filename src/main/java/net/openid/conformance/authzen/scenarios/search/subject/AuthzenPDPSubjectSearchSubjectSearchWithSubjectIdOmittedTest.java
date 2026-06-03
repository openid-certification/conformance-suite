package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-search-with-subject-id-omitted",
	displayName = "Authzen Subject Search API - Section 8.4.1: Subject search with subject.id omitted",
	summary = "Section 8.4.1 the harness does not send subject.id in Subject Search requests. The PDP MUST accept the request and return at least alice and bob.\n" + AuthzenPDPSubjectSearchSubjectSearchWithSubjectIdOmittedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchSubjectSearchWithSubjectIdOmittedTest extends AbstractAuthzenPDPSubjectSearchTest {

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
