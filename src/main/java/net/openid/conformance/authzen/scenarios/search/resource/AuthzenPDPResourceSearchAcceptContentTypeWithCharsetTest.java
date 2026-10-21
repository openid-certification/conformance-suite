package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-accept-content-type-with-charset",
	displayName = "AuthZEN Resource Search API - Section 10.1: Accept Content-Type with charset",
	summary = "Section 10.1 has the PEP send the Content-Type application/json; this request sends `application/json; charset=utf-8`. RFC 8259 Section 11 defines no charset parameter for application/json and says \"Adding one really has no effect on compliant recipients\", so the suite requires HTTP 200 and the expected response. Whether a PDP may reject the parameter is an open question for the working group.",
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchAcceptContentTypeWithCharsetTest extends AbstractAuthzenPDPResourceSearchTest {

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
	protected String getRequestContentTypeOverride() {
		return "application/json; charset=utf-8";
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
