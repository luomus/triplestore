<#include "macro.ftl">
<#if isSynonym>
	<@printTaxon taxon "synonym" false false />
<#else>
	<@printTaxon taxon />
</#if>
