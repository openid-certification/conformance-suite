var FAPI_UI = {

	logTemplates : {},

	loadHomepageTemplates: function() {
		this.logTemplates.INDEX_START = _.template($("#indexTemplate_Start").html());
		this.logTemplates.INDEX_FORM = _.template($("#indexTemplate_Form").html());
		this.logTemplates.INDEX_RUNNNING_TESTS = _.template($("#indexTemplate_RunningTests").html());
		this.logTemplates.INDEX_END = _.template($("#indexTemplate_End").html());
	},
	
	loadTemplates: function() {
		this.logTemplates.LOG_START = _.template($("#logDetailTemplate_LogStart").html());
		this.logTemplates.LOG_DETAIL = _.template($("#logDetailTemplate").html());
		this.logTemplates.SOURCE = _.template($("#logDetailTemplate_Source").html());
		this.logTemplates.MESSAGE = _.template($("#logDetailTemplate_Message").html());
		this.logTemplates.REQUIREMENTS = _.template($("#logDetailTemplate_Requirements").html());
		this.logTemplates.UPLOAD = _.template($("#logDetailTemplate_Upload").html());
		this.logTemplates.RESULT = _.template($("#logDetailTemplate_Result").html());
		this.logTemplates.TIME = _.template($("#logDetailTemplate_Time").html());
		this.logTemplates.MORE = _.template($("#logDetailTemplate_More").html());
		this.logTemplates.LOG_END = _.template($("#logDetailTemplate_LogEnd").html());
	},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],
	testResults : {success:0, warning:0, failure:0, interrupted:0, review:0, default:0, total:0},
	
	/**
	 * 
	 */
	goHome : function() {
		window.location.replace("index.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: render
	    });
	},

}

