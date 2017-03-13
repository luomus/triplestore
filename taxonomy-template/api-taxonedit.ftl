<div>
<#include "macro.ftl">

<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon /> | 
	<@printEditorExpert taxon />
	<a class="button" id="descriptionTextButton" href="${baseURL}/taxon-descriptions/${taxon.qname}">Description texts</a>
	<button id="imagesButton">Images</button>
</h5>

<#if noPermissions??>
	<div class="noPermissionsToAlterTaxon"><span class="ui-icon ui-icon-info" title="Note!"></span> You do not have permissions to alter this taxon.</div>
</#if>

<div class="hidden">
	<span id="taxonToEditQname">${taxon.qname?html}</span>
	<span id="taxonToEditScientificName">${taxon.scientificName!""}</span>
	<span id="taxonToEditCreatedAtTimestamp">${taxon.createdAtTimestamp}</span>
</div>

<div class="column">

	<@portletHeader "Basic taxonomic information" "" "scientificNameSection" />
		<#if taxon.checklist??> 
			<label>Checklist</label> ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
		<#else>
			<label>Checklist</label> Orphan taxa
		</#if>
		<div style="height: 1px; font-size: 1px; display:block;">&nbsp;</div>
		<label>ID</label> ${taxon.qname} 
		<@labeledSelect "MX.taxonRank" />

		<div class="clear"></div>

		<div id="scientificNameTools">
			<div id="originalNamesView">
				<label>Scientific name</label>
				<@printScientificNameAndAuthor taxon />
			</div>
			<div id="originalNamesInputs" class="hidden">
				<@labeledInput "MX.scientificName" />
				<@labeledInput "MX.scientificNameAuthorship" "on" />
			</div>
			<div id="alteredNamesInputs" class="hidden">
				<label>New scientific name</label>
				<input type="text" name="alteredScientificName" id="alteredScientificName" />
				<br />
				<label>New authors</label>
				<input type="text" name="alteredAuthor" id="alteredAuthor" />
			</div>
			<span id="scientificNameToolButtons">
				<label>&nbsp;</label>
				<button id="fixTypo">Fix a mistake</button> 
				OR 
				<button id="alterScientificName">Change name and create synonym</button>
			</span>
			<div class="clear"></div>
			<span id="scientificNameHelp"></span>
		</div>

		<div class="clear"></div>
		
		<label for="nameDecidedBy">Name decision by</label>  
		<select id="nameDecidedBy" name="MX.nameDecidedBy" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQname>
				<option value="${personQname}" <#if same(taxon.nameDecidedBy, personQname)>selected="selected"</#if> >${persons[personQname].fullname}</option>
			</#list>
		</select>
		<@labeledInput "MX.nameDecidedDate" "on" />
		<div style="height: 1px; font-size: 1px; display:block;">&nbsp;</div>
		<label>Hidden?</label>	
		<select name="MX.hiddenTaxon" <@checkPermissions/> >
			<option value=""></option>
			<option value="true" <#if taxon.hidden>selected="selected"</#if>>Yes</option>
			<option value="false">No</option>
		</select>
	<@portletFooter />
	
	<@portletHeader "Source of taxonomy" "" "multirowSection"/>
		<table class="publicationSelect">
			<#list taxon.originalPublications as existingPublicationQname>
			<tr>
				<td>
					<select name="MX.originalPublication" class="chosen" <@checkPermissions/> >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}" <#if same(existingPublicationQname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			</#list>
			<tr>
				<td>
					<select name="MX.originalPublication" class="chosen" <@checkPermissions/> data-placeholder="Select existing publication" >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}">${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
		</table>
	<@portletFooter />
	
	<@portletHeader "Notes" />
		<@labeledTextarea "MX.notes" />
		<@labeledTextarea "MX.privateNotes" />
	<@portletFooter />
	
</div>

<div class="column">
	
	<@portletHeader "Primary vernacular names" "" "primaryVernacularNameSection" />
		<table id="vernacularNameTable">
			<tr>
				<th>&nbsp;</th>
				<th>Name</th>
			</tr>
			<tr>
				<td> <label for="vernacularName___fi">FI</label> </td>
				<td> <@input "MX.vernacularName___fi" "off" taxon.vernacularName.forLocale("fi")!"" /> </td>
			</tr>
			<tr>
				<td> <label for="vernacularName___sv">SV</label> </td>
				<td> <@input "MX.vernacularName___sv" "off" taxon.vernacularName.forLocale("sv")!"" /> </td>
			</tr>
			<tr>
				<td> <label for="vernacularName___en">EN</label> </td>
				<td> <@input "MX.vernacularName___en" "off" taxon.vernacularName.forLocale("en")!"" /> </td>
			</tr>
		</table>
	<@portletFooter />	
	
	
	<@portletHeader "Occurrence in Finland" "" "multirowSection" />
		<@labeledSelect "MX.occurrenceInFinland" />
		
		<table>
			<tr>
				<th><label>Type of occurrence</label></th>
			</tr>
			<#list taxon.typesOfOccurrenceInFinland as type>
				<tr><td><@select "MX.typeOfOccurrenceInFinland" type /></td></tr>
			</#list>
			<tr><td><@select "MX.typeOfOccurrenceInFinland" type /></td></tr>
		</table>
		
		<@label "MX.typeOfOccurrenceInFinlandNotes" "longtext" />
		<@textarea "MX.typeOfOccurrenceInFinlandNotes" />
	<@portletFooter />				

	<@portletHeader "Source of occurrence" "" "multirowSection" />
		<table class="publicationSelect">
			<#list taxon.occurrenceInFinlandPublications as existingPublicationQname>
			<tr>
				<td>
					<select name="MX.occurrenceInFinlandPublication" class="chosen" <@checkPermissions/> >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}" <#if same(existingPublicationQname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			</#list>
			<tr>
				<td>
					<select name="MX.occurrenceInFinlandPublication" class="chosen" <@checkPermissions/> data-placeholder="Select existing publication" >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}">${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
		</table>		
	<@portletFooter />	
	
	<@portletHeader "Informal groups" "" "multirowSection" />
		<#assign headerPrinted = false>
		<#list taxon.informalTaxonGroups as groupQname>
			<#if !taxon.explicitlySetInformalTaxonGroups?seq_contains(groupQname)>
				<#if !headerPrinted>
					<label>Inherited groups</label>
					<ul class="inheritedGroups">
					<#assign headerPrinted = true>
				</#if>
				<li>${informalGroups[groupQname.toString()].name.forLocale("fi")!""} - ${informalGroups[groupQname.toString()].name.forLocale("en")!groupQnameString}</li>
			</#if>	
		</#list>
		<#if headerPrinted>
			</ul>
			<br/>
		</#if>
		
		<table>
		<#list taxon.explicitlySetInformalTaxonGroups as groupQname>
			<tr><td>
			<select name="MX.isPartOfInformalTaxonGroup" data-placeholder="Select group" class="chosen" <@checkPermissions/> >
				<option value=""></option>
				<#list informalGroups?keys as groupQnameString>
					<option value="${groupQnameString}" <#if same(groupQname.toString(), groupQnameString)>selected="selected"</#if> >${informalGroups[groupQnameString].name.forLocale("fi")!""} - ${informalGroups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
				</#list>
			</select>
			</td></tr>
		</#list>
		<tr><td>
		<select name="MX.isPartOfInformalTaxonGroup" data-placeholder="Add new group" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list informalGroups?keys as groupQnameString>
				<option value="${groupQnameString}">${informalGroups[groupQnameString].name.forLocale("fi")!""} - ${informalGroups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
			</#list>
		</select>
		</td></tr>
		</table>
		<p class="info">When adding a new informal group it is only required to add the 'lowest' group. 'Parent groups' are added automatically. (If you want to see them after saving, reload this view.)</p>
	<@portletFooter />				

</div>

<div class="column">

	<@portletHeader "Other vernacular names" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
				<th>Language</th>
			</tr>
			<#list ["fi", "sv", "en"] as localeSelector>
				<#list taxon.alternativeVernacularNames.forLocale(localeSelector) as alternativeVernaculaName>
					<tr>
						<td><@input "MX.alternativeVernacularName___${localeSelector}" "off" alternativeVernaculaName /></td>
						<td><@languageSelector localeSelector /></td>
					</tr>
				</#list>
			</#list>
			<tr>
				<td><@input "MX.alternativeVernacularName___fi" "off" "" /></td>
				<td><@languageSelector "fi" /></td>
			</tr>
		</table>
	<@portletFooter />	
	
	<@portletHeader "Obsolete vernacular names" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
				<th>Language</th>
			</tr>
			<#list ["fi", "sv", "en"] as localeSelector>
				<#list taxon.obsoleteVernacularNames.forLocale(localeSelector) as obsoleteVernaculaName>
					<tr>
						<td><@input "MX.obsoleteVernacularName___${localeSelector}" "off" obsoleteVernaculaName /></td>
						<td><@languageSelector localeSelector /></td>
					</tr>
				</#list>
			</#list>
			<tr>
				<td><@input "MX.obsoleteVernacularName___fi" "off" "" /></td>
				<td><@languageSelector "fi" /></td>
			</tr>
		</table>
	<@portletFooter />	

	<@portletHeader "Trade names" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
				<th>Language</th>
			</tr>
			<#list ["fi", "sv", "en"] as localeSelector>
				<#list taxon.tradeNames.forLocale(localeSelector) as tradeName>
					<tr>
						<td><@input "MX.tradeName___${localeSelector}" "off" tradeName /></td>
						<td><@languageSelector localeSelector /></td>
					</tr>
				</#list>
			</#list>
			<tr>
				<td><@input "MX.tradeName___fi" "off" "" /></td>
				<td><@languageSelector "fi" /></td>
			</tr>
		</table>
	<@portletFooter />
	
	<@portletHeader "Misapplied scientific names" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
			</tr>
			<#list taxon.misappliedNames as misappliedName>
			<tr>
				<td><@input "MX.misappliedName" "off" misappliedName /></td>
			</tr>
			</#list>
			<tr>
				<td><@input "MX.misappliedName" "off" "" /></td>
			</tr>
		</table>
		<@label "MX.misappliedNameNotes" "longtext" />
		<@textarea "MX.misappliedNameNotes" />
	<@portletFooter />	

	<#if user.isAdmin()>
	<@portletHeader "AKA names (Admin only)" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
			</tr>
			<#list taxon.alsoKnownAsNames as name>
			<tr>
				<td><@input "MX.alsoKnownAs" "off" name /></td>
			</tr>
			</#list>
			<tr>
				<td><@input "MX.alsoKnownAs" "off" "" /></td>
			</tr>
		</table>
	<@portletFooter />	
	</#if>

	<@portletHeader "Species codes" "initiallyClosed" />
		<@labeledInput "MX.euringCode" "off" />
		<@labeledInput "MX.euringNumber" "off" />
		<@labeledInput "MX.birdlifeCode" "off" />
	<@portletFooter />	
</div>

<div class="clear"></div>


<div class="column">

  <#if taxon.occurrences.hasOccurrences()>
	<@portletHeader "Biogeographical province occurrences" />
  <#else>
	<@portletHeader "Biogeographical province occurrences" "initiallyClosed" />
  </#if>
		<table id="biogeographicalProvinceTable">
			<tr>
				<th colspan="2">Area</th>
				<th>Status</th>
				<th>Notes</th>
				<th>Year</th>
			</tr>
			<#list biogeographicalProvinces?values as area>
				<tr>
					<td>${area.abbreviation}</td><td>&nbsp;${area.name.forLocale("fi")!""}</td>
					<td>
						<select class="status" name="MO.occurrence___${area.qname}___status" <@checkPermissions/>>
							<option value=""></option>
							<#list occurrenceProperties.getProperty("MO.status").range.values as prop>
								<#if taxon.occurrences.hasStatus(area.qname, prop.qname)>
									<option value="${prop.qname}" selected="selected">${prop.label.forLocale("en")!prop.qname}</option>
								<#else>
									<option value="${prop.qname}">${prop.label.forLocale("en")!prop.qname}</option>
								</#if>
							</#list>
						</select>
					</td>
					<td>
						<input class="notes" name="MO.occurrence___${area.qname}___notes" <@checkPermissions/> value="${((taxon.occurrences.getOccurrence(area.qname).notes)!"")?html}">
					</td>
					<td>
						<input class="year" name="MO.occurrence___${area.qname}___year" <@checkPermissions/> value="${((taxon.occurrences.getOccurrence(area.qname).year)!"")?html}">
					</td>
				</tr>
			</#list>
		</table>
	<@portletFooter />
	
</div>

<#macro emptyEditors>
	<p>
		<select name="MX.taxonEditor" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}">${persons[personQnameString].fullname}</option>
			</#list>
		</select>
	</p>
</#macro>

<#macro emptyExperts>
	<p>
		<select name="MX.taxonExpert" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}">${persons[personQnameString].fullname}</option>
			</#list>
		</select>
	</p>
</#macro>

<div class="column">
  <#if taxon.explicitlySetExperts?has_content || taxon.explicitlySetEditors?has_content>
	<@portletHeader "Editors and Experts" />
  <#else>
	<@portletHeader "Editors and Experts" "initiallyClosed" />
  </#if>
		<div class="info">
			Here you can explicitly set or change the editors and experts of this taxon. This will affect this taxon and all child taxons. However, if a child taxon 
			has a different explicitly set experts or editors, that will override what is set here.
		</div>
		
		<#if !taxon.explicitlySetExperts?has_content && !taxon.explicitlySetEditors?has_content>
			<p><label>Inherited editors and experts:</label></p>
		</#if>

		<#if !taxon.explicitlySetEditors?has_content && taxon.editors?has_content>
			<span class="editor">Editors: 
				<#list taxon.editors as editorQname>${(persons[editorQname.toString()].fullname)!editorQname}<#if editorQname_has_next>, </#if></#list>
			</span>
		<#elseif !taxon.explicitlySetEditors?has_content><span class="editor">Editors: [admin only]</span> </#if>

		<#if !taxon.explicitlySetExperts?has_content && taxon.experts?has_content>
			<span class="expert">Experts: 
				<#list taxon.experts as expertQname>${(persons[expertQname.toString()].fullname)!expertQname}<#if expertQname_has_next>, </#if></#list>
			</span>
		<#elseif !taxon.explicitlySetExperts?has_content><span class="expert">Experts:[none]</span></#if>
		
		<p><label>Editors</label></p>
		<#list taxon.explicitlySetEditors as editorQname>
			<p>
			<select name="MX.taxonEditor" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
				<option value=""></option>
				<#list persons?keys as personQnameString>
					<option value="${personQnameString}" <#if same(editorQname.toString(), personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname}</option>
				</#list>
			</select>
			</p>
		</#list>
		<@emptyEditors />
		<@emptyEditors />
		<@emptyEditors />
		
		<p><label>Experts</label></p>
		<#list taxon.explicitlySetExperts as expertQname>
			<p>
			<select name="MX.taxonExpert" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
				<option value=""></option>
				<#list persons?keys as personQnameString>
					<option value="${personQnameString}" <#if same(expertQname.toString(), personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname}</option>
				</#list>
			</select>
			</p>
		</#list>
		<@emptyExperts />
		<@emptyExperts />
		<@emptyExperts />
		
		<div class="info">
			Note that after saving the changes you must close and re-open the affected branches in the taxonomy tree to be able to see the changes.  
		</div>
		
	<@portletFooter />	
</div>

<div class="column">

	<@portletHeader "Administrative statuses (Admin only)" "initiallyClosed" "multirowSection" />
		<table>
		<#list taxon.administrativeStatuses as status>
			<tr><td><@select "MX.hasAdminStatus" status "requireAdminPermissions" /></td></tr>
		</#list>
		<tr><td><@select "MX.hasAdminStatus" status "requireAdminPermissions" /></td></tr>
		</table>
	<@portletFooter />	 
	
	<@portletHeader "Secure levels (Admin only)" "initiallyClosed" />
		<@labeledSelect "MX.secureLevel" "taxonValue" "requireAdminPermissions" />
		<@labeledSelect "MX.breedingSecureLevel" "taxonValue" "requireAdminPermissions" />
		<@labeledSelect "MX.winteringSecureLevel" "taxonValue" "requireAdminPermissions" />
		<@labeledSelect "MX.nestSiteSecureLevel" "taxonValue" "requireAdminPermissions" />
		<@labeledSelect "MX.naturaAreaSecureLevel" "taxonValue" "requireAdminPermissions" />
	<@portletFooter />
	
	<@portletHeader "Invasive species (Admin only)" "initiallyClosed" />
		<@labeledSelect "MX.invasiveSpeciesCategory" "taxonValue" "requireAdminPermissions" />
		<@labeledSelect "MX.invasiveSpeciesEstablishment" "taxonValue" "requireAdminPermissions" />
		<#list taxon.invasiveSpeciesMainGroups as mainGroup>
			<@labeledSelect "HBE.invasiveSpeciesMainGroup" mainGroup "requireAdminPermissions" />
		</#list>
		<@labeledSelect "HBE.invasiveSpeciesMainGroup" "" "requireAdminPermissions" />
	<@portletFooter />
	
	<#if user.isAdmin()>
		<@portletHeader "Misc (Admin only)" "initiallyClosed" />
			<@labeledInput "MX.customReportFormLink" "on" />
			<hr />
			<label>External links</label>
			<table>
				<#list taxon.externalLinks as link>
					<tr>
						<td><@input "MX.externalLinkURL___${link.locale}" "on" link.toString()!"" /></td>
						<td><@languageSelector link.locale /></td>
					</tr>
				</#list>
					<tr>
						<td><@input "MX.externalLinkURL___fi" "on" "" /></td>
						<td><@languageSelector "fi" /></td>
					</tr>
				</table>
		<@portletFooter />
	</#if>
</div>


<div class="taxonDeleteContainer">
<#if !taxon.hasCriticalData()>
		<button id="detachTaxon" class="ui-state-error">Detach taxon</button>
		<button id="deleteTaxon" class="ui-state-error">Delete taxon</button>
<#else>
	<p><span class="criticalData ui-icon ui-icon-key" title="Taxon has critical data"></span> This taxon can not be deleted or detached. It has critical data.</p>
	<p class="info">(Children, administrative properties, description texts, editors/experts, etc.)</p>
</#if>
</div>


<div class="clear"></div>


<script>
$(function() {

	$("textarea").attr("placeholder", "In english");
	
	$("#deleteTaxon").on('click', function() {
		var taxonToDeleteQname = $("#taxonToEditQname").text();
		var taxonToDeleteScientificName = $("#taxonToEditScientificName").text();
		var taxonToDeteteCreatedAt =  parseInt($("#taxonToEditCreatedAtTimestamp").text());
		var lastAllowedTaxonDeleteTimestamp = ${lastAllowedTaxonDeleteTimestamp};
		
		if (taxonToDeteteCreatedAt < lastAllowedTaxonDeleteTimestamp) {
			alert('This taxon can not be deleted anymore. You can only delete taxons for 5 hours after creation. Please, contact admins to get taxon deleted. You can detach the taxon from the checklist.');
			return;
		}
		
		if (!confirm('Are you sure you want to delete taxon '+taxonToDeleteScientificName +' ('+taxonToDeleteQname+') ?')) return;
		
		$.post("${baseURL}/api/deleteTaxon/"+taxonToDeleteQname, function(data) {
			$("#editTaxonContent").html('<p class="successMessage">Deleted!</p>');
			var taxon = $("#"+taxonToDeleteQname.replace("MX.", "MX"));
			var closeButton = taxon.find('.treePlusMinusSign').first();
			collapseTaxon(closeButton);
			taxon.hide('slow', function() {
				$(this).remove();
			});
  		});
  		
	});
	
	$("#detachTaxon").on('click', function() {
		var taxonToDetachQname = $("#taxonToEditQname").text();
		var taxonToDetachScientificName = $("#taxonToEditScientificName").text();
		
		if (!confirm('Are you sure you want to detach taxon '+taxonToDetachScientificName +' ('+taxonToDetachQname+') from this checklist and make it an orphan taxa?')) return;
		
		$.post("${baseURL}/api/detachTaxon/"+taxonToDetachQname, function(data) {
			$("#editTaxonContent").html('<p class="successMessage">Detached!</p>');
			$("#"+taxonToDetachQname.replace("MX.", "MX")).hide('slow', function() {
				$(this).remove();
			});
  		});
  		
	});
	
	$("#imagesButton").on('click', function() {
		var container = $('<div id="iframeContainer"><iframe src="${kotkaURL}/tools/taxon-images?taxonID=${taxon.qname}&amp;personToken=${user.personToken}"></iframe></div>');
		$("body").append(container);
		var windowHeight = $(window).height();
        var dialogHeight = windowHeight * 0.9;
		container.dialog({
			title: 'Add/modify taxon images',
			autoOpen: true,
      		height: dialogHeight,
      		width: "95%",
      		modal: true,
      		buttons: {
        		"Close": function() {
          			container.dialog("close");
        		}
			},
      		close: function() {
				container.remove();
      		}
    	});
	});
	
});
</script>
</div>
