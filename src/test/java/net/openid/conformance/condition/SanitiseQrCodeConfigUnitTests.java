package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.SanitiseQrCodeConfig;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;

public class SanitiseQrCodeConfigUnitTests {

	@Test
	public void qrCodeRemovedFromPaymentConsentAndPaymentInitiation() throws IOException {

		String rawJson = IOUtils.resourceToString("test_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject config = new JsonParser().parse(rawJson).getAsJsonObject();

		Environment environment = new Environment();
		environment.putObject("config", config);

		SanitiseQrCodeConfig condition = new SanitiseQrCodeConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		String consentQrCode = environment.getString("config", "resource.brazilPaymentConsent.data.payment.details.qrCode");
		String paymentQrCode = environment.getString("config", "resource.brazilPixPayment.data.qrCode");

		Assert.assertNull(consentQrCode);
		Assert.assertNull(paymentQrCode);

	}

	@Test
	public void qrCodeLeftAloneForQresLocalInstrument() throws IOException {

		String rawJson = IOUtils.resourceToString("test_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject config = new JsonParser().parse(rawJson).getAsJsonObject();

		Environment environment = new Environment();
		environment.putObject("config", config);
		JsonObject details = (JsonObject) environment.getElementFromObject("config", "resource.brazilPaymentConsent.data.payment.details");
		details.addProperty("localInstrument", "QRES");

		SanitiseQrCodeConfig condition = new SanitiseQrCodeConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		String consentQrCode = environment.getString("config", "resource.brazilPaymentConsent.data.payment.details.qrCode");
		String paymentQrCode = environment.getString("config", "resource.brazilPixPayment.data.qrCode");

		Assert.assertNotNull(consentQrCode);
		Assert.assertNotNull(paymentQrCode);

	}

	@Test
	public void qrCodeLeftAloneForQrdnLocalInstrument() throws IOException {

		String rawJson = IOUtils.resourceToString("test_config.json", Charset.defaultCharset(), getClass().getClassLoader());

		JsonObject config = new JsonParser().parse(rawJson).getAsJsonObject();

		Environment environment = new Environment();
		environment.putObject("config", config);
		JsonObject details = (JsonObject) environment.getElementFromObject("config", "resource.brazilPaymentConsent.data.payment.details");
		details.addProperty("localInstrument", "QRDN");


		SanitiseQrCodeConfig condition = new SanitiseQrCodeConfig();
		condition.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);
		condition.evaluate(environment);

		String consentQrCode = environment.getString("config", "resource.brazilPaymentConsent.data.payment.details.qrCode");
		String paymentQrCode = environment.getString("config", "resource.brazilPixPayment.data.qrCode");

		Assert.assertNotNull(consentQrCode);
		Assert.assertNotNull(paymentQrCode);

	}

}
