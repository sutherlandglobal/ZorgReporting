package zorg.api;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import zorg.report.ZorgRoster;

public class GetAgentNames extends HttpServlet 
{
	private static final long serialVersionUID = -191126832774810020L;

	private GetAgentNames() {}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		ZorgRoster roster = null;
		PrintWriter out = null;
		try
		{
			roster = new ZorgRoster();
			//tell site logs where we're coming from
			roster.getParameters().setSource("UI");
			
			roster.setIncludeAllUsers(true);
			roster.load();
			
			TreeSet<String> agentSet = new TreeSet<String>();
			
			for(String userID : roster.getUserIDs())
			{
				agentSet.add(roster.getFullName(userID));
			}
			
			out = response.getWriter();
			
			for(String agentName : agentSet)
			{
				out.println(agentName);
			}
		}
		catch (Exception t)
		{
			t.printStackTrace();
				
			//simple message for the user
			try 
			{
				response.getWriter().println("Error: " + t.getMessage());
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		finally
		{
			if(roster != null)
			{
				roster.close();
			}
			
			if(out != null)
			{
				out.close();
			}
		}
	}
}
