package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import java.util.Date;
import java.text.SimpleDateFormat;

public class EnsurePaymentDateIsCorrect extends AbstractCondition{

	@Override
	public final Environment evaluate(Environment env) {

		JsonObject obj = env.getObject("resource");
		obj = obj.getAsJsonObject("brazilPaymentConsent");
		obj = obj.getAsJsonObject("data");
		obj = obj.getAsJsonObject("payment");

		JsonElement enteredDate = obj.get("date");

		Date currentDate = new Date();
		String strDateFormat = "yyyy-MM-dd";
		SimpleDateFormat objSDF = new SimpleDateFormat(strDateFormat);
		String updatedCurrentDateFormat = '"'  + objSDF.format(currentDate) + '"';

		JsonElement o = new JsonParser().parse(updatedCurrentDateFormat);

		if (o.equals(enteredDate)){
			logSuccess("Date is correct");
		}else{
			logFailure("Date is incorrect - needs to be today, today is " + o + " and the date submitted is " + enteredDate);
		}

		return env;
	}

}
