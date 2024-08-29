package net.openid.conformance.export;

import net.openid.conformance.info.Plan;
import net.openid.conformance.info.PublicPlan;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PlanHelper {
	private Logger logger = LoggerFactory.getLogger(PlanHelper.class);
	private PlanExportInfo planExportInfo;
	private String suiteBaseUrl;

	public PlanHelper(PlanExportInfo planExportInfo, String suiteBaseUrl) {
		this.planExportInfo = planExportInfo;
		this.suiteBaseUrl = suiteBaseUrl;
	}

	public String getExportedFrom() {
		return planExportInfo.getExportedFrom();
	}
	public String getExportedBy() {
		return planExportInfo.getExportedBy().get("sub") + " " + planExportInfo.getExportedBy().get("iss");
	}
	public String getExportedBySub() {
		return planExportInfo.getExportedBy().get("sub");
	}
	public String getExportedByIss() {
		return planExportInfo.getExportedBy().get("iss");
	}
	public String getSuiteVersion() {
		return planExportInfo.getExportedVersion();
	}
	private String formatDate(Date date) {
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		dateFormat.setTimeZone(timeZone);
		String formatted = dateFormat.format(date) + " (UTC)";
		return formatted;
	}

	public String getExportedTime() {
		return formatDate(planExportInfo.getExportedAt());
	}
	public String getPlanName() {
		try {
			if (planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan) planExportInfo.getPlanInfo();
				return publicPlan.getPlanName();
			} else if (planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan) planExportInfo.getPlanInfo();
				return plan.getPlanName();
			} else if (planExportInfo.getPlanInfo() instanceof Document) {
				Document doc = (Document) planExportInfo.getPlanInfo();
				return doc.getString("planName");
			}
		} catch (Exception ex) {
			logger.error("Error getting plan name from " + planExportInfo.getPlanInfo().getClass() + " id=" + getPlanId(), ex);
		}
		throw new RuntimeException("Error getting plan name. This is a bug in the suite.");
	}
	public String getPlanVariant() {
		try {
			if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
				if(publicPlan.getVariant()!=null && publicPlan.getVariant().getVariant()!=null) {
					return publicPlan.getVariant().getVariantAsKeyPairString();
				} else {
					return "";
				}
			} else if(planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan)planExportInfo.getPlanInfo();
				//will be null for old tests
				if(plan.getVariant()!=null && plan.getVariant().getVariant()!=null) {
					return plan.getVariant().getVariantAsKeyPairString();
				} else {
					return "";
				}
			} else if(planExportInfo.getPlanInfo() instanceof Document) {
				Document doc = (Document)planExportInfo.getPlanInfo();
				return String.valueOf(doc.get("variant"));
			}
		} catch (Exception ex) {
			logger.error("Error getting variant from " + planExportInfo.getPlanInfo().getClass() + " id=" + getPlanId(), ex);
		}
		//old plans don't have variants, this can happen
		return "";
	}
	public String getPlanId() {
		try {
			if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
				return publicPlan.getId();
			} else if(planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan)planExportInfo.getPlanInfo();
				return plan.getId();
			} else if(planExportInfo.getPlanInfo() instanceof Document) {
				Document doc = (Document)planExportInfo.getPlanInfo();
				return doc.getString("_id");
			}
		} catch (Exception ex) {
			logger.error("Error getting id from " + planExportInfo.getPlanInfo().getClass(), ex);
		}

		throw new RuntimeException("Error getting plan id. This is a bug in the suite.");
	}
	public String getPlanLink() {
		return this.suiteBaseUrl + "/plan-detail.html?public=true&plan=" + getPlanId();
	}

	public String getPlanDescription() {
		try {
			if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
				return publicPlan.getDescription();
			} else if(planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan)planExportInfo.getPlanInfo();
				return plan.getDescription();
			} else if(planExportInfo.getPlanInfo() instanceof Document) {
				Document doc = (Document)planExportInfo.getPlanInfo();
				return doc.getString("description");
			}
		} catch (Exception ex) {
			logger.error("Error getting plan description from " + planExportInfo.getPlanInfo().getClass() + " id=" + getPlanId(), ex);
		}

		throw new RuntimeException("Error getting plan descripton. This is a bug in the suite.");
	}

	public String getCertificationProfileName() {
		try {
			if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
				return publicPlan.getCertificationProfileName();
			} else if(planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan)planExportInfo.getPlanInfo();
				return plan.getCertificationProfileName();
			}
		} catch (Exception ex) {
			logger.error("Error getting certification profile name from " + planExportInfo.getPlanInfo().getClass() + " id=" + getPlanId(), ex);
		}

		return null;
	}

	public String getPlanVersion() {
		try {
			if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
				PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
				return publicPlan.getVersion();
			} else if(planExportInfo.getPlanInfo() instanceof Plan) {
				Plan plan = (Plan)planExportInfo.getPlanInfo();
				return plan.getVersion();
			} else if(planExportInfo.getPlanInfo() instanceof Document) {
				Document doc = (Document)planExportInfo.getPlanInfo();
				return doc.getString("version");
			}
		} catch (Exception ex) {
			logger.error("Error getting plan version from " + planExportInfo.getPlanInfo().getClass() + " id=" + getPlanId(), ex);
		}

		throw new RuntimeException("Error getting plan version. This is a bug in the suite.");
	}
	public String getPlanStarted() {
		if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
			PublicPlan publicPlan = (PublicPlan)planExportInfo.getPlanInfo();
			return publicPlan.getStarted();
		} else if(planExportInfo.getPlanInfo() instanceof Plan) {
			Plan plan = (Plan)planExportInfo.getPlanInfo();
			return plan.getStarted();
		} else if(planExportInfo.getPlanInfo() instanceof Document) {
			Document doc = (Document)planExportInfo.getPlanInfo();
			return doc.getString("started");
		}
		throw new RuntimeException("Error getting plan started timestamp. This is a bug in the suite.");
	}
	public String getPlanOwner() {
		if(planExportInfo.getPlanInfo() instanceof PublicPlan) {
			return "";
		} else if(planExportInfo.getPlanInfo() instanceof Plan) {
			Plan plan = (Plan)planExportInfo.getPlanInfo();
			return plan.getOwner().get("sub") + " " + plan.getOwner().get("iss");
		} else if(planExportInfo.getPlanInfo() instanceof Document) {
			Document doc = (Document)planExportInfo.getPlanInfo();
			return String.valueOf(doc.get("owner"));
		}
		throw new RuntimeException("Error getting plan owner. This is a bug in the suite.");
	}

	public List<TestHelper> getTestHelpers() {
		List<TestHelper> helpers = new ArrayList<>();
		for(PlanExportInfo.TestExportInfoHolder holder : planExportInfo.getTestLogExports()) {
			TestHelper testHelper = new TestHelper(holder.getExport(), suiteBaseUrl);
			helpers.add(testHelper);
		}
		return helpers;
	}

}
