package net.openid.conformance.export;

import com.google.gson.Gson;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.StringWriter;
import java.util.Locale;

@Service
public class HtmlExportRenderer {

	@Autowired
	private SpringTemplateEngine exportRenderingTemplateEngine;

	@Value("${fintechlabs.base_url}")
	private String suiteBaseUrl;

	private Gson gson;
	private String planTemplateName = "self-contained-export/plan.html";
	private String testTemplateName = "self-contained-export/test.html";
	private String logEntryTemplateName = "self-contained-export/log-entry.html";

	public HtmlExportRenderer() {
		gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson(true);
	}

	/**
	 * Used in unit test
	 * @param noAutoWiring
	 */
	@SuppressWarnings("unused")
	public HtmlExportRenderer(boolean noAutoWiring) {
		this();
		ExportRenderingTemplateEngineBean bean = new ExportRenderingTemplateEngineBean();
		this.exportRenderingTemplateEngine = bean.exportRenderingTemplateEngine();
		this.planTemplateName = "/templates/" + this.planTemplateName;
		this.testTemplateName = "/templates/" + this.testTemplateName;
		this.logEntryTemplateName = "/templates/" + this.logEntryTemplateName;
	}


	public String createHtmlForPlan(PlanExportInfo exportInfo) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		PlanHelper helper = new PlanHelper(exportInfo, suiteBaseUrl);
		thymleafContext.setVariable("planHelper", helper);
		StringWriter writer = new StringWriter();
		exportRenderingTemplateEngine.process(planTemplateName, thymleafContext, writer);
		return writer.toString();
	}

	public String createHtmlForTestLogs(TestExportInfo export) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		TestHelper helper = new TestHelper(export, suiteBaseUrl);
		thymleafContext.setVariable("helper", helper);

		for(Document testResult : helper.getTestResults()){
			LogEntryHelper logEntryHelper = new LogEntryHelper(testResult, gson);
			helper.addLogEntryHelper(logEntryHelper);
		}
		StringWriter writer = new StringWriter();
		exportRenderingTemplateEngine.process(testTemplateName, thymleafContext, writer);
		return writer.toString();
	}

	public String createHtmlForLogEntry(Document logEntry) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		LogEntryHelper item = new LogEntryHelper(logEntry, gson);
		thymleafContext.setVariable("item", item);
		StringWriter writer = new StringWriter();
		exportRenderingTemplateEngine.process(logEntryTemplateName, thymleafContext, writer);
		return writer.toString();
	}

}
