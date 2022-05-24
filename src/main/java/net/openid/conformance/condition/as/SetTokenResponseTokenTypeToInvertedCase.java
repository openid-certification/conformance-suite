package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetTokenResponseTokenTypeToInvertedCase extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"token_type"})
	public Environment evaluate(Environment env) {
		String tokenType = env.getString("token_type");
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
		env.putString("token_type", tokenType);
		logSuccess("Set token endpoint response 'token_type' to inverted case letters", args("token_type", tokenType));
		return env;
	}
}
