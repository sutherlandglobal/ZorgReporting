package zorg.constants;

public final class Constants 
{
	private Constants(){}
	
	public final static String HELIOS_SITE_NAME = "Zorg";
	public final static String HELIOS_READABLE_SITE_NAME = "Zorg Industries";
	public final static String REPORT_CLASS_PREFIX = "zorg.report.";
	
	public final static String PLATFORM_DIR = "/opt/tomcat/apache-tomee-plus";
	public final static String PRIVATE_LABEL_PROD_DB = PLATFORM_DIR + "/webapps/" + HELIOS_SITE_NAME +"/WEB-INF/lib/conf/database/rocjfsdbs27.properties";
	public final static String PRIVATE_LABEL_DEV_DB = PLATFORM_DIR + "/webapps/" + HELIOS_SITE_NAME +"/WEB-INF/lib/conf/database/rocjfsdev18.properties";
	public final static String REPORT_LOGGER_HANDLE = "zorg_reporting";
	public final static String API_LOGGER_HANDLE = "zorg_api";
	
	public final static String SITE_JAR = PLATFORM_DIR + "/webapps/" + HELIOS_SITE_NAME +"/WEB-INF/lib/ZorgReporting.jar";

}
