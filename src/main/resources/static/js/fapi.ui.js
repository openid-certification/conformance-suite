var FAPI_UI = {

	logTemplates : {},
	
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
	},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],		
	
	/**
	 * 
	 */
	goHome : function() {
		window.location.replace("logs.html");
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

