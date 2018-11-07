# ConnectorSakai
Liferay 7 Sakai Connector
this Liferay portlet for Liferay & Sakai Integration

Sakai is exposing through LTI and Liferay is consuming LTI services

we need to have some configution on sakai.properties file to enable LTI and update sakai.properties file as below.

basiclti.provider.enabled=true
basiclti.provider.allowedtools=sakai.announcements:sakai.singleuser:sakai.assignment.grades:blogger:sakai.dropbox:sakai.forums:sakai.gradebook.tool:sakai.poll:sakai.resources:sakai.schedule	Allowed tools for LTI , can add more tools based on feasibility supported by tools provider
basiclti.provider.highly.trusted.consumers=liferaydemo.xxxxxxxx.com
basiclti.provider.liferaydemo.xxxxxxxx.com.secret=xxxxanypwdxxxx 
basiclti.provider.email.trusted.consumers=
basiclti.provider.newsitetype=Course
lti.role.mapping.Student=Member
lti.role.mapping.Instructor=Owner
basiclti.contentlink.enabled=true
webservices.allowlogin=true
webservices.allow=.*
webservices.deny=
webservices.log-allowed=true

Configuration on Liferay side in liferay portlet.xml



deploy war file in liferay .

This has been tested and working on liferay 7.


Feel free to reach me on jaykrs@gmail.com in case of any issue with subject line liferaysakai connector.
