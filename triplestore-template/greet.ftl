<#include "luomus-header.ftl">

<h4>Welcome, ${user.fullname}!</h4>

<p>Please, select your service.</p>

<ul class="servicelist">
	<li><a href="${taxonomyEditorBaseURL}">Taxon editor</a></li>
	<#if user.isAdmin()>
		<li><a href="${baseURL}/editor">Triplestore</a> (admin only)</li>
		<li><a href="${baseURL}/it">Information Systems</a> (admin only)</li>
		<li><a href="https://login.laji.fi/admin/unapproved">Laji-Auth</a></li>
	</#if>
</ul>

<#include "luomus-footer.ftl">