package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.ValidateQrdnConfig;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ValidateQrdnConfigTests {

	@Test
	public void passesIfPresent() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();
		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		ValidateQrdnConfig condition = new ValidateQrdnConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
		} catch (ConditionError ce) {
			fail(ce.getMessage());
		}

	}

	@Test
	public void failsGracefullyIfConfigMissing() throws IOException {

		JsonObject qrdnConsentConfig = new JsonObject();

		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		ValidateQrdnConfig condition = new ValidateQrdnConfig();

		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("Should have thrown an error");
		} catch(ConditionError e) {
			assertEquals( "ValidateQrdnConfig: QRDN consent config is missing or incomplete", e.getMessage());
		}

	}

	@Test
	public void failsIfCnpjMissing() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();
		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnRemittance", "Pagamento da nota RSTO035-002.");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		ValidateQrdnConfig condition = new ValidateQrdnConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("Should have thrown an error");
		} catch(ConditionError e) {
			assertEquals( "ValidateQrdnConfig: QRDN consent config does not contain an initiators CNPJ", e.getMessage());
		}

	}

	@Test
	public void failsIfRemittanceDetailsMissing() throws IOException {

		String rawJson = IOUtils.resourceToString("qrdn_consent_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject qrdnConsentConfig = new JsonParser().parse(rawJson).getAsJsonObject();
		JsonObject resourceConfig = new JsonObject();
		resourceConfig.add("brazilQrdnPaymentConsent", qrdnConsentConfig);
		resourceConfig.addProperty("brazilQrdnCnpj", "60545350000165");
		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);

		Environment environment = new Environment();
		environment.putObject("resource", resourceConfig);
		environment.putObject("config", config);

		ValidateQrdnConfig condition = new ValidateQrdnConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

		try {
			condition.execute(environment);
			fail("Should have thrown an error");
		} catch(ConditionError e) {
			assertEquals( "ValidateQrdnConfig: QRDN config does not contain a remittance advice", e.getMessage());
		}

	}

}
