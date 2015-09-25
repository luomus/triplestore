<#include "luomus-header.ftl">

<div id="luomusLogin">
<h4>Log in using LUOMUS username</h4>

<form action="login" method="post" name="login">
<input type="hidden" name="originalURL" value="${originalURL!""}" />

<table id="login">
	<#if error?has_content>
		<thead><tr><td colspan="2" class="error"> ${error} </td></tr></thead>
	</#if>
	<tfoot><tr><td></td><td> <button onclick="document.login.submit();">Login</button> </td></tr></tfoot>
	<tbody>
		<tr>
			<td><label for="username">Username</label></td>
			<td><input type="text" id="username" name="username" value="${username!""}" /></td>
		</tr>
		<tr>
			<td><label for="password">Password</label></td>
			<td><input type="password" id="password" name="password" value="" /></td>
		</tr>
	</tbody>
</table>
</form>
</div>

<div id="lajiAuthLogin">
<h4>Or use your own organization's login</h4>
	<#if lajiAuthError??>
		<div class="error"> ${lajiAuthError} </div>
	</#if>
	<ul>
		<li><a href="${hakaURI}"><img src="${staticURL}/img/haka.gif" alt="HAKA" /></a></li>
		<li><a href="${virtuURI}"><img src="${staticURL}/img/virtu.png" alt="VIRTU" /></a></li>
	</ul>
</div>

<#include "luomus-footer.ftl">