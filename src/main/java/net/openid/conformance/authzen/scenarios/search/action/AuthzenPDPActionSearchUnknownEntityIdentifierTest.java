package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-unknown-entity-identifier",
	displayName = "Authzen Action Search API - Section 4.6.1: Unknown entity identifier",
	summary = "Section 4.6.1 unknown entity identifier. The PDP MUST return an empty results array (not an error) when an identifier is unknown.\n" + AuthzenPDPActionSearchUnknownEntityIdentifierTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchUnknownEntityIdentifierTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "nonexistent-user" },
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
				"results": []
			}
			""";
	}
}
