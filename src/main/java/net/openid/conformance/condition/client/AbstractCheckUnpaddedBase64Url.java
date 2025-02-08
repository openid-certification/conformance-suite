package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;

import java.util.regex.Pattern;

public abstract class AbstractCheckUnpaddedBase64Url extends AbstractCondition {
	protected void checkUnpaddedBase64Url(String mdocBase64, String tokenName) {
		String regex = "[a-zA-Z0-9_-]";
		for (int i = 0; i < mdocBase64.length(); i++) {
			char character = mdocBase64.charAt(i);
			if (!Pattern.matches(regex, String.valueOf(character))) {
				throw error(tokenName + " is invalid because it contains characters not permitted in unpadded base64url",
					args("bad_character", "'" + character + "'"));
			}
		}

		logSuccess(tokenName + " contains only characters permitted in unpadded base64url");
	}
}
