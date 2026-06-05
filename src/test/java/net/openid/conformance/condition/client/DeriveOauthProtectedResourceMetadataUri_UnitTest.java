package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DeriveOauthProtectedResourceMetadataUri_UnitTest {

	private DeriveOauthProtectedResourceMetadataUri cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new DeriveOauthProtectedResourceMetadataUri();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		env = new Environment();
	}

	@Test
	public void stripsTrailingSlashWhenDerivingProtectedResourceMetadataUri() {
		// RFC 9728 sections 3 and 3.1 require removing any terminating "/" before
		// inserting "/.well-known/oauth-protected-resource".
		// https://www.rfc-editor.org/rfc/rfc9728.html#section-3.1
		setServerIssuer("https://resource.example.com/resource1/");

		cond.execute(env);

		assertEquals("https://resource.example.com/.well-known/oauth-protected-resource/resource1",
			env.getString("server", "oauth_protected_resource_metadata_uri"));
	}

	@Test
	public void derivesProtectedResourceMetadataUriWithoutPath() {
		setServerIssuer("https://resource.example.com/");

		cond.execute(env);

		assertEquals("https://resource.example.com/.well-known/oauth-protected-resource",
			env.getString("server", "oauth_protected_resource_metadata_uri"));
	}

	private void setServerIssuer(String issuer) {
		JsonObject server = new JsonObject();
		server.addProperty("issuer", issuer);
		env.putObject("server", server);
	}
}
