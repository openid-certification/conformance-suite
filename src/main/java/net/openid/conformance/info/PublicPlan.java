package net.openid.conformance.info;

import net.openid.conformance.variant.VariantSelection;

import java.util.List;

public class PublicPlan {

	private String _id;
	private String planName;
	private VariantSelection variant;
	private String description;

	private String certificationProfileName;
	private String started;
	private List<Plan.Module> modules;
	private String publish;
	private String version;
	private Boolean immutable;

	public String getId() {
		return _id;
	}

	public String getPlanName() {
		return planName;
	}

	public VariantSelection getVariant() {
		return variant;
	}

	public String getDescription() {
		return description;
	}

	public String getCertificationProfileName() {
		return certificationProfileName;
	}

	public String getStarted() {
		return started;
	}

	public List<Plan.Module> getModules() {
		return modules;
	}

	public String getPublish() {
		return publish;
	}

	public String getVersion() {
		return version;
	}

	public Boolean getImmutable() {
		return immutable;
	}
}
