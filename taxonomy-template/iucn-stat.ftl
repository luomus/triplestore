<#assign groupSpeciesCount = data.speciesOfGroup?size>
<#assign readyCount = data.getYear(year).readyCount>
<#if groupSpeciesCount != 0>
	<#assign percent = (readyCount/groupSpeciesCount*100)?ceiling >
<#else>
	<#assign percent = 0>
</#if>

<span class="stat ready">${readyCount}</span> 
/
<span class="stat <#if (percent>=100)>ready<#else>total</#if>">${groupSpeciesCount}</span>
<span class="stat <#if (percent>=100)>ready<#else>total</#if>">${percent}%</span>

