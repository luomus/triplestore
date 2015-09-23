<#include "luomus-header.ftl">

<h4>Login in using your FMNH username and password</h4>

<form action="login" method="post" name="login">
<input type="hidden" name="originalURL" value="${originalURL!""}" />

<table id="login">
	<#if error?has_content>
		<thead><tr><td colspan="2" class="error"> ${error} </td></tr></thead>
	</#if>
	<tfoot><tr><td colspan="2"> <button onclick="document.login.submit();">Login</button> </td></tr></tfoot>
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

<#include "luomus-footer.ftl">