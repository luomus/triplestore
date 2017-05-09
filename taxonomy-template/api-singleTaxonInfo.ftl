<#include "macro.ftl">
<#if isSynonym>
	<@printTaxon taxon true />
<#else>
	<@printTaxon taxon />
</#if>
