package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
public class VCIEnsureLocationsMatchesCredentialIssuerWhenAuthorizationServersAdvertised_UnitTest {

	private static final String ISSUER = "https://issuer.example.com";
	private static final String CONFIG_ID = "oid4vc_natural_person_sd";

	private VCIEnsureLocationsMatchesCredentialIssuerWhenAuthorizationServersAdvertised cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIEnsureLocationsMatchesCredentialIssuerWhenAuthorizationServersAdvertised();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		env.putString("credential_issuer", ISSUER);
	}

	private void givenAuthorizationServersAdvertised(boolean advertised) {
		JsonObject metadata = new JsonObject();
		if (advertised) {
			metadata.add("authorization_servers", JsonParser.parseString("[\"" + ISSUER + "\"]"));
		}
		env.putObject("credential_issuer_metadata", metadata);
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject req = new JsonObject();
		req.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("effective_authorization_endpoint_request", req);
	}

	@Test
	public void testEvaluate_locationsMatchesIssuer_succeeds() {
		givenAuthorizationServersAdvertised(true);
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":[\"" + ISSUER + "\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_authorizationServersNotAdvertised_skipsCheck() {
		givenAuthorizationServersAdvertised(false);
		// Even with missing locations, the check skips because the rule is conditional.
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_locationsMissing_fails() {
		givenAuthorizationServersAdvertised(true);
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("missing 'locations'"));
	}

	@Test
	public void testEvaluate_locationsDoesNotMatchIssuer_fails() {
		givenAuthorizationServersAdvertised(true);
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":[\"https://elsewhere.example.com\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("does not equal the Credential Issuer Identifier"));
	}

	@Test
	public void testEvaluate_locationsWithMultipleEntries_fails() {
		givenAuthorizationServersAdvertised(true);
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":[\"" + ISSUER + "\",\"https://elsewhere.example.com\"]}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("exactly the Credential Issuer Identifier"));
	}

	@Test
	public void testEvaluate_nonOpenidCredentialEntriesAreIgnored() {
		givenAuthorizationServersAdvertised(true);
		// A non-openid_credential entry doesn't need to follow the §5.1.1 rule
		givenAuthorizationDetails("[{\"type\":\"some_other_type\",\"locations\":[\"https://elsewhere.example.com\"]},"
			+ "{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":[\"" + ISSUER + "\"]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_locationsNotArray_fails() {
		givenAuthorizationServersAdvertised(true);
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"locations\":\"" + ISSUER + "\"}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
