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
		<label>ID</label> ${taxon.qname} 
		<@labeledSelect "MX.taxonRank" />
		<@labeledInput "MX.scientificName" />
		<@labeledInput "MX.scientificNameAuthorship" "on" />
		<br />
		<label for="nameDecidedBy">Name decision by</label>  
		<select id="nameDecidedBy" name="MX.nameDecidedBy" data-placeholder="Select person" class="chosen" <@permissions/> >
			<option value=""></option>
			<#list persons?keys as personQname>
				<option value="${personQname}" <#if same(taxon.nameDecidedBy, personQname)>selected="selected"</#if> >${persons[personQname].fullname}</option>
			</#list>
		</select>
		<@labeledInput "MX.nameDecidedDate" "on" />
		<@labeledTextarea "MX.notes" />
		<@labeledTextarea "MX.privateNotes" />
	<@portletFooter />
	
	<@portletHeader "Publications" "" "reloadAfterSaveSection" />
		<table class="publicationSelect">
			<tr>
				<th>Select publications</th> 
			</tr>
			<#list taxon.getOriginalPublicationsSortedByPublication(publications) as publication>
			<tr>
				<td>
					<select name="MX.originalPublication" class="chosen" <@permissions/> >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}" <#if same(publication.qname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			</#list>
			<tr>
				<td>
					<select name="MX.originalPublication" class="chosen" <@permissions/> data-placeholder="Select existing publication" >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}">${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			<tr>
				<th>Or create new publication</th> 
			</tr>
			<tr>
				<td><input type="text" name="newPublicationCitation" id="createNewPublicationInput" placeholder="Type citaction, for example 'Silfverberg, H. 2007. Changes in the list of Finnish insects 2001-2005. - Entomol. Fenn. 18:82-101'"/></td>
			</tr>
		</table>
		<div></div>
	<@portletFooter />
		
</div>

<div class="column">
	
	<@portletHeader "Primary vernacular names" />
		<table id="vernacularNameTable">
			<tr>
				<th>&nbsp;</th>
				<th>Name</th>
			</tr>
			<tr>
				<td> <label for="vernacularName___fi">FI</label> </td>
				<td> <@input "MX.vernacularName___fi" "off" taxon.getVernacularName("fi") /> </td>
			</tr>
			<tr>
				<td> <label for="vernacularName___sv">SV</label> </td>
				<td> <@input "MX.vernacularName___sv" "off" taxon.getVernacularName("sv") /> </td>
			</tr>
			<tr>
				<td> <label for="vernacularName___en">EN</label> </td>
				<td> <@input "MX.vernacularName___en" "off" taxon.getVernacularName("en") /> </td>
			</tr>
		</table>
	<@portletFooter />	
	
	
	<@portletHeader "Occurrence in Finland" "" "reloadAfterSaveSection" />
		<@labeledSelect "MX.occurrenceInFinland" />
		
		<#list taxon.typesOfOccurrenceInFinland as type>
			<@labeledSelect "MX.typeOfOccurrenceInFinland" type />
		</#list>
		<@labeledSelect "MX.typeOfOccurrenceInFinland" "" />
		<br /><br />
		<@label "MX.occurrenceInFinlandPublication" />
		<table class="publicationSelect">
			<tr>
				<th>Select publication</th> 
			</tr>
			<#list taxon.getOccurrenceInFinlandPublicationsSortedByPublication(publications) as publication>
			<tr>
				<td>
					<select name="MX.occurrenceInFinlandPublication" class="chosen" <@permissions/> >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}" <#if same(publication.qname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			</#list>
			<tr>
				<td>
					<select name="MX.occurrenceInFinlandPublication" class="chosen" <@permissions/> data-placeholder="Select existing publication" >
						<option value=""></option>
						<#list publications?keys as publicationQname>
							<option value="${publicationQname}">${publications[publicationQname].citation}</option>
						</#list>
					</select>
				</td>
			</tr>
			<tr>
				<th>Or create new publication</th> 
			</tr>
			<tr>
				<td><input type="text" name="newOccurrenceInFinlandPublicationCitation" id="createNewOccurrenceInFinlandPublicationInput" placeholder="Type citaction, for example 'Hudd, R. & Leskelä, A. 1998. Acidification-induced species shifts in coastal fisheries off the River Kyrönjoki, Finland: A case study. Ambio 27: 535–538.'"/></td>
			</tr>
		</table>
		<div></div>
		
	<@portletFooter />				

	<#if taxon.isSpecies()>
	<@portletHeader "Finnish Red list statuses (species only)" />
		<@labeledSelect "MX.redListStatus2010Finland" />
		<@labeledSelect "MX.redListDraftStatusFinland" />
	<@portletFooter />
	</#if>
	
</div>

<div class="column">

	<@portletHeader "Other vernacular names" "" "multirowSection" />
		<table>
			<tr>
				<th>Name</th> 
				<th>Language</th>
			</tr>
			<#list taxon.alternativeVernacularNamesWithLangCodes as alternativeVernaculaName>
			<tr>
				<td><@input "MX.alternativeVernacularName___${alternativeVernaculaName.langcode}" "off" alternativeVernaculaName.name /></td>
				<td><@languageSelector alternativeVernaculaName.langcode/></td>
			</tr>
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
			<#list taxon.obsoleteVernacularNamesWithLangCodes as obsoleteVernaculaName>
			<tr>
				<td><@input "MX.obsoleteVernacularName___${obsoleteVernaculaName.langcode}" "off" obsoleteVernaculaName.name /></td>
				<td><@languageSelector obsoleteVernaculaName.langcode/></td>
			</tr>
			</#list>
			<tr>
				<td><@input "MX.obsoleteVernacularName___fi" "off" "" /></td>
				<td><@languageSelector "fi" /></td>
			</tr>
		</table>
	<@portletFooter />	

	<#if user.isAdmin()>
		<@portletHeader "Admin only" "initiallyClosed" />
			<p class="info">Applicable for taxon rank order or a higher level</p>
			<@labeledSelect "MX.checklistStatus" />
			<@labeledSelect "MX.higherTaxaStatus" />
			<@labeledSelect "MX.finnishSpeciesTaggingStatus" />
			<@labeledInput "MX.customReportFormLink" "on" taxon.adminContent.getDefaultContextText("MX.customReportFormLink") />
		<@portletFooter />	
	</#if>
	
  <#if taxon.explicitlySetExperts?has_content || taxon.explicitlySetEditors?has_content>
	<@portletHeader "Editors and Experts" />
  <#else>
	<@portletHeader "Editors and Experts" "initiallyClosed" />
  </#if>
		<div class="info">
			Here you can explicitly set or change the editors and experts of this taxon. This will affect this taxon and all child taxons. However, if a child taxon 
			has a different explicitly set experts or editors, that will override what is set here.
		</div>
	
		<p><label>Editors</label></p>
		<#list taxon.explicitlySetEditors as editorQname>
			<p>
			<select name="MX.taxonEditor" data-placeholder="Select person" class="chosen" <@permissions/> >
				<option value=""></option>
				<#list persons?keys as personQnameString>
					<option value="${personQnameString}" <#if same(editorQname.toString(), personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname}</option>
				</#list>
			</select>
			</p>
		</#list>
		<p>
		<select name="MX.taxonEditor" data-placeholder="Select person" class="chosen" <@permissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}">${persons[personQnameString].fullname}</option>
			</#list>
		</select>
		</p>
		
		<p><label>Experts</label></p>
		<#list taxon.explicitlySetExperts as expertQname>
			<p>
			<select name="MX.taxonExpert" data-placeholder="Select person" class="chosen" <@permissions/> >
				<option value=""></option>
				<#list persons?keys as personQnameString>
					<option value="${personQnameString}" <#if same(expertQname.toString(), personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname}</option>
				</#list>
			</select>
			</p>
		</#list>
		<p>
		<select name="MX.taxonExpert" data-placeholder="Select person" class="chosen" <@permissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}">${persons[personQnameString].fullname}</option>
			</#list>
		</select>
		</p>
		
		<div class="info">
			Note that after saving the changes you must close and re-open the affected branches in the taxonomy tree to be able to see the changes.  
		</div>
	<@portletFooter />	
	
</div>

<div class="clear"></div>


<div class="column">

  <#if taxon.occurrences.hasOccurrences()>
	<@portletHeader "Biogeographical province occurrences" />
  <#else>
	<@portletHeader "Biogeographical province occurrences" "initiallyClosed" />
  </#if>
	
		<table>
			<tr>
				<th colspan="2">Area</th>
				<th>Status</th>
			</tr>
			<#list occurrenceProperties.getProperty("MO.area").range.values as areaProp>
				<#assign area = areas[areaProp.qname]>
				<tr>
					<td>${area.abbreviation}</td><td>&nbsp;${area.name.forLocale("fi")}</td>
					<td>
						<select name="MO.occurrence___${area.qname}">
							<option value=""></option>
							<#list occurrenceProperties.getProperty("MO.status").range.values as prop>
								<#if taxon.occurrences.hasStatus(area.qname, prop.qname)>
									<option value="${prop.qname}" selected="selected">${prop.label.forLocale("en")}</option>
								<#else>
									<option value="${prop.qname}">${prop.label.forLocale("en")!prop.qname}</option>
								</#if>
							</#list>
						</select>
					</td>
				</tr>
			</#list>
		</table>
	<@portletFooter />
	
</div>


<div class="column">

	<#if user.isAdmin()>
	<@portletHeader "Invasive species (Admin only)" "initiallyClosed" />
		<@labeledSelect "MX.invasiveSpeciesCategory" />
		<@labeledSelect "MX.invasiveSpeciesEstablishment" />
		
		<#list taxon.adminContent.getDefaultContextTexts("HBE.invasiveSpeciesMainGroup") as mainGroup>
			<@labeledSelect "HBE.invasiveSpeciesMainGroup" mainGroup />
		</#list>
		<@labeledSelect "HBE.invasiveSpeciesMainGroup" "" />
		
		<@labeledSelect "HBE.invasiveSpeciesGroup" taxon.adminContent.getDefaultContextText("HBE.invasiveSpeciesGroup") />
		<@labeledInput "HBE.invasiveSpeciesCustomReportFormLink" "on" taxon.adminContent.getDefaultContextText("HBE.invasiveSpeciesCustomReportFormLink") />
	<@portletFooter />
	</#if>
	
</div>


<div class="column">

	<@portletHeader "Ringing department" "initiallyClosed" />
		<@labeledInput "MX.euringCode" "off" taxon.birdContent.getDefaultContextText("MX.euringCode") />
		<@labeledInput "MX.euringNumber" "off" taxon.birdContent.getDefaultContextText("MX.euringNumber") />
		<@labeledInput "MX.birdlifeCode" "off" taxon.birdContent.getDefaultContextText("MX.birdlifeCode") />
	<@portletFooter />
	
</div>


<#if !taxon.hasChildren()>
<div class="taxonDeleteContainer">
	<button id="detachTaxon" class="ui-state-error">Detach taxon</button>
	<button id="deleteTaxon" class="ui-state-error">Delete taxon</button>
</div>
</#if>

<div class="clear"></div>


<script>
$(function() {
	$("#notes").attr("placeholder", "In english");
	$("#deleteTaxon").on('click', function() {
		var taxonToDeleteQname = $("#taxonToEditQname").text();
		var taxonToDeleteScientificName = $("#taxonToEditScientificName").text();
		var taxonToDeteteCreatedAt =  parseInt($("#taxonToEditCreatedAtTimestamp").text());
		var lastAllowedTaxonDeleteTimestamp = ${lastAllowedTaxonDeleteTimestamp};
		
		if (taxonToDeteteCreatedAt < lastAllowedTaxonDeleteTimestamp) {
			alert('This taxon can not be deleted anymore. You can only delete taxons for 5 hours after creation. Please, contact admins to get taxon deleted.');
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
		var container = $('<div id="iframeContainer"><iframe src="${kotkaURL}/tools/taxon-images?taxonID=${taxon.qname}"></iframe></div>');
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

