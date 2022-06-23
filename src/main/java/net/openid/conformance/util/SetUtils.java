package net.openid.conformance.util;

import com.google.common.collect.Sets;
import java.util.*;

public class SetUtils {

	public static Set<String> createSet(String values) {
		return Sets.newHashSet(values.trim().split(", "));
	}
}
