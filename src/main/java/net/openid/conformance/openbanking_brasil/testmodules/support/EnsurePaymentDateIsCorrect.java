package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.LocalDate;
import java.time.ZoneId;


public class EnsurePaymentDateIsCorrect extends AbstractCondition{

	@Override
	public final Environment evaluate(Environment env) {

		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		JsonElement enteredDate = obj.get("date");
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		log(currentDate.toString());

		if (currentDate.equals(LocalDate.parse(OIDFJSON.forceConversionToString(enteredDate)))){
			logSuccess("Date is correct");
		}else{
			logFailure("Date is incorrect - needs to be today, today is " + currentDate + " and the date submitted is " + enteredDate);
		}

		return env;
	}
}
