package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetAccessTokenTypeToInvertedCase extends AbstractCondition {

	@Override
	@PreEnvironment(required = "access_token")
	public Environment evaluate(Environment env) {
		String tokenType = env.getString("access_token","type");
		if(Strings.isNullOrEmpty(tokenType)) {
			throw error("token_type not available");
		} else {
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < tokenType.length(); i++) {
				char c = tokenType.charAt(i);
				if(Character.isAlphabetic(tokenType.codePointAt(i))) {
					if(Character.isLowerCase(c)) {
						sb.append(Character.toUpperCase(c));
					} else {
						sb.append(Character.toLowerCase(c));
					}
				} else {
					sb.append(c);
				}
			}
			tokenType = sb.toString();
		}
		env.putString("access_token", "type", tokenType);
		logSuccess("Set token endpoint request token type to inverted case letters", args("type", tokenType));

		return env;
	}

}
