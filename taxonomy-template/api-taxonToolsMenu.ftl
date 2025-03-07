<ul id="menu">
	<#if !taxon.hasTreeRelatedCriticalData()>
		<li id="taxonToolMenuMove">Move as ...</li>
	<#else>
		<li class="ui-state-disabled">Move as ...</li>
	</#if>
	<#if !taxon.synonym>
		<#if taxon.hidden>
			<li id="taxonToolMenuUnhide">Unhide</li>
		<#else>
			<li id="taxonToolMenuHide">Hide</li>
		</#if>
		<li id="taxonToolMenuAddChildBelow">Add sibling after this taxon</li>
	</#if>
	<#if taxon.deletable>
		<li id="taxonToolMenuDelete"><span class="ui-icon ui-icon-trash"></span>Delete</li>
	</#if>
	<#if taxon.detachable>
		<#if taxon.synonym>
			<#if taxon.synonymParent??>
				<li id="taxonToolMenuDetachSynonym"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
			</#if>
		<#else>
			<li id="taxonToolMenuDetach"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
		</#if>
	<#else>
		<li class="ui-state-disabled"><span class="ui-icon ui-icon-scissors"></span>Detach</li>
		<li id="taxonToolMenuCritical"><span class="ui-icon ui-icon-alert"></span> Manage critical data</li>
	</#if>
	<#if taxon.synonym>
		<li id="taxonToolMenuEditFull">Edit (show all fields)</li>
	</#if>
</ul>