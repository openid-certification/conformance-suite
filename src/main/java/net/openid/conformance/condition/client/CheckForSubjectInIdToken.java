package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class CheckForSubjectInIdToken extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String sub = env.getString("id_token", "claims.sub");

		if (Strings.isNullOrEmpty(sub)) {
			throw error("id_token does not contain 'sub'");
		}

		// As per https://openid.net/specs/openid-connect-core-1_0.html#IDToken :
		// "It MUST NOT exceed 255 ASCII characters in length."
		if (sub.length() > 255) {
			throw error("id_token 'sub' exceeds 255 ASCII characters", args("sub", sub));
		}

		for (int i = 0; i < sub.length(); i++) {
			int c = sub.charAt(i);
			if (c < 0x20) {
				throw error("id_token 'sub' contains non-printable character 0x%02x at offset %d".formatted(c, i), args("sub", sub));
			}
			if (c >= 0x7F) {
				throw error("id_token 'sub' contains non-ASCII character 0x%02x at offset %d".formatted(c, i), args("sub", sub));
			}
		}

		logSuccess("Found 'sub' in id_token", args("sub", sub));
		return env;
	}

}
