package net.openid.conformance.authzen.condition;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers AbstractSetAuthzenApiEndpoint through its subclasses: a configured endpoint is used as
 * is, and an empty one falls back to the Section 10.1 default path under the PDP Identifier.
 */
@ExtendWith(MockitoExtension.class)
class AbstractSetAuthzenApiEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private String endpointFor(AbstractCondition cond, String pdp) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env.putObject("pdp", JsonParser.parseString(pdp).getAsJsonObject());
		cond.execute(env);
		return env.getString("authzen_api_endpoint");
	}

	@Test
	public void configuredEndpoint_isUsed() {
		assertEquals("https://pdp.example.com/custom/subjects",
			endpointFor(new SetAuthzenApiEndpointToSubjectSearchEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com",
				  "search_subject_endpoint": "https://pdp.example.com/custom/subjects" }"""));
	}

	@Test
	public void emptyEndpoint_usesDefaultPath() {
		assertEquals("https://pdp.example.com/access/v1/evaluation",
			endpointFor(new SetAuthzenApiEndpointToAccessEvaluationEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com" }"""));
		assertEquals("https://pdp.example.com/access/v1/evaluations",
			endpointFor(new SetAuthzenApiEndpointToAccessEvaluationsEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com" }"""));
		assertEquals("https://pdp.example.com/access/v1/search/resource",
			endpointFor(new SetAuthzenApiEndpointToResourceSearchEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com" }"""));
		assertEquals("https://pdp.example.com/access/v1/search/action",
			endpointFor(new SetAuthzenApiEndpointToActionSearchEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com" }"""));
	}

	@Test
	public void defaultPath_keepsTheIdentifierPathAndAvoidsADoubleSlash() {
		assertEquals("https://pdp.example.com/tenant1/access/v1/search/subject",
			endpointFor(new SetAuthzenApiEndpointToSubjectSearchEndpoint(), """
				{ "policy_decision_point": "https://pdp.example.com/tenant1/" }"""));
	}

	@Test
	public void noEndpointAndNoIdentifier_fails() {
		assertThrows(ConditionError.class, () -> endpointFor(new SetAuthzenApiEndpointToSubjectSearchEndpoint(), """
			{ }"""));
	}
}
