package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CMFChileRegistrationRequestConditions_UnitTest {

	private static final String VALID_REQUEST = """
		{
			"software_statement": "eyJ.a.b",
			"redirect_uris": ["https://rp.example.cl/cb"],
			"grant_types": ["authorization_code"],
			"token_endpoint_auth_method": "tls_client_auth",
			"jwks_uri": "https://rp.example.cl/jwks",
			"scope": "openid accounts payments",
			"application_type": "web",
			"client_name": "Example RP",
			"contacts": ["security@rp.example.cl"]
		}
		""";

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
	}

	private <T extends AbstractCondition> T condition(T cond) {
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		return cond;
	}

	private void putRequest(String json) {
		env.putObject("dynamic_registration_request", JsonParser.parseString(json).getAsJsonObject());
	}

	private void putValidRequestWith(String name, String jsonValue) {
		var request = JsonParser.parseString(VALID_REQUEST).getAsJsonObject();
		if (jsonValue == null) {
			request.remove(name);
		} else {
			request.add(name, JsonParser.parseString(jsonValue));
		}
		env.putObject("dynamic_registration_request", request);
	}

	@Test
	public void testMandatoryMetadata_allPresent() {
		putRequest(VALID_REQUEST);

		assertThatCode(() -> condition(new CMFChileEnsureRegistrationRequestContainsMandatoryMetadata()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testMandatoryMetadata_missingScope() {
		putValidRequestWith("scope", null);

		assertThatThrownBy(() -> condition(new CMFChileEnsureRegistrationRequestContainsMandatoryMetadata()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("metadata that CMF requires");
	}

	@Test
	public void testMandatoryMetadata_emptyRedirectUris() {
		putValidRequestWith("redirect_uris", "[]");

		assertThatThrownBy(() -> condition(new CMFChileEnsureRegistrationRequestContainsMandatoryMetadata()).execute(env))
			.isInstanceOf(ConditionError.class);
	}

	@Test
	public void testMandatoryMetadata_clientNameNotString() {
		putValidRequestWith("client_name", "[\"Example RP\"]");

		assertThatThrownBy(() -> condition(new CMFChileEnsureRegistrationRequestContainsMandatoryMetadata()).execute(env))
			.isInstanceOf(ConditionError.class);
	}

	@Test
	public void testGrantTypes_authorizationCodeOnly() {
		putRequest(VALID_REQUEST);

		assertThatCode(() -> condition(new CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testGrantTypes_includesClientCredentials() {
		putValidRequestWith("grant_types", "[\"authorization_code\", \"client_credentials\"]");

		assertThatThrownBy(() -> condition(new CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("authorization_code");
	}

	@Test
	public void testGrantTypes_missingIsLeftToMandatoryCheck() {
		putValidRequestWith("grant_types", null);

		assertThatCode(() -> condition(new CMFChileEnsureRegistrationRequestGrantTypesIsAuthorizationCodeOnly()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testApplicationType_web() {
		putRequest(VALID_REQUEST);

		assertThatCode(() -> condition(new CMFChileValidateRegistrationRequestApplicationType()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testApplicationType_service() {
		putValidRequestWith("application_type", "\"service\"");

		assertThatCode(() -> condition(new CMFChileValidateRegistrationRequestApplicationType()).execute(env))
			.doesNotThrowAnyException();
	}

	@Test
	public void testApplicationType_native() {
		putValidRequestWith("application_type", "\"native\"");

		assertThatThrownBy(() -> condition(new CMFChileValidateRegistrationRequestApplicationType()).execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("'web' or 'service'");
	}
}
