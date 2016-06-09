<#include "luomus-header.ftl">

<div id="lajiAuthLogin">
<h4>Use your organization's login</h4>
	<#if lajiAuthError??>
		<div class="error"> ${lajiAuthError} </div>
	</#if>
	<ul>
		<li><a href="${ltkmURI}"><img src="${staticURL}/img/luomus.png" alt="Luomus" /></a></li>
	</ul>
</div>

<#include "luomus-footer.ftl">