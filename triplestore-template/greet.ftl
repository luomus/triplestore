<#include "luomus-header.ftl">

<h4>Welcome, ${user.fullname}!</h4>

<p>Please, select your service.</p>

<ul class="servicelist">
	<li><a href="${taxonomyEditorBaseURL}">Taxonomy editor</a></li>
	<#if user.isAdmin()><li><a href="${baseURL}/editor">Triplestore</a> (admin only)</li></#if>
</ul>

<#include "luomus-footer.ftl">