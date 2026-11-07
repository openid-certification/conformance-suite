package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.variant.VCICredentialOfferParameterVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class VCICreateCredentialOfferRedirectUrl_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private Environment env;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		JsonObject config = new JsonObject();
		config.add("vci", JsonParser.parseString("{\"credential_offer_endpoint\":\"openid-credential-offer://\"}"));
		env.putObject("config", config);
	}

	@Test
	public void byValueOfferIsFormUrlEncoded() {
		JsonObject offer = JsonParser.parseString("{\"credential_issuer\":\"https://issuer.example.com/a\","
			+ "\"credential_configuration_ids\":[\"dc+sd-jwt pid\"]}").getAsJsonObject();
		JsonObject vci = new JsonObject();
		vci.add("credential_offer", offer);
		env.putObject("vci", vci);
		VCICreateCredentialOfferRedirectUrl cond = new VCICreateCredentialOfferRedirectUrl(VCICredentialOfferParameterVariant.BY_VALUE);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		String url = env.getString("vci", "credential_offer_redirect_url");
		assertThat(url).startsWith("openid-credential-offer://?credential_offer=");
		assertThat(url).contains("dc%2Bsd-jwt%20pid").doesNotContain("+");
		String decoded = URLDecoder.decode(url.substring(url.indexOf('=') + 1), StandardCharsets.UTF_8);
		assertThat(JsonParser.parseString(decoded)).isEqualTo(offer);
	}

	@Test
	public void byReferenceOfferUriIsFormUrlEncoded() {
		JsonObject vci = new JsonObject();
		vci.addProperty("credential_offer_uri", "https://issuer.example.com/offers/1?x=a+b");
		env.putObject("vci", vci);
		VCICreateCredentialOfferRedirectUrl cond = new VCICreateCredentialOfferRedirectUrl(VCICredentialOfferParameterVariant.BY_REFERENCE);
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		cond.execute(env);

		assertThat(env.getString("vci", "credential_offer_redirect_url"))
			.isEqualTo("openid-credential-offer://?credential_offer_uri=https%3A%2F%2Fissuer.example.com%2Foffers%2F1%3Fx%3Da%2Bb");
	}
}
