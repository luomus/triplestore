<#include "luomus-header.ftl">

<div id="lajiAuthLogin">
<h4>Use your organization's login</h4>
	<#if lajiAuthError??>
		<div class="error"> ${lajiAuthError} </div>
	</#if>
	<ul>
		<li><a href="${hakaURI}"><img src="${staticURL}/img/haka.gif" alt="HAKA" /></a></li>
		<li><a href="${ltkmURI}"><img src="${staticURL}/img/luomus.png" alt="Luomus" /></a></li>
		<li><a href="${virtuURI}"><img src="${staticURL}/img/virtu.png" alt="VIRTU" /></a></li>
		<li><a href="${lajifiURI}"><img src="${staticURL}/img/laji_fi.jpg" alt="FINBIF" /><br />FinBIF-account</a></li>
	</ul>
</div>

<#include "luomus-footer.ftl">