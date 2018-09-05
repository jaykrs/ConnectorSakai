<%--

   Author : Jayant Kumar

--%>
<%@ page contentType="text/html" isELIgnored="false" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet" %>

<portlet:defineObjects /> 

<div class="sakaiconnector-portlet">

	<div class="portlet-msg-error">
		<h2>${errorHeading}</h2>
		<br class="clear"> 
		<p>
			<c:choose>
				<c:when test="${not empty errorLink}">
					<a href="${errorLink}">${errorMessage}</a>
				</c:when>
				<c:otherwise>
					${errorMessage}
				</c:otherwise>
			</c:choose>
		</p>
	</div>
	
</div>
	

