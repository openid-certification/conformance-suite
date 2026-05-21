package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-action-search-action-search-with-properties",
	displayName = "Authzen Action Search API - Section 4.4.3: Action search with properties -- fixture validated",
	summary = "Section 4.4.3 action search with properties (validates rule 6). Actions an admin can perform on an archived record MUST include at least write.\n" + AuthzenPDPActionSearchActionSearchWithPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPActionSearchActionSearchWithPropertiesTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob",
				"properties": { "role": "admin" }
			},
			"resource": {
				"type": "record",
				"id": "record-2",
				"properties": { "status": "archived" }
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
					{ "name": "write" }
				]
			}
			""";
	}
}
