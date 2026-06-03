package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-search-with-resource-id-omitted",
	displayName = "Authzen Resource Search API - Section 8.5.1: Resource search with resource.id omitted",
	summary = "Section 8.5.1 the harness does not send resource.id in Resource Search requests. The PDP MUST accept the request and return records including at least record-1.\n" + AuthzenPDPResourceSearchResourceSearchWithResourceIdOmittedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchResourceSearchWithResourceIdOmittedTest extends AbstractAuthzenPDPResourceSearchTest {

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
		return """
			{
				"results": [
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
