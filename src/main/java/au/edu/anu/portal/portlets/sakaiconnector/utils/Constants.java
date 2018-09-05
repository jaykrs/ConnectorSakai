/**
 * Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.utils;

public class Constants {

	/* portlet height settings */
	public static final int PORTLET_HEIGHT_400 = 400;
	public static final int PORTLET_HEIGHT_600 = 600;
	public static final int PORTLET_HEIGHT_800 = 800;
	public static final int PORTLET_HEIGHT_1200 = 1200;
	public static final int PORTLET_HEIGHT_1600 = 1600;
	
	public static final int PORTLET_HEIGHT_DEFAULT=PORTLET_HEIGHT_600;
	
	public static final String PORTLET_TITLE_DEFAULT="LMS-Liferay connector";

	//cache name - if this changes, update ehcache.xml as well, and vice versa
	public static final String CACHE_NAME = "au.edu.anu.portal.portlets.cache.SakaiConnectorPortletCache";

	
}
