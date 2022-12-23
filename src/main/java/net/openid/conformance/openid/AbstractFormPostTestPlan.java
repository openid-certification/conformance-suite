package net.openid.conformance.openid;

import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseMode;

import java.util.ArrayList;
import java.util.List;

public class AbstractFormPostTestPlan implements TestPlan {
	/** Utility function to change a list of modules with variants to use form post response mode */
	public static List<ModuleListEntry> changeResponseTypeToFormPost(List<ModuleListEntry> normal) {
		List<ModuleListEntry> result = new ArrayList<>();
		boolean changedList = false;

		for (ModuleListEntry e: normal) {
			List<Variant> variants = new ArrayList<>();
			for (Variant v: e.variant) {
				if (v.key == ResponseMode.class) {
					variants.add(new Variant(ResponseMode.class, "form_post"));
					changedList = true;
				} else {
					variants.add(v);
				}
			}
			result.add(new ModuleListEntry(e.testModules, variants));
		}

		if (!changedList) {
			throw new RuntimeException("failed to change list to use form post");
		}

		return result;
	}
}
