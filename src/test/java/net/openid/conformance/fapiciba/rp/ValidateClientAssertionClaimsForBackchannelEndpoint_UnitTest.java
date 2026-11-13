package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValidateClientAssertionClaimsForBackchannelEndpoint_UnitTest {

	private final Environment env = new Environment();
	private final JsonObject server = new JsonObject();
	private final JsonObject claims = new JsonObject();
	private final ValidateClientAssertionClaimsForBackchannelEndpoint condition =
		new ValidateClientAssertionClaimsForBackchannelEndpoint();

	@BeforeEach
	public void setUp() {
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.INFO, new String[0]);
		server.addProperty("issuer", "https://as.example.com/");
		server.addProperty("token_endpoint", "https://as.example.com/token");
		server.addProperty("backchannel_authentication_endpoint", "https://as.example.com/backchannel");
		env.putObject("server", server);
		JsonObject client = new JsonObject();
		client.addProperty("client_id", "test-client");
		env.putObject("client", client);
		claims.addProperty("iss", "test-client");
		claims.addProperty("sub", "test-client");
		claims.addProperty("jti", "test-assertion");
		claims.addProperty("iat", Instant.now().getEpochSecond());
		claims.addProperty("exp", Instant.now().plusSeconds(300).getEpochSecond());
		JsonObject assertion = new JsonObject();
		assertion.add("claims", claims);
		env.putObject("client_assertion", assertion);
	}

	private void addAliases(boolean aliases) {
		if (aliases) {
			JsonObject endpoints = new JsonObject();
			endpoints.addProperty("token_endpoint", "https://alias.example.com/token");
			endpoints.addProperty("backchannel_authentication_endpoint", "https://alias.example.com/backchannel");
			server.add("mtls_endpoint_aliases", endpoints);
		}
	}

	static Stream<Arguments> validAudiences() {
		return Stream.of(false, true).flatMap(aliases -> {
			Stream<String> audiences = Stream.of("https://as.example.com/", "https://as.example.com/token",
				"https://as.example.com/backchannel");
			if (aliases) {
				audiences = Stream.concat(audiences,
					Stream.of("https://alias.example.com/token", "https://alias.example.com/backchannel"));
			}
			return audiences.flatMap(audience -> {
				JsonArray array = new JsonArray();
				array.add("https://other.example.com/");
				array.add(audience);
				return Stream.of(Arguments.of(new JsonPrimitive(audience), aliases), Arguments.of(array, aliases));
			});
		});
	}

	@ParameterizedTest
	@MethodSource("validAudiences")
	public void acceptsPermittedAudience(JsonElement audience, boolean aliases) {
		addAliases(aliases);
		claims.add("aud", audience);
		assertDoesNotThrow(() -> condition.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"https://as.example.com/\"", "[\"https://as.example.com/\"]"})
	public void acceptsIssuerWithDifferentEndpointHostsAndNoAliases(String audience) {
		server.addProperty("token_endpoint", "https://mtls.example.com/token");
		server.addProperty("backchannel_authentication_endpoint", "https://mtls.example.com/backchannel");
		claims.add("aud", JsonParser.parseString(audience));
		assertDoesNotThrow(() -> condition.execute(env));
	}

	static Stream<Arguments> invalidAudiences() {
		return Stream.of(false, true).flatMap(aliases -> Stream.of(
			"\"https://attacker.example.com/\"", "\"https://\"", "\"https://as.example.com/back\"", "\"\"",
			"\"https://as.example.com/extra\"", "[]", "[\"https://attacker.example.com/\"]", "[null]", "null",
			"123", "true", "{}", "[123]", "[{}]"
		).map(audience -> Arguments.of(JsonParser.parseString(audience), aliases)));
	}

	@ParameterizedTest
	@MethodSource("invalidAudiences")
	public void rejectsInvalidAudienceWithConditionError(JsonElement audience, boolean aliases) {
		addAliases(aliases);
		claims.add("aud", audience);
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"\"", "[\"\"]"})
	public void rejectsEmptyAudienceWhenAliasIsEmpty(String audience) {
		JsonObject endpoints = new JsonObject();
		endpoints.addProperty("token_endpoint", "");
		endpoints.addProperty("backchannel_authentication_endpoint", "");
		server.add("mtls_endpoint_aliases", endpoints);
		claims.add("aud", JsonParser.parseString(audience));
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void rejectsMissingBackchannelEndpointWithConditionError(String backchannelEndpoint) {
		server.addProperty("backchannel_authentication_endpoint", backchannelEndpoint);
		claims.addProperty("aud", "https://as.example.com/");
		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
