/**
 *   Author : Jayant Kumar
 */
package au.edu.anu.portal.portlets.sakaiconnector.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * A convenience object to hold a list of pages, in turn holding a list of tools,
 * retrieved from the web service call, when the XML is parsed.

 *
 */

@NoArgsConstructor
@Data
@Root(name="site")
public class SitePageList {

	@Attribute(name="id")
	private String id;
	
	@Element(name="pages")
	private PageList pageList;

	public PageList getPageList() {
		return pageList;
	}

	public void setPageList(PageList pageList) {
		this.pageList = pageList;
	}
	
}

