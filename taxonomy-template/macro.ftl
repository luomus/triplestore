<#function same qname1="" qname2="">
	<#return qname1 == qname2>
</#function>

<#macro printScientificNameAndAuthor taxon><span class="scientificName <#if taxon.isCursiveName()>speciesName</#if>">${taxon.scientificName!taxon.vernacularName.forLocale("en")!taxon.qname}</span><span class="author">${taxon.scientificNameAuthorship!""}</span></#macro>

<#macro printEditorExpert taxon><#if taxon.checklist?has_content><@printEditorExpertSpecific taxon.editors taxon.experts /></#if></#macro>

<#macro printEditorExpertSpecific editors experts printNoValue=true><@compress single_line=true>
	<#if editors?has_content>
		<span class="editor">Editors: 
			<#list editors as editorQname>${(persons[editorQname.toString()].fullname)!editorQname}<#if editorQname_has_next>, </#if></#list>
		</span>
	<#elseif printNoValue><span class="editor">Editors: [admin only]</span> </#if>
	<#if experts?has_content>
		<span class="expert">Experts: 
			<#list experts as expertQname>${(persons[expertQname.toString()].fullname)!expertQname}<#if expertQname_has_next>, </#if></#list>
		</span>
	<#elseif printNoValue><span class="expert">Experts:[none]</span></#if>
</@compress></#macro>

<#macro taxonChildsTools parentTaxon><@compress single_line=true>
	<div class="taxonChildTools">
		<div>
			<h3>Children of ${parentTaxon.scientificName!parentTaxon.vernacularName.forLocale("en")!parentTaxon.qname}</h3>
			<button class="closeTaxonChildsButton" title="Close" onclick="collapseTaxonByCloseButton(this)">&#215;</button>
			<#if parentTaxon.allowsAlterationsBy(user)>
				<button class="enableSortingButton" onclick="enableSorting(this);">Enable sorting</button>
			</#if>
			<a href="${baseURL}/${parentTaxon.qname}" class="button" onclick="changeRoot(this, '${baseURL}/${parentTaxon.qname}'); return false;">Use as work root</a>
		</div>
		<div class="clear"></div>
		<div class="sortingControls ui-widget ui-widget-header"">
			<button class="saveSortingButton" onclick="saveSorting(this);">Save sorting</button>
			<button class="sortAlphabeticallyButton" onclick="sortAlphabetically(this);">ABC..</button> 
			<button onclick="cancelSorting(this);">Cancel</button>
		</div>
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

<#macro printTaxon taxon isSynonym=false additionalClass="">
	<#assign allowsAlterationsByUserOnThis = taxon.allowsAlterationsBy(user)>
	<div class="taxonWithTools ${additionalClass} <#if isSynonym>synonym</#if> <#if taxon.hasChildren()>hasChildren</#if>" id="${taxon.qname?replace(".","")}">
		<div class="taxonInfo <#if taxon.taxonRank?has_content>${taxon.taxonRank?replace("MX.","")}<#else>unranked</#if>">
			<span class="taxonRank"><#if taxon.taxonRank?has_content>[${taxon.taxonRank?replace("MX.","")}]</#if></span> 
			
			<@printScientificNameAndAuthor taxon />
			<div class="icons">
				<#if taxon.markedAsFinnishTaxon><img class="finnishTaxonFlag" src="${staticURL}/img/flag_fi_small.png" title="Marked as finnish" /></#if>
				<#if taxon.hasCriticalData()><span class="criticalData ui-icon ui-icon-key" title="Taxon has critical data"></span></#if>
				<#if !isSynonym && taxon.species>
					<span class="taxonToolButton ui-icon ui-icon-gear <#if allowsAlterationsByUserOnThis>allowsAlterationsByUser</#if>" title="Tools"></span>
				</#if>
				<#if isSynonym && allowsAlterationsByUserOnThis>
					<#if additionalClass == "BASIONYM">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as basionym synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink basionym"></span></a>
					<#elseif additionalClass == "OBJECTIVE">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as objective synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink objective synonym"></span></a>
					<#elseif additionalClass == "SUBJECTIVE">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as subjective synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink subjective synonym"></span></a>
					<#elseif additionalClass == "HOMOTYPIC">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as homotypic synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink homotypic synonym"></span></a>
					<#elseif additionalClass == "HETEROTYPIC">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as heterotypic synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink heterotypic synonym"></span></a>
					<#elseif additionalClass == "SYNONYM">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink synonym"></span></a>
					<#elseif additionalClass == "MISSPELLED">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as misspelled name of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink misspelled name"></span></a>
					<#elseif additionalClass == "ORTOGRAPHIC">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as ortographic synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink ortographic synonym"></span></a>
					<#elseif additionalClass == "UNCERTAIN">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as uncertain synonym of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink uncertain synonym"></span></a>
					<#elseif additionalClass == "MISAPPLIED">
						<a href="#" onclick="unlinkSynonym(this, '${additionalClass}', ' as misapplied name of '); return false;"><span class="unlinkSynonymLink ui-icon ui-icon-close" title="Unlink misapplied name"></span></a>
					</#if>
				</#if>
			</div>
			<span class="vernacularNameFI">${taxon.vernacularName.forLocale("fi")!""}</span>
			
			<#if additionalClass == "rootTaxon">
				<div class="taxonEditorExpert">
					<@printEditorExpert taxon />
				</div>
			<#else>
				<div class="taxonEditorExpert">
					<@printEditorExpertSpecific taxon.explicitlySetEditors taxon.explicitlySetExperts false />
				</div>
			</#if>
			<#if !isSynonym>
				<#if !taxon.checklist??>
					<div class="checklistChangesMidTree">
						Checklist: Orphan taxa
					</div>
				<#elseif taxon.hasParent() && !same(taxon.parent.checklist, taxon.checklist)> 
					<div class="checklistChangesMidTree">
						Checklist: ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
					</div>
				</#if>
			<#else>
				<#if taxon.checklist??>
					<div class="checklistChangesMidTree">
						Checklist: ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
					</div>
				</#if>
			</#if>
			<#if taxon.hidden>
				<div class="checklistChangesMidTree">
					[Hidden]
				</div>
			</#if>
		</div>
		<#if !isSynonym>
			<div class="showChildrenTools">
				<button class="treePlusMinusSign taxonToolButton" onclick="treePlusMinusSignClick(this);">
					<span class="ui-icon ui-icon-plus"></span>
				</button>
				<span class="taxonChildCount">(${taxon.children?size})</span>
			</div>
		</#if>
		<#if !isSynonym && synonymsMode == "show">
			<div class="synonyms" id="${taxon.qname?replace(".","")}Synonyms">
				<@printSynonyms taxon.basionyms "Basionyms" "BASIONYM" /> 
				<@printSynonyms taxon.objectiveSynonyms "Objective synonyms" "OBJECTIVE" />
				<@printSynonyms taxon.subjectiveSynonyms "Subjective synonyms" "SUBJECTIVE" />
				<@printSynonyms taxon.homotypicSynonyms "Homotypic synonyms" "HOMOTYPIC" />
				<@printSynonyms taxon.heterotypicSynonyms "Heterotypic synonyms" "HETEROTYPIC" />
				<@printSynonyms taxon.synonyms "Synonyms" "SYNONYM" />
				<@printSynonyms taxon.misspelledNames "Misspelled names" "MISSPELLED" />
				<@printSynonyms taxon.ortographicVariants "Ortographical variants" "ORTOGRAPHIC" />
				<@printSynonyms taxon.uncertainSynonyms "Uncertain synonyms" "UNCERTAIN" />
 				<@printSynonyms taxon.misappliedNames "Misapplied names" "MISAPPLIED" />
			</div>		
			<button class="addSynonymButton taxonToolButton" onclick="addNewSynonym(this);">
				Add synonyms
			</button>
		</#if>
	</div>
</#macro>

<#macro printSynonyms synonyms label type>
	<#if synonyms?has_content>
		<div class="synonymSection">
			<h3>${label}</h3>
			<#list synonyms as synonym>	 
				<@printTaxon synonym true type />
			</#list>
		</div>
	</#if>
</#macro>

<#macro loadingSpinner text="Loading..."><@compress single_line=true>
	<div class="ui-corner-all loadingSpinner"> 
    	${text} &nbsp; &nbsp; <img src="${staticURL}/img/loading.gif" alt="Loading.." /> 
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

<#macro languageSelector selectedLangcode permissions="requireEditorPermissions" >
	<select class="languageSelector" onchange="languageChanged(this)" <@checkPermissions permissions/>>
		<option value="fi" <#if selectedLangcode == "fi">selected="selected"</#if>>FI</option>
		<option value="sv" <#if selectedLangcode == "sv">selected="selected"</#if>>SV</option>
		<option value="en" <#if selectedLangcode == "en">selected="selected"</#if>>EN</option>
	</select>
</#macro>

<#macro checkPermissions permissions="requireEditorPermissions"><#if permissions == "requireEditorPermissions"><#if noPermissions??> disabled="disabled" </#if><#elseif permissions=="requireAdminPermissions"><#if !user.isAdmin()> disabled="disabled" </#if></#if></#macro>

<#macro label field class="" locale="en">
	<#assign property = properties.getProperty(field)>
	<#assign cleanedName = cleanName(field)>
	<div style="height: 1px; font-size: 1px; display:block;">&nbsp;</div>
	<label for="${cleanedName}" class="${cleanedName}Label ${class}">${property.label.forLocale(locale)!field}</label>
</#macro>

<#macro labeledSelect field defaultValue="taxonValue" permissions="requireEditorPermissions" >
	<@label field />
	<@select field defaultValue permissions />
</#macro>

<#macro select field defaultValue="taxonValue" permissions="requireEditorPermissions">
	<#assign property = properties.getProperty(field)>
	<#if !property.hasRange()>NO RANGE!<#else>
		<#assign cleanedName = cleanName(field)>
		<#if defaultValue == "taxonValue">
			<#if taxon[cleanedName]??> <#assign value = taxon[cleanedName]> <#else> <#assign value = ""> </#if>
		<#else>
			<#assign value = defaultValue>
		</#if>
		<#if property.isBooleanProperty()>
			<select class="${cleanedName}" name="${field}" <@checkPermissions permissions /> > 
				<option value=""></option>
				<#list property.range.values as optionValue>
					<option value="${optionValue.qname}" <#if value?string == optionValue.qname>selected="selected"</#if>>${optionValue.label.forLocale("en")!optionValue.qname}</option>
				</#list>
			</select>
		<#else>
			<select class="${cleanedName}" name="${field}" <@checkPermissions permissions /> > 
				<option value=""></option>
				<#list property.range.values as optionValue>
					<option value="${optionValue.qname}" <#if same(value, optionValue.qname)>selected="selected"</#if>>${optionValue.label.forLocale("en")!optionValue.qname}</option>
				</#list>
			</select>
		</#if>
	</#if>
</#macro>

<#macro labeledInput field autocomplete="off" defaultValue="taxonValue" permissions="requireEditorPermissions">
	<@label field />
	<@input field autocomplete defaultValue permissions/>
</#macro>

<#macro labeledTextarea field defaultValue="taxonValue" permissions="requireEditorPermissions">
	<@label field />
	<@textarea field defaultValue/>
</#macro>

<#macro input field autocomplete="off" defaultValue="taxonValue" permissions="requireEditorPermissions">
	<#if autocomplete != "on" && autocomplete != "off">INVALID autocomplete value given!</#if>
	<#assign cleanedName = cleanName(field)>
	<#if defaultValue == "taxonValue">
		<#if taxon[cleanedName]??> <#assign value = taxon[cleanedName]> <#else> <#assign value = ""> </#if>
	<#else>
		<#assign value = defaultValue>
	</#if>
	<#if properties.hasProperty(field) && properties.getProperty(field).isDateProperty()>
		<input type="text" name="${field}" class="${cleanedName} datepicker" value="${value?html}" autocomplete="${autocomplete}" <@checkPermissions permissions/> />
	<#else>
		<input type="text" name="${field}" class="${cleanedName}" value="${value?html}" autocomplete="${autocomplete}" <@checkPermissions permissions/> />
	</#if>
</#macro>

<#macro textarea field defaultValue="taxonValue" class="" permissions="requireEditorPermissions">
	<#assign cleanedName = cleanName(field)>
	<#if defaultValue == "taxonValue">
		<#if taxon[cleanedName]??> <#assign value = taxon[cleanedName]> <#else> <#assign value = ""> </#if>
	<#else>
		<#assign value = defaultValue>
	</#if>
	<textarea name="${field}" class="${cleanedName} ${class}" <@checkPermissions permissions />>${value?html}</textarea>
</#macro>

<#function cleanName field>
	<#return nameCleaner.clean(field)>
</#function>

<#macro portletHeader portletTitle initiallyClosed="" additionalFormClass="">
<div class="portlet">
	<div class="portlet-header ${initiallyClosed}">${portletTitle}</div>
	<div class="portlet-content">
		<form class="taxonEditSection ${additionalFormClass}">
			<input type="hidden" name="taxonQname" class="taxonQname" value="${taxon.qname?html}" />
</#macro>

<#macro portletFooter>
		</form>
	</div>
</div>
</#macro>
