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
public class VCIWarnOnAuthorizationDetailsConventionsInIncomingRequest_UnitTest {

	private static final String CONFIG_ID = "oid4vc_natural_person_sd";

	private VCIWarnOnAuthorizationDetailsConventionsInIncomingRequest cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIWarnOnAuthorizationDetailsConventionsInIncomingRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.WARNING);
		env = new Environment();
	}

	private void givenAuthorizationDetails(String authorizationDetailsJson) {
		JsonObject req = new JsonObject();
		req.add("authorization_details", JsonParser.parseString(authorizationDetailsJson));
		env.putObject("effective_authorization_endpoint_request", req);
	}

	@Test
	public void testEvaluate_validEntry_doesNotWarn() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_authorizationDetailsAbsent_isNoOp() {
		env.putObject("effective_authorization_endpoint_request", new JsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_rfc9396CommonFields_doNotWarn() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\","
			+ "\"locations\":[\"https://issuer.example.com\"],"
			+ "\"actions\":[\"read\"],"
			+ "\"datatypes\":[\"x\"],"
			+ "\"identifier\":\"res-1\","
			+ "\"privileges\":[\"basic\"],"
			+ "\"claims\":[]}]");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_unknownFieldOnEntry_warns() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"unknown_field\":\"value\"}]");

		ConditionError err = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(err.getMessage().contains("conventions"));
	}

	@Test
	public void testEvaluate_nonOpenidCredentialType_warns() {
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\"},"
			+ "{\"type\":\"payment_initiation\",\"actions\":[\"initiate\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_credentialIdentifiersWarnsAsUnknownField() {
		// credential_identifiers is response-only; if a wallet sends it, the structural
		// validator fails. The warner classes it as an unknown field on this side too.
		givenAuthorizationDetails("[{\"type\":\"openid_credential\",\"credential_configuration_id\":\""
			+ CONFIG_ID + "\",\"credential_identifiers\":[\"x\"]}]");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
