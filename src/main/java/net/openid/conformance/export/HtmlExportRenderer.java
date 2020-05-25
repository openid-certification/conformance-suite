package net.openid.conformance.export;

import com.google.gson.Gson;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import org.bson.Document;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class HtmlExportRenderer {

	private SpringTemplateEngine templateEngine;
	private ClassLoaderTemplateResolver templateResolver;
	private Gson gson;

	public HtmlExportRenderer() {
		//TODO template engine should be a spring bean
		templateEngine = new SpringTemplateEngine();

		templateResolver = new ClassLoaderTemplateResolver();
		templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson(true);
	}

	//this is incomplete
	public String createHtmlForPlan(Map<String, Object> export) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		thymleafContext.setVariable("export", export);
		StringWriter writer = new StringWriter();
		templateEngine.process("/templates/self-contained-export/plan.html", thymleafContext, writer);
		return writer.toString();
	}

	public String createHtmlForTestLogs(Map<String, Object> export) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		TestHelper helper = new TestHelper(export);
		thymleafContext.setVariable("helper", helper);
		//Note: this is obviously less efficient, we could have done it using a single template
		//it is implemented this way to see if log api can be changed to use the same log entry rendering
		for(Document testResult : helper.getTestResults()){
			String html = createHtmlForLogEntry(testResult);
			helper.addRenderedResult(html);
		}
		StringWriter writer = new StringWriter();
		templateEngine.process("/templates/self-contained-export/test.html", thymleafContext, writer);
		return writer.toString();
	}

	public String createHtmlForLogEntry(Document logEntry) {
		Context thymleafContext = new Context();
		thymleafContext.setLocale(Locale.ENGLISH);
		LogEntryHelper item = new LogEntryHelper(logEntry, gson);
		thymleafContext.setVariable("item", item);
		StringWriter writer = new StringWriter();
		templateEngine.process("/templates/self-contained-export/log-entry.html", thymleafContext, writer);
		return writer.toString();
	}

}
