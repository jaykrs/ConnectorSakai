/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.models;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * An object to store information about a page in a site, and the List of tools contained within it.
 * 
 *
 */
@NoArgsConstructor
@Data
@Root(name="page")
public class Page {

	@Attribute(name="id")
	private String id;
	
	@Element(name="page-title")
	private String title;
	
	@ElementList(name="tools")
	private List<Tool> tools;

	public List<Tool> getTools() {
		return tools;
	}

	public void setTools(List<Tool> tools) {
		this.tools = tools;
	}
	
}
