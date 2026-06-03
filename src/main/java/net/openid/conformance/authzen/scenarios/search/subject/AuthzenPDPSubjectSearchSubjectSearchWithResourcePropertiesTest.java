package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-search-with-resource-properties",
	displayName = "Authzen Subject Search API - Section 8.4.1: Subject search with resource properties -- fixture validated",
	summary = "Section 8.4.1 subject search with resource properties (validates rule 6). Searches for users who can write to an archived record; results MUST include at least bob.\n" + AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPSubjectSearchSubjectSearchWithResourcePropertiesTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "write" },
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
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}
}
