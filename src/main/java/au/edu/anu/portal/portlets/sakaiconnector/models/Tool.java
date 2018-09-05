/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import au.edu.anu.portal.portlets.sakaiconnector.logic.SakaiWebServiceLogic;

/**
 * An object to store information about a tool.
 * 

 *
 */
@NoArgsConstructor
@Data
@Root(name="tool")
public class Tool {

	@Attribute(name="id")
	private String id;
	
	@Element(name="tool-id")
	private String registrationId;
	
	@Element(name="tool-title")
	private String title;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRegistrationId() {
		log.info("tool-id "+registrationId);
		return registrationId;
	}

	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	private static Log log = LogFactoryUtil.getLog(Tool.class);
}
