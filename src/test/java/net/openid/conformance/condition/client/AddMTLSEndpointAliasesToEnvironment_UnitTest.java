package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddMTLSEndpointAliasesToEnvironment_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddMTLSEndpointAliasesToEnvironment cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddMTLSEndpointAliasesToEnvironment();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noError() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
			+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
			+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
			+ "\"mtls_endpoint_aliases\": {"
			+ 		"\"token_endpoint\": \"https://mtls.example.com/token\","
			+ 		"\"revocation_endpoint\": \"https://mtls.example.com/revo\","
			+ 		"\"introspection_endpoint\": \"https://mtls.example.com/introspect\""
			+ "}}").getAsJsonObject();

		env.putObject("server", server);

		cond.execute(env);

		assertThat(env.getString("server", "mtls_endpoint_aliases.token_endpoint")).isEqualTo(env.getString("token_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.revocation_endpoint")).isEqualTo(env.getString("revocation_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.introspection_endpoint")).isEqualTo(env.getString("introspection_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.only_in_mtsaliases_endpoint")).isEqualTo(env.getString("only_in_mtsaliases_endpoint"));
		assertThat(env.getString("server", "userinfo_endpoint")).isEqualTo(env.getString("userinfo_endpoint"));

	}

	@Test
	public void testEvaluate_onlyInMtls() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
			+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
			+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
			+ "\"mtls_endpoint_aliases\": {"
			+ 		"\"token_endpoint\": \"https://mtls.example.com/token\","
			+ 		"\"revocation_endpoint\": \"https://mtls.example.com/revo\","
			+ 		"\"introspection_endpoint\": \"https://mtls.example.com/introspect\","
			+ 		"\"only_in_mtsaliases_endpoint\": \"https://mtls.example.com/onlyinmtlsaliases\""
			+ "}}").getAsJsonObject();

		env.putObject("server", server);

		boolean exception = false;

		try {
			cond.execute(env);
		} catch (ConditionError e) {
			exception = true;
		}

		assertThat(env.getString("server", "mtls_endpoint_aliases.token_endpoint")).isEqualTo(env.getString("token_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.revocation_endpoint")).isEqualTo(env.getString("revocation_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.introspection_endpoint")).isEqualTo(env.getString("introspection_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.only_in_mtsaliases_endpoint")).isEqualTo(env.getString("only_in_mtsaliases_endpoint"));
		assertThat(env.getString("server", "userinfo_endpoint")).isEqualTo(env.getString("userinfo_endpoint"));

		assertThat(exception);

	}

	@Test
	public void testEvaluate_notPresentMtlsEndpointAliases() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\""
			+ "}").getAsJsonObject();

		env.putObject("server", server);

		cond.execute(env);

		assertThat(env.getString("server", "token_endpoint")).isEqualTo(env.getString("token_endpoint"));

	}

	@Test
	public void testEvaluate_missingConfig() {
		assertThrows(ConditionError.class, () -> {
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_notObject() {
		assertThrows(ConditionError.class, () -> {
			JsonObject server = JsonParser.parseString("{"
				+ "\"token_endpoint\": \"https://server.example.com/token\","
				+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
				+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
				+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
				+ "\"mtls_endpoint_aliases\": \"https://mtls.example.com/token\""
				+ "}").getAsJsonObject();

			env.putObject("server", server);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_authEndpointAliasedError() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
			+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
			+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
			+ "\"mtls_endpoint_aliases\": {"
			+ 		"\"token_endpoint\": \"https://mtls.example.com/token\","
			+ 		"\"revocation_endpoint\": \"https://mtls.example.com/revo\","
			+ 		"\"authorization_endpoint\": \"https://mtls.example.com/auth\","
			+ 		"\"introspection_endpoint\": \"https://mtls.example.com/introspect\""
			+ "}}").getAsJsonObject();

		env.putObject("server", server);

		boolean exception = false;

		try {
			cond.execute(env);
		} catch (ConditionError e) {
			exception = true;
		}

		assertThat(env.getString("server", "mtls_endpoint_aliases.token_endpoint")).isEqualTo(env.getString("token_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.revocation_endpoint")).isEqualTo(env.getString("revocation_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.introspection_endpoint")).isEqualTo(env.getString("introspection_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.only_in_mtsaliases_endpoint")).isEqualTo(env.getString("only_in_mtsaliases_endpoint"));
		assertThat(env.getString("server", "userinfo_endpoint")).isEqualTo(env.getString("userinfo_endpoint"));
		assertThat(exception);
	}

	@Test
	public void testEvaluate_NotEndpointAliasedError() {

		JsonObject server = JsonParser.parseString("{"
			+ "\"token_endpoint\": \"https://server.example.com/token\","
			+ "\"revocation_endpoint\": \"https://server.example.com/revo\","
			+ "\"introspection_endpoint\": \"https://server.example.com/introspect\","
			+ "\"userinfo_endpoint\": \"https://server.example.com/me\","
			+ "\"mtls_endpoint_aliases\": {"
			+ 		"\"token_endpoint\": \"https://mtls.example.com/token\","
			+ 		"\"revocation_endpoint\": \"https://mtls.example.com/revo\","
			+ 		"\"issuer\": \"https://mtls.example.com/\","
			+ 		"\"introspection_endpoint\": \"https://mtls.example.com/introspect\""
			+ "}}").getAsJsonObject();

		env.putObject("server", server);

		boolean exception = false;

		try {
			cond.execute(env);
		} catch (ConditionError e) {
			exception = true;
		}

		assertThat(env.getString("server", "mtls_endpoint_aliases.token_endpoint")).isEqualTo(env.getString("token_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.revocation_endpoint")).isEqualTo(env.getString("revocation_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.introspection_endpoint")).isEqualTo(env.getString("introspection_endpoint"));
		assertThat(env.getString("server", "mtls_endpoint_aliases.only_in_mtsaliases_endpoint")).isEqualTo(env.getString("only_in_mtsaliases_endpoint"));
		assertThat(env.getString("server", "userinfo_endpoint")).isEqualTo(env.getString("userinfo_endpoint"));
		assertThat(exception);
	}

}
