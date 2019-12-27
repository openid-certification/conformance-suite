package net.openid.conformance.condition.client;

import java.util.Set;

public abstract class ValidateResponseTypesArray extends ValidateJsonArray {

	@Override
	protected boolean elementsEqual(String e1, String e2) {
		return Set.of(e1.split(" ")).equals(Set.of(e2.split(" ")));
	}

}
