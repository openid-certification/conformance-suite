package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.time.LocalDate;
import java.time.ZoneId;

public class EnsurePixScheduleDateIsInPast extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {

		JsonObject obj = env.getObject("resource");
		if(obj == null){
			obj = env.getObject("config");
			obj = obj.getAsJsonObject("resource");
		}
		if(obj == null){
			logFailure("Cannot find resource object.");
			return env;
		}
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		log("yooo schedule in the past", obj);

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

		log("Setting date in consent config to date in the past: " + currentDate.minusDays(1));
		obj.addProperty("date", currentDate.minusDays(1).toString());
		logSuccess("Successfully added date in the past to consent config", obj);

		return env;
	}
}
