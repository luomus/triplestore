<div class="clear"></div>
<ul id="menu">
	<li id="taxonToolMenuMove">Move as ...</li>
	<#if !taxon.synonym>
		<#if taxon.hidden>
			<li id="taxonToolMenuUnhide">Unhide</li>
		<#else>
			<li id="taxonToolMenuHide">Hide</li>
		</#if>
	</#if>
	<#if taxon.deleteable>
		<li id="taxonToolMenuDelete"><span class="ui-icon ui-icon-trash"></span>Delete</li>
	</#if>
	<#if taxon.hasCriticalData()>
		<li class="ui-state-disabled"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
		<li id="taxonToolMenuCritical"><span class="ui-icon ui-icon-alert"></span> Manage critical data</li>
	<#else>
		<#if taxon.synonym>
			<li id="taxonToolMenuDetachSynonym"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
		<#else>
			<li id="taxonToolMenuDetach"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
		</#if>
	</#if>
	<#if taxon.synonym>
		<li id="taxonToolMenuEditFull">Edit (show all fields)</li>
	</#if>
</ul>