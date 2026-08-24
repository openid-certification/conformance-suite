package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureServerConfigurationHasRequiredOidcMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureServerConfigurationHasRequiredOidcMetadata cond;

	private static final String COMPLETE_SERVER = """
		{
			"issuer": "https://localhost.emobix.co.uk:8443/test/a/unit-test/",
			"authorization_endpoint": "https://localhost.emobix.co.uk:8443/test/a/unit-test/authorize",
			"token_endpoint": "https://localhost.emobix.co.uk:8443/test/a/unit-test/token",
			"jwks_uri": "https://localhost.emobix.co.uk:8443/test/a/unit-test/jwks",
			"response_types_supported": [ "code" ],
			"subject_types_supported": [ "public" ],
			"id_token_signing_alg_values_supported": [ "PS256" ]
		}
		""";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureServerConfigurationHasRequiredOidcMetadata();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private JsonObject server() {
		return JsonParser.parseString(COMPLETE_SERVER).getAsJsonObject();
	}

	@Test
	public void testEvaluate_allRequiredFieldsPresent() {
		env.putObject("server", server());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingIdTokenSigningAlgValues() {
		JsonObject server = server();
		server.remove("id_token_signing_alg_values_supported");
		env.putObject("server", server);

		ConditionError error = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(error.getMessage().contains("missing metadata"), error.getMessage());
	}

	@Test
	public void testEvaluate_missingSubjectTypesSupported() {
		JsonObject server = server();
		server.remove("subject_types_supported");
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void testEvaluate_missingJwksUri() {
		JsonObject server = server();
		server.remove("jwks_uri");
		env.putObject("server", server);

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}
}
