package net.openid.conformance.vci10issuer.condition;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class VCIEnsureAuthorizationServerIssuerMatchesExpected_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	void setUp() {
		env = new Environment();
	}

	private void run(int serverIndex) {
		VCIEnsureAuthorizationServerIssuerMatchesExpected cond =
			new VCIEnsureAuthorizationServerIssuerMatchesExpected(serverIndex);
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		cond.execute(env);
	}

	private void put(String credentialIssuerMetadataJson, String serverJson) {
		JsonObject vci = new JsonObject();
		vci.add("credential_issuer_metadata", JsonParser.parseString(credentialIssuerMetadataJson).getAsJsonObject());
		env.putObject("vci", vci);
		env.putObject("server", JsonParser.parseString(serverJson).getAsJsonObject());
	}

	@Test
	void acceptsMatchingIssuerFromAuthorizationServersList() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "authorization_servers": ["https://as0.example.com", "https://as1.example.com"]
			}
			""", """
			{"issuer": "https://as0.example.com"}
			""");
		assertDoesNotThrow(() -> run(0));
	}

	@Test
	void acceptsMatchingIssuerForSecondAuthorizationServer() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "authorization_servers": ["https://as0.example.com", "https://as1.example.com"]
			}
			""", """
			{"issuer": "https://as1.example.com"}
			""");
		assertDoesNotThrow(() -> run(1));
	}

	@Test
	void rejectsIssuerNotMatchingAuthorizationServersEntry() {
		// server.issuer claims as0 but this is the metadata fetched for as1 -> impersonation
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "authorization_servers": ["https://as0.example.com", "https://as1.example.com"]
			}
			""", """
			{"issuer": "https://as0.example.com"}
			""");
		assertThrows(ConditionError.class, () -> run(1));
	}

	@Test
	void acceptsCredentialIssuerAsAuthorizationServerWhenNoList() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com"
			}
			""", """
			{"issuer": "https://issuer.example.com"}
			""");
		assertDoesNotThrow(() -> run(0));
	}

	@Test
	void rejectsMismatchWhenNoAuthorizationServersList() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com"
			}
			""", """
			{"issuer": "https://evil.example.com"}
			""");
		assertThrows(ConditionError.class, () -> run(0));
	}

	@Test
	void ignoresTrailingSlashDifference() {
		put("""
			{
			  "credential_issuer": "https://issuer.example.com",
			  "authorization_servers": ["https://as0.example.com"]
			}
			""", """
			{"issuer": "https://as0.example.com/"}
			""");
		assertDoesNotThrow(() -> run(0));
	}

	@Test
	void rejectsMissingIssuer() {
		put("""
			{
			  "authorization_servers": ["https://as0.example.com"]
			}
			""", """
			{"token_endpoint": "https://as0.example.com/token"}
			""");
		assertThrows(ConditionError.class, () -> run(0));
	}

	@Test
	void rejectsIndexOutOfRange() {
		put("""
			{
			  "authorization_servers": ["https://as0.example.com"]
			}
			""", """
			{"issuer": "https://as0.example.com"}
			""");
		assertThrows(ConditionError.class, () -> run(5));
	}
}
