package net.openid.conformance.openbanking_brasil.testmodules.support.payments;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubsequentPixPaymentEditorCondition extends AbstractCondition {

	private static final Pattern P = Pattern.compile("^\\$\\$EDITPAYMENT\\$\\$:(?<amount>\\d+\\.\\d{2}$)");

	@Override
	public Environment evaluate(Environment env) {
		String remittanceInformation = env.getString("resource", "brazilQrdnRemittance");
		Matcher matcher = P.matcher(remittanceInformation);
		if(matcher.matches()) {
			String amount = matcher.group("amount");
			JsonObject payment = (JsonObject) env.getElementFromObject("resource", "brazilQrdnPaymentConsent.data.payment");
			payment.addProperty("amount", amount);
			logSuccess("Edited QRDN payment amount for testing test", Map.of("amount", amount));
		} else {
			log("Second QRDN payment not being edited");
		}
		
		return env;
	}
}
