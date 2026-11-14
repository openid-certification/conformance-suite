package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-search-with-context",
	displayName = "AuthZEN Subject Search API - Section 8.4.1: Subject search with context",
	summary = "Section 8.4.1 subject search with the optional context field (certification scenario 7.2.2). The PDP MUST accept the request, and as context does not affect fixture decisions (scenario 4.6), the results MUST include alice and bob. The harness first sends the same search without the context, following every page, and fails if the fixture entities in the two result sets differ: scenario 7.2.2, as revised for openid/authzen#774, requires them to match, as the fixture decisions hold with or without a context (scenario 4.6). Results outside the fixture may differ, since scenario 4.6 lets a PDP have other policies a context may affect, so they are not compared.\n" + AuthzenPDPSubjectSearchSubjectSearchWithContextTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchSubjectSearchWithContextTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"context": {
				"time": "2025-06-27T18:03-07:00",
				"ip": "192.168.1.1"
			}
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

	@Override
	protected BaseSearch getBaseSearch() {
		// Certification scenario 4.6 guarantees the fixture decisions with or without a context,
		// and lets a PDP have other policies a context may affect. No section of the specification
		// requires the results to match and the scenario has no requirement prefix, so the
		// comparison cites none.
		return new BaseSearch("a context", request -> {
			request.remove("context");
			return request;
		}, true, List.of());
	}
}
