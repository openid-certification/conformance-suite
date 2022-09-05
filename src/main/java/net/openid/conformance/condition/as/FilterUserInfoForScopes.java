package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class FilterUserInfoForScopes extends AbstractCondition {

	private static SetMultimap<String, String> scopesToClaims = HashMultimap.create();
	{
		scopesToClaims.put("openid", "sub");

		scopesToClaims.put("profile", "name");
		scopesToClaims.put("profile", "preferred_username");
		scopesToClaims.put("profile", "given_name");
		scopesToClaims.put("profile", "family_name");
		scopesToClaims.put("profile", "middle_name");
		scopesToClaims.put("profile", "nickname");
		scopesToClaims.put("profile", "profile");
		scopesToClaims.put("profile", "picture");
		scopesToClaims.put("profile", "website");
		scopesToClaims.put("profile", "gender");
		scopesToClaims.put("profile", "zoneinfo");
		scopesToClaims.put("profile", "locale");
		scopesToClaims.put("profile", "updated_at");
		scopesToClaims.put("profile", "birthdate");

		scopesToClaims.put("email", "email");
		scopesToClaims.put("email", "email_verified");

		scopesToClaims.put("phone", "phone_number");
		scopesToClaims.put("phone", "phone_number_verified");

		scopesToClaims.put("address", "address");
	}

	@Override
	@PreEnvironment(strings = "scope", required = "user_info")
	@PostEnvironment(required = "user_info_endpoint_response")
	public Environment evaluate(Environment env) {

		String scope = env.getString("scope");
		JsonObject userInfo = env.getObject("user_info");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope));

		JsonObject out = new JsonObject();

		// look through all the scopes that we have approved
		for (String s : scopes) {
			if (scopesToClaims.containsKey(s)) {
				for (String claim : scopesToClaims.get(s)) {
					if (userInfo.has(claim)) {
						// if we have a claim that fits that scope, copy it over
						out.add(claim, userInfo.get(claim));
					}
				}
			}
		}

		env.putObject("user_info_endpoint_response", out);

		logSuccess("User info endpoint output", out);

		return env;
	}

}
