package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddOpenIDProviderMetadataToEntityConfiguration_UnitTest {

	@Test
	public void publishesIssuerMatchingEntityIdentifier() {
		Environment env = new Environment();
		JsonObject server = new JsonObject();
		server.addProperty("iss", "https://op.example:8443/test/federation");
		server.add("metadata", new JsonObject());
		env.putObject("server", server);

		JsonObject encryptionKeys = new JsonObject();
		encryptionKeys.add("keys", new JsonArray());
		env.putObject("server_encryption_keys", encryptionKeys);

		var condition = new AddOpenIDProviderMetadataToEntityConfiguration();
		condition.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), Condition.ConditionResult.INFO);
		condition.execute(env);

		assertEquals("https://op.example:8443/test/federation", env.getString("server", "metadata.openid_provider.issuer"));
	}

}
