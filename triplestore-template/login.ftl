<#include "luomus-header.ftl">

<div id="lajiAuthLogin">

	<#if error??>
		<div class="error"> ${error} </div>
	</#if>

<h4>Login</h4>

	<ul>
		<li><a href="${hakaURI}"><img src="${staticURL}/img/haka.gif" alt="HAKA" /></a></li>
		<li><hr/>LUOMUS LOGIN SOON TO BE REMOVED. LINK HAKA TO YOUR ACCOUNT: <br/><a href="${ltkmURI}"><img src="${staticURL}/img/luomus.png" alt="Luomus" /></a></li>
	</ul>
	
</div>

<#include "luomus-footer.ftl">