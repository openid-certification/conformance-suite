package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ValidateVpTokenPresentationCountMatchesDcqlQuery_UnitTest {

	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateVpTokenPresentationCountMatchesDcqlQuery cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateVpTokenPresentationCountMatchesDcqlQuery();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	private void setDcqlQuery(String credentialEntryJson) {
		env.putObjectFromJsonString("dcql_query", """
			{
			  "credentials": [ %s ]
			}
			""".formatted(credentialEntryJson));
	}

	private void setVpToken(String vpTokenJson) {
		env.putObjectFromJsonString("authorization_endpoint_response", "vp_token", vpTokenJson);
	}

	@Test
	public void testEvaluate_multipleOmittedOnePresentation() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt" }
			""");
		setVpToken("""
			{ "my_credential": [ "eyJhbGci...QMA" ] }
			""");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_multipleOmittedTwoPresentations() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt" }
			""");
		setVpToken("""
			{ "my_credential": [ "eyJhbGci...QMA", "eyJhbGci...QMB" ] }
			""");

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));

		assertTrue(e.getMessage().contains("exactly one"),
			"failure message should say exactly one presentation is permitted, was: " + e.getMessage());
	}

	@Test
	public void testEvaluate_multipleFalseTwoPresentations() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt", "multiple": false }
			""");
		setVpToken("""
			{ "my_credential": [ "eyJhbGci...QMA", "eyJhbGci...QMB" ] }
			""");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_multipleTrueThreePresentations() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt", "multiple": true }
			""");
		setVpToken("""
			{ "my_credential": [ "eyJhbGci...QMA", "eyJhbGci...QMB", "eyJhbGci...QMC" ] }
			""");

		cond.execute(env);
	}

	/**
	 * An entry whose key doesn't match any Credential Query in the DCQL query is reported by
	 * ValidateVpTokenCredentialIdMatchesDcqlQuery; this condition must not double-report it.
	 */
	@Test
	public void testEvaluate_unknownCredentialIdSkipped() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt" }
			""");
		setVpToken("""
			{ "other_credential": [ "eyJhbGci...QMA", "eyJhbGci...QMB" ] }
			""");

		cond.execute(env);
	}

	/**
	 * A bare-string value (wallet failed to wrap the presentation in an array) is reported by
	 * ExtractVP1FinalVpTokenDCQL; this condition must not fail on it.
	 */
	@Test
	public void testEvaluate_bareStringValueSkipped() {
		setDcqlQuery("""
			{ "id": "my_credential", "format": "dc+sd-jwt" }
			""");
		setVpToken("""
			{ "my_credential": "eyJhbGci...QMA" }
			""");

		cond.execute(env);
	}

}
