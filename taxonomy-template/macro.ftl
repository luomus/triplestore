<#function same qname1="" qname2="">
	<#return qname1 == qname2>
</#function>

<#function booleanValue value="">
	<#if value?string == ""><#return ""></#if>
	<#return value?string("true","false")>
</#function>

<#macro printScientificNameAndAuthor taxon><span class="scientificName <#if taxon.isSpecies()>speciesName</#if>">${taxon.scientificName!taxon.getVernacularName("en")!taxon.qname}</span><span class="author">${taxon.scientificNameAuthorship!""}</span></#macro>

<#macro printEditorExpert taxon><@printEditorExpertSpecific taxon.editors taxon.experts /></#macro>

<#macro printEditorExpertSpecific editors experts printNoValue=true><@compress single_line=true>
	<#if editors?has_content>
		<span class="editor">Editors: 
			<#list editors as editorQname>${persons[editorQname.toString()].fullname}<#if editorQname_has_next>, </#if></#list>
		</span>
	<#elseif printNoValue><span class="editor">Editors: [admin only]</span> </#if>
	<#if experts?has_content>
		<span class="expert">Experts: 
			<#list experts as expertQname>${persons[expertQname.toString()].fullname}<#if expertQname_has_next>, </#if></#list>
		</span>
	<#elseif printNoValue><span class="expert">Experts:[none]</span></#if>
</@compress></#macro>

<#macro taxonChildsTools parentTaxon><@compress single_line=true>
	<div class="taxonChildTools">
		<div>
			<h3>Children of ${parentTaxon.scientificName!parentTaxon.getVernacularName("en")!parentTaxon.qname}</h3>
			<button class="closeTaxonChildsButton" title="Close" onclick="collapseTaxonByCloseButton(this)">&#215;</button>
			<#if parentTaxon.allowsAlterationsBy(user)>
				<button class="enableSortingButton" onclick="enableSorting(this);">Enable sorting</button>
			</#if>
			<a href="${baseURL}/${parentTaxon.qname}" class="button" onclick="changeRoot(this, '${baseURL}/${parentTaxon.qname}'); return false;">Use as work root</a>
		</div>
		<div class="clear"></div>
		<div class="taxonEditorExpert">
			<@printEditorExpert parentTaxon />
		</div>
		<div class="sortingControls ui-widget ui-widget-header""><button class="saveSortingButton" onclick="saveSorting(this);">Save sorting</button> <button onclick="cancelSorting(this);">Cancel</button></div>
		<div class="clear"></div>
	</div>
</@compress></#macro>

<#macro taxonChildsToolsBottom parentTaxon><@compress single_line=true>
	<#if parentTaxon.allowsAlterationsBy(user)>
		<div class="taxonChildTools taxonChildToolsBottom">
			<div>
				<button class="addNewChildButton" onclick="addNewChild(this);">Add child</button>
			</div>
		</div>
	</#if>
</@compress></#macro>

<#macro printTaxon taxon additionalClass="" showSynonymsAndSynonymTools=true showChildrenTools=true>
	<div class="taxonWithTools ${additionalClass} <#if taxon.hasChildren()>hasChildren</#if>" id="${taxon.qname?replace(".","")}">
		<div class="taxonInfo <#if taxon.taxonRank?has_content>${taxon.taxonRank?replace("MX.","")}<#else>unranked</#if>" onclick="editTaxon(this); return false;">
			<span class="taxonRank">[<#if taxon.taxonRank?has_content>${taxon.taxonRank?replace("MX.","")}<#else></#if>]</span> <@printScientificNameAndAuthor taxon />
			<#if additionalClass == "rootTaxon">
				<div class="taxonEditorExpert">
					<@printEditorExpert taxon />
				</div>
			<#else>
				<div class="taxonEditorExpert">
					<@printEditorExpertSpecific taxon.explicitlySetEditors taxon.explicitlySetExperts false />
				</div>
			</#if>
			<#if taxon.checklist?? && taxon.hasParent() && !same(taxon.parent.checklist, taxon.checklist)>
				<div class="checklistChangesMidTree">
					Checklist: ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
				</div>
			</#if> 
		</div>
		<#if showChildrenTools>
			<div class="showChildrenTools">
				<button class="treePlusMinusSign taxonToolButton" onclick="treePlusMinusSignClick(this);">
					<span class="ui-icon ui-icon-plus"></span>
				</button>
				<span class="taxonChildCount">(${taxon.childTaxons?size})</span>
			</div>
		</#if>
		<#if showSynonymsAndSynonymTools && showSynonymsMode??>
			<div class="synonyms ui-widget ui-widget-header" id="${taxon.qname?replace(".","")}Synonyms">
				<#list taxon.synonymTaxons as synonymTaxon>	 
					<@printTaxon synonymTaxon "" false false />
				</#list>
				<#if !taxon.synonymTaxons?has_content>
					<span class="noSynonymsText">(no synonyms)</span>
				</#if>
				<#if taxon.allowsAlterationsBy(user)><button class="addSynonymButton taxonToolButton" onclick="addNewSynonym(this);">Add synonym</button></#if>
			</div>
		</#if>
	</div>
</#macro>

<#macro loadingSpinner><@compress single_line=true>
	<div class="ui-corner-all loadingSpinner"> 
    	Loading... &nbsp; &nbsp; <img src="${staticURL}/img/loading.gif" alt="Loading.." /> 
    </div>
</@compress></#macro>

<#macro showValidationResults>
<#if validationResults?? && validationResults.hasErrorsOrWarnings()>	
	<div id="validationDialog">
		<h2>Validation errors and warnings</h2>
		<p class="info">
			Changes were made, but there were following errors or warnings, which should be looked into and corrected.
		</p>
		<table class="validationResults">
		<#list validationResults.errors as error>
			<tr class="errorMessage"><td>${error.field}</td><td>${error.message}</td></tr>
		</#list>
		<#list validationResults.warnings as warning>
			<tr class="warningMessage"><td>${warning.field}</td><td>${warning.message}</td></tr>
		</#list>
		</table>
	</div>
</#if>
</#macro>
