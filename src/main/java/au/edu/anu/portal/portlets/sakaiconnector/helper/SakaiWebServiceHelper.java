/**
 * Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.helper;

import java.util.List;

import au.edu.anu.portal.portlets.sakaiconnector.logic.SakaiWebServiceLogic;
import au.edu.anu.portal.portlets.sakaiconnector.models.Site;
import au.edu.anu.portal.portlets.sakaiconnector.models.Tool;

/**
 * This is a static cover for the logic class, used until we convert to Spring.
 * 
 * 
 */
public class SakaiWebServiceHelper {

	
	public static String getRemoteUserIdForUser(SakaiWebServiceLogic logic, String eid) {
		return logic.getRemoteUserIdForUser(eid);
	}
	
	public static List<Site> getAllSitesForUser(SakaiWebServiceLogic logic, String eid) {
		return logic.getAllSitesForUser(eid);
	}
	
	public static List<Tool> getToolsForSite(SakaiWebServiceLogic logic, String siteId, String eid) {
		return logic.getToolsForSite(siteId, eid);
	}
	
}
