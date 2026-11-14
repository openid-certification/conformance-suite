package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-search-with-context",
	displayName = "AuthZEN Resource Search API - Section 8.5.1: Resource search with context",
	summary = "Section 8.5.1 resource search with the optional context field (certification scenario 7.3.2). The PDP MUST accept the request, and as context does not affect fixture decisions (scenario 4.6), the results MUST include record-1 and record-2. The harness first sends the same search without the context, following every page, and fails if the fixture entities in the two result sets differ: scenario 7.3.2, as revised for openid/authzen#774, requires them to match, as the fixture decisions hold with or without a context (scenario 4.6). Results outside the fixture may differ, since scenario 4.6 lets a PDP have other policies a context may affect, so they are not compared.\n" + AuthzenPDPResourceSearchResourceSearchWithContextTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchResourceSearchWithContextTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
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
					{ "type": "record", "id": "record-1" },
					{ "type": "record", "id": "record-2" }
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
