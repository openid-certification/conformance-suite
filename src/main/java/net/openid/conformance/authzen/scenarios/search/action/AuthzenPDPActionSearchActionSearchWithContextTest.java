package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.List;

@PublishTestModule(
	testName = "authzen-pdp-action-search-action-search-with-context",
	displayName = "AuthZEN Action Search API - Section 8.6.1: Action search with context",
	summary = "Section 8.6.1 action search with the optional context field (certification scenario 7.4.2). The PDP MUST accept the request, and as context does not affect fixture decisions (scenario 4.6), the results MUST include read and write. The harness first sends the same search without the context, following every page, and warns if the fixture entities in the two result sets differ. Scenario 7.4.2 asks for identical results, but it rests that on scenario 4.6, which guarantees only the fixture decisions with or without a context and lets a PDP have other policies a context may affect, so results outside the fixture are not compared.\n" + AuthzenPDPActionSearchActionSearchWithContextTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchActionSearchWithContextTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "name": "read" },
					{ "name": "write" }
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
