/**
 * 
 */
package com.sutherland.zorg.report;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.sutherland.helios.api.report.frontend.ReportFrontEndGroups;
import com.sutherland.helios.data.Aggregation;
import com.sutherland.helios.database.connection.SQL.ConnectionFactory;
import com.sutherland.helios.database.connection.SQL.RemoteConnection;
import com.sutherland.helios.date.formatting.DateFormatter;
import com.sutherland.helios.date.parsing.DateParser;
import com.sutherland.helios.exceptions.DatabaseConnectionCreationException;
import com.sutherland.helios.exceptions.ExceptionFormatter;
import com.sutherland.helios.exceptions.ReportSetupException;
import com.sutherland.helios.logging.LogIDFactory;
import com.sutherland.helios.report.Report;
import com.sutherland.helios.report.parameters.groups.ReportParameterGroups;
import com.sutherland.helios.util.results.Filter;
import com.sutherland.zorg.datasources.DatabaseConfigs;


/**
 * @author Jason Diamond
 *
 */
public class TopCaseDrivers extends Report
{
	private RemoteConnection dbConnection;
	private ZorgRoster roster;
	private final String dbPropFile = DatabaseConfigs.PRIVATE_LABEL_PROD_DB;
	private final static Logger logger = Logger.getLogger(TopCaseDrivers.class);
	
	public static String uiGetReportName()
	{
		return "Top Case Drivers";
	}
	
	public static String uiGetReportDesc()
	{
		return "Determines the most common case categories.";
	}
	
	public final static LinkedHashMap<String, String> uiSupportedReportFrontEnds = ReportFrontEndGroups.BASIC_METRIC_FRONTENDS;
	
	public final static LinkedHashMap<String, ArrayList<String>> uiReportParameters = ReportParameterGroups.DRIVERS_REPORT_PARAMETERS;
	
	/**
	 * Build the report object.
	 * 
	 * @throws ReportSetupException		If a failure occurs during creation of the report or its resources.
	 */
	public TopCaseDrivers() throws ReportSetupException
	{
		super();
	}
	
	/* (non-Javadoc)
	 * @see helios.Report#setupReport()
	 */
	@Override
	protected boolean setupReport() 
	{
		boolean retval = false;

		try
		{
			reportName = TopCaseDrivers.uiGetReportName();
			reportDesc = TopCaseDrivers.uiGetReportDesc();
			
			for(Entry<String, ArrayList<String>> reportType : uiReportParameters.entrySet())
			{
				for(String paramName :  reportType.getValue())
				{
					getParameters().addSupportedParameter(paramName);
				}
			}
			
			retval = true;
		}
		catch (Exception e)
		{
			setErrorMessage("Error setting up report");
			
			logErrorMessage(getErrorMessage());
			logErrorMessage(ExceptionFormatter.asString(e));
		}

		return retval;
	}
	
	@Override
	protected boolean setupLogger() 
	{
		logID = LogIDFactory.getLogID().toString();

		if (MDC.get(LOG_ID_PREFIX) == null) 
		{
			MDC.put(LOG_ID_PREFIX, LOG_ID_PREFIX + logID);
		}

		return (logger != null);
	}
	
	/* (non-Javadoc)
	 * @see helios.Report#setupDataSourceConnections()
	 */
	@Override
	protected boolean setupDataSourceConnections()
	{
		boolean retval = false;

		try 
		{
			ConnectionFactory factory = new ConnectionFactory();
			
			factory.load(dbPropFile);
			
			dbConnection = factory.getConnection();
		} 
		catch(DatabaseConnectionCreationException e )
		{
			setErrorMessage("DatabaseConnectionCreationException on attempt to access database");
			
			logErrorMessage(getErrorMessage());
			logErrorMessage(ExceptionFormatter.asString(e));
		}
		finally
		{
			if(dbConnection != null)
			{
				retval = true;
			}
		}

		return retval;
	}

	/* (non-Javadoc)
	 * @see helios.Report#runReport(java.lang.String, java.lang.String)
	 */
	@Override
	protected ArrayList<String[]> runReport() throws Exception
	{
		ArrayList<String[]> retval = new ArrayList<String[]>();
		
		String query = "SELECT CRM_TRN_PROSPECT.PROSPECT_CREATEDBY,CRM_TRN_PROSPECT.PROSPECT_CREATEDDATE,CRM_MST_REFVALUES.REFVAL_DISPLAYVALUE,CRM_MST_REFVALUES_1.REFVAL_DISPLAYVALUE " + 
				" FROM (CRM_TRN_PROSPECT INNER JOIN CRM_MST_REFVALUES ON CRM_TRN_PROSPECT.PROSPECT_OOSRTPREASONID = CRM_MST_REFVALUES.REFVAL_REFVALID) INNER JOIN CRM_MST_REFVALUES AS CRM_MST_REFVALUES_1 ON CRM_TRN_PROSPECT.PROSPECT_SRCOFCHARGEID = CRM_MST_REFVALUES_1.REFVAL_REFVALID " + 
				" WHERE CRM_TRN_PROSPECT.PROSPECT_CREATEDDATE >= '" + 
				getParameters().getStartDate() + 
				"' AND CRM_TRN_PROSPECT.PROSPECT_CREATEDDATE <= '" + 
				getParameters().getEndDate() + 
				"' AND CRM_MST_REFVALUES.REFVAL_DISPLAYVALUE is not null" ;
		
		roster = new ZorgRoster();
		roster.setChildReport(true);
		roster.getParameters().setAgentNames(getParameters().getAgentNames());
		roster.getParameters().setTeamNames(getParameters().getTeamNames());
		roster.load();
		
		Aggregation reportGrainData = new Aggregation();

		String driver;
		String userID;
		String reportGrain; 
		
		//don't assign time grain just yet. in case this is a non-time report, because the timegrain param is not guaranteed to be set 
		int timeGrain, dateFormat;
		
		for(String[] row : dbConnection.runQuery(query))
		{
			userID = row[0];
			driver = row[2] + "-" + row[3];

			if(roster.hasUser(userID))
			{
				timeGrain = Integer.parseInt(getParameters().getTimeGrain());
				dateFormat = Integer.parseInt(getParameters().getDateFormat());
				reportGrain = DateFormatter.getFormattedDate(DateParser.convertSQLDateToGregorian(row[1]), timeGrain, dateFormat);

				reportGrainData.addDatum(reportGrain);
				reportGrainData.getDatum(reportGrain).addAttribute(driver);
				reportGrainData.getDatum(reportGrain).addData(driver, userID);	
			}
		}
		
		for( Entry<String, String> queryStats  : dbConnection.getStatistics().entrySet())
		{
			logInfoMessage( "Query " + queryStats.getKey() + ": " + queryStats.getValue());
		}

		for(String grain : reportGrainData.getDatumIDList())
		{
			for(String[] row : Filter.filterTopDrivers(reportGrainData.getDatum(grain), Integer.parseInt(getParameters().getNumDrivers())))
			{
				retval.add(new String[]{grain, row[0], row[1] });
			}
		}
		
		return retval;
	}

	/* (non-Javadoc)
	 * @see report.Report#close()
	 */
	@Override
	public void close()
	{
		if(roster != null)
		{
			roster.close();
		}

		if(dbConnection != null)
		{
			dbConnection.close();
		}

		super.close();	
		
		if (!isChildReport) 
		{
			MDC.remove(LOG_ID_PREFIX);
		}
	}
	
	@Override
	public ArrayList<String> getReportSchema() 
	{
		ArrayList<String> retval = new ArrayList<String>();
		
		retval.add("Date");
		retval.add("Driver");
		retval.add("Count");
		
		return retval;
	}
	
	@Override
	protected void logErrorMessage(String message) 
	{
		logger.log(Level.ERROR, message);
	}

	@Override
	protected void logInfoMessage(String message) 
	{
		logger.log(Level.INFO, message);
	}

	@Override
	protected void logWarnMessage(String message) 
	{
		logger.log(Level.WARN, message);
	}
}
