package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class FAPIBrazilExtractSoftwareStatement extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "software_statement_assertion"})
	@PostEnvironment(required = "software_statement")
	public Environment evaluate(Environment env) {
		String ssa = env.getString("software_statement_assertion");
		try
		{
			JsonObject statement = JWTUtil.jwtStringToJsonObjectForEnvironment(ssa);
			env.putObject("software_statement", statement);
			logSuccess("Parsed software_statement", args("software_statement", statement));
			return env;
		} catch (ParseException exception) {
			throw error("Failed to parse software_statement", exception, args("software_statement_assertion", ssa));
		}
	}
}
