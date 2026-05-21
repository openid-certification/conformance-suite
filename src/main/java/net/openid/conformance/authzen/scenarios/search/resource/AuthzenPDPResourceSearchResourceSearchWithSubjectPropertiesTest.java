package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-search-with-subject-properties",
	displayName = "Authzen Resource Search API - Section 4.3.3: Resource search with subject properties -- fixture validated",
	summary = "Section 4.3.3 resource search with subject properties (validates rule 6). Records bob (admin) can write to MUST include at least record-2.\n" + AuthzenPDPResourceSearchResourceSearchWithSubjectPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPResourceSearchResourceSearchWithSubjectPropertiesTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob",
				"properties": { "role": "admin" }
			},
			"action": { "name": "write" },
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
					{ "type": "record", "id": "record-2" }
				]
			}
			""";
	}
}
