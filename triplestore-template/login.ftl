<#include "luomus-header.ftl">

<div id="lajiAuthLogin">

	<#if error??>
		<div class="error"> ${error} </div>
	</#if>

<h4>Use your organization's login</h4>
	<ul>
		<li><a href="${ltkmURI}"><img src="${staticURL}/img/luomus.png" alt="Luomus" /></a></li>
	</ul>
</div>

<#include "luomus-footer.ftl">