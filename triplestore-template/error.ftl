<#include "luomus-header.ftl">
<#include "macro.ftl">

<h4 class="errorMessage">Uups! An error has occured!</h4>

<p>ICT-team has been notified by email. Below may be some useful error message (or not). The message has already been send to ICT-team.</p>

<p>Message:</p>
<div class="errorMessage">
	<#if flashMessage?has_content> 
 		${flashMessage}
 	<#elseif error?has_content>
 		${error}
 	</#if>
</div>

<hr />

<p>You could continue using the system, but perhaps check with ICT-team first...</p>

<ul class="servicelist">
	<li><a href="${baseURL}/taxonomy-editor">Taxon editor</a></li>
	<#if user?? && user.isAdmin()><li><a href="${baseURL}/editor">Triplestore</a> (admin only)</li></#if>
</ul>

<#include "luomus-footer.ftl">

