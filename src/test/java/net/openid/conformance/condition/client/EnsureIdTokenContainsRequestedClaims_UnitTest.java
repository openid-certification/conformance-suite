package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureIdTokenContainsRequestedClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIdTokenContainsRequestedClaims cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureIdTokenContainsRequestedClaims();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		// This is AustraliaConnectIdCheckClaimsSupported::ConnectIdMandatoryToSupportClaims - "name".
		JsonObject server = JsonParser.parseString("{"
			+ "\"claims_supported\": ["
				+ "\"given_name\","
				+ "\"middle_name\","
				+ "\"family_name\","
				+ "\"email\","
				+ "\"birthdate\","
				+ "\"phone_number\","
				+ "\"address\","
				+ "\"txn\""
			+ "]}")
			.getAsJsonObject();
		env.putObject("server", server);
	}

	@Test
	public void testEvaluate_idTokenContainsAllRequestedClaims() {

		// Add all supported claims to the authorization request.
		JsonObject authRequestClaims = JsonParser.parseString("""
			{\
			"claims": {\
			"id_token": {
			"given_name": {},\
			"middle_name": {},\
			"family_name": {"essential": true},\
			"email": {"7EimPyJ0oq": "eRpxS9SF3u"},\
			"birthdate": {"essential": false},\
			"phone_number": null,\
			"address": {},\
			"txn": {}\
			}\
			}}\
			""")
			.getAsJsonObject();
		env.putObject("authorization_endpoint_request", authRequestClaims);

		// Add all requested claims to the token id.
		JsonObject idTokenClaims = JsonParser.parseString("{"
			+ "\"claims\": {"
				+ "\"given_name\": \"Jane\","
				+ "\"middle_name\": \"Anne\","
				+ "\"family_name\": \"Doe\","
				+ "\"email\": \"janedoe@example.com\","
				+ "\"birthdate\": \"1/1/1970\","
				+ "\"phone_number\": \"12345678\","
				+ "\"address\": \"10 Downing Street\","
				+ "\"txn\": \"2c6fb585-d51b-465a-9dca-b8cd22a11451\""
			+ "}}")
			.getAsJsonObject();
		env.putObject("id_token", idTokenClaims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_idTokenMissingUnsupportedClaim() {

		// Add all supported claims to the authorization request + "name" which is not supported.
		JsonObject authRequestClaims = JsonParser.parseString("{"
			+ "\"claims\": {"
				+ "\"id_token\": {"
					+ "\"name\": null,"
					+ "\"given_name\": {},"
					+ "\"middle_name\": {},"
					+ "\"family_name\": {\"essential\": true},"
					+ "\"email\": {\"7EimPyJ0oq\": \"eRpxS9SF3u\"},"
					+ "\"birthdate\": {\"essential\": false},"
					+ "\"phone_number\": null,"
					+ "\"address\": {},"
					+ "\"txn\": {}"
				+ "}"
			+ "}}")
			.getAsJsonObject();
		env.putObject("authorization_endpoint_request", authRequestClaims);

		// Add all requested claims to the token id - "name" which should not be returned
		// as it is not supported by the server.
		JsonObject idTokenClaims = JsonParser.parseString("{"
			+ "\"claims\": {"
				+ "\"given_name\": \"Jane\","
				+ "\"middle_name\": \"Anne\","
				+ "\"family_name\": \"Doe\","
				+ "\"email\": \"janedoe@example.com\","
				+ "\"birthdate\": \"1/1/1970\","
				+ "\"phone_number\": \"12345678\","
				+ "\"address\": \"10 Downing Street\","
				+ "\"txn\": \"2c6fb585-d51b-465a-9dca-b8cd22a11451\""
			+ "}}")
			.getAsJsonObject();
		env.putObject("id_token", idTokenClaims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_idTokenMissingSupportedClaim() {
		assertThrows(ConditionError.class, () -> {

			// Add all supported claims to the authorization request.
			JsonObject authRequestClaims = JsonParser.parseString("{"
				+ "\"claims\": {"
				+ "\"id_token\": {"
				+ "\"given_name\": {},"
				+ "\"middle_name\": {},"
				+ "\"family_name\": {\"essential\": true},"
				+ "\"email\": {\"7EimPyJ0oq\": \"eRpxS9SF3u\"},"
				+ "\"birthdate\": {\"essential\": false},"
				+ "\"phone_number\": null,"
				+ "\"address\": {},"
				+ "\"txn\": {}"
				+ "}"
				+ "}}")
				.getAsJsonObject();
			env.putObject("authorization_endpoint_request", authRequestClaims);

			// Add all requested claims to the token id - "given_name".
			// This should result in an error as this claim is supported by the server.
			JsonObject idTokenClaims = JsonParser.parseString("{"
				+ "\"claims\": {"
				+ "\"middle_name\": \"Anne\","
				+ "\"family_name\": \"Doe\","
				+ "\"email\": \"janedoe@example.com\","
				+ "\"birthdate\": \"1/1/1970\","
				+ "\"phone_number\": \"12345678\","
				+ "\"address\": \"10 Downing Street\","
				+ "\"txn\": \"2c6fb585-d51b-465a-9dca-b8cd22a11451\""
				+ "}}")
				.getAsJsonObject();
			env.putObject("id_token", idTokenClaims);

			cond.execute(env);
		});
	}
}
