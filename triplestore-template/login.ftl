<#include "luomus-header.ftl">

<div id="lajiAuthLogin">

	<#if error??>
		<div class="error"> ${error} </div>
	</#if>

<h4>Log in to Triplestore Editor</h4>

  <p>
	<a href="${lajiAuthLoginURI}"><button>Log in</button></a>
  </p>
	
</div>

<#include "luomus-footer.ftl">