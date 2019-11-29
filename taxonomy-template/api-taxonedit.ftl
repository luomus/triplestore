<div>
<#include "macro.ftl">

<h5 id="taxonEditHeader">
	<@printScientificNameAndAuthor taxon /> &nbsp; 
	<@printEditorExpert taxon />
	<#if fullView>
		<a class="button" id="descriptionTextButton" href="${baseURL}/taxon-descriptions/${taxon.qname}">Description texts</a>
		<button id="imagesButton">Images</button>
	</#if>
</h5>

<#if noPermissions??>
	<div class="noPermissionsToAlterTaxon"><span class="ui-icon ui-icon-info" title="Note!"></span> You do not have permissions to alter this taxon.</div>
</#if>

<div class="hidden">
	<span id="taxonToEditQname">${taxon.qname?html}</span>
	<span id="taxonToEditScientificName">${taxon.scientificName!""}</span>
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
				<input type="text" name="alteredScientificName" id="alteredScientificName" autocomplete="off" />
				<br />
				<label>New authors</label>
				<input type="text" name="alteredAuthor" id="alteredAuthor" autocomplete="off" />
			</div>
			<#if noPermissions??><#else>
			<span id="scientificNameToolButtons">
				<label>&nbsp;</label>
				<button id="fixTypo">Fix name/author</button> 
				<#if !taxon.synonym> 
					OR
					<button id="alterScientificName">Change name and create synonym</button>
				</#if>
			</span>
			</#if>
			<div class="clear"></div>
			<span id="scientificNameHelp"></span>
		</div>

		<div class="clear"></div>
		
		<label for="nameDecidedBy">Name decision by</label>  
		<input type="hidden" name="MX.nameDecidedBy" value="" />
		<select id="nameDecidedBy" name="MX.nameDecidedBy" data-placeholder="Select person" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQname>
				<option value="${personQname}" <#if same(taxon.nameDecidedBy, personQname)>selected="selected"</#if> >${persons[personQname].fullname} ${personQname}</option>
			</#list>
		</select>
		<@labeledInput "MX.nameDecidedDate" "on" />
	<@portletFooter />
	
	<@portletHeader "Source of taxonomy" />
		<#if taxon.setToStopOriginalPublicationInheritance || (!taxon.explicitlySetOriginalPublications?has_content && taxon.originalPublications?has_content)>
			<label>Inherited publications</label>
			<ul class="inheritedValues">
				<#list taxon.originalPublications as publicationQname>	
					<li>${publications[publicationQname].citation}</li>
				</#list>
			</ul>
			<p class="info">Publications are inherited from parents. If you define one or more publications, the parent's publications are no longer inherited. To stop inheriting without defining a new publication, use the provided tool. You must reload this view to see changes.</p>
			<label>Stop inheritance</label>
			<select name="MX.stopOriginalPublicationInheritance" <@checkPermissions/> >
				<option value=""></option>
				<option value="true" <#if taxon.setToStopOriginalPublicationInheritance>selected="selected"</#if>>Yes</option>
				<option value="false">No</option>
			</select>
			<br /><br />
		</#if>
		<input type="hidden" name="MX.originalPublication" value="" />
		<select id="originalPublicationSelector" name="MX.originalPublication" class="chosen" data-placeholder="Select publication" multiple="multiple" <@checkPermissions/> >
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasExplicitlySetOriginalPublication(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
		<br/>
		<p><label class="">Add a new publication</label></p>
		<textarea <@checkPermissions/> class="newPublicationInput" name="newPublicationCitation" id="newPublicationCitation" placeholder="For example 'Hellén, W. 1940: Enumeratio Insectorum Fenniae II Hymenoptera 2. Terebrantia. - Helsinki, 32 s.' "></textarea>
	<@portletFooter />
	
	<@portletHeader "Type specimen / Nomenclatural reference" />
		<@labeledInput "MX.typeSpecimenURI" "off" />
		
		<@label "MX.originalDescription" "longtext" />
		<input type="hidden" name="MX.originalDescription" value="" />
		<select id="originalDescriptionSelector" name="MX.originalDescription" class="chosen" data-placeholder="Select publication" <@checkPermissions/> >
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasOriginalDescription(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
	<@portletFooter />
	
	<@portletHeader "Notes" />
		<@labeledTextarea "MX.notes" />
		<@labeledTextarea "MX.privateNotes" />
	<@portletFooter />
	
</div>

<#if fullView>
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
	
	
	<@portletHeader "Occurrence in Finland" "" "multirowSection finnishnessSection" />
	    <@label "MX.finnish" />
	    <select class="finnish" name="MX.finnish">
	    	<option value="">&nbsp;</option>
	    	<option value="true" <#if taxon.finnish>selected="selected"</#if>>Yes</option>
	    </select>
	    
		<@labeledSelect "MX.occurrenceInFinland" />
		
		<table>
			<thead>
				<tr>
					<th><label>Type of occurrence</label></th>
				</tr>
			</thead>
			<tbody>
				<#list taxon.typesOfOccurrenceInFinland as type>
					<tr><td><@select "MX.typeOfOccurrenceInFinland" type /></td></tr>
				</#list>
				<tr><td><@select "MX.typeOfOccurrenceInFinland" "" /></td></tr>
			</tbody>
		</table>
		
		<@label "MX.occurrenceInFinlandSpecimenURI" "longtext" />
		<@input "MX.occurrenceInFinlandSpecimenURI" "off" />
		
		<@label "MX.typeOfOccurrenceInFinlandNotes" "longtext" />
		<@textarea "MX.typeOfOccurrenceInFinlandNotes" />
	<@portletFooter />				

	<@portletHeader "Source of occurrence" />
		<#if taxon.setToStopOccurrenceInFinlandPublicationInheritance || (!taxon.explicitlySetOccurrenceInFinlandPublications?has_content && taxon.occurrenceInFinlandPublications?has_content)>
			<label>Inherited publications</label>
			<ul class="inheritedValues">
				<#list taxon.occurrenceInFinlandPublications as publicationQname>	
					<li>${publications[publicationQname].citation}</li>
				</#list>
			</ul>
			<p class="info">Publications are inherited from parents. If you define one or more publications, the parent's publications are no longer inherited. To stop inheriting without defining a new publication, use the provided tool. You must reload this view to see changes.</p>
			<label>Stop inheritance</label>
			<select name="MX.stopOccurrenceInFinlandPublicationInheritance" <@checkPermissions/> >
				<option value=""></option>
				<option value="true" <#if taxon.setToStopOccurrenceInFinlandPublicationInheritance>selected="selected"</#if>>Yes</option>
				<option value="false">No</option>
			</select>
			<br /><br />
		</#if>
		<input type="hidden" name="MX.occurrenceInFinlandPublication" value="" />
		<select id="occurrenceInFinlandPublicationSelector" name="MX.occurrenceInFinlandPublication" multiple="multiple" data-placeholder="Select publication" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasExplicitlySetOccurrenceInFinlandPublication(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
		<br/>
		<p><label class="">Add a new publication</label></p>
		<textarea <@checkPermissions/> class="newPublicationInput" name="newOccurrenceInFinlandPublicationCitation" id="newOccurrenceInFinlandPublicationCitation" placeholder="For example 'Juutinen, R. & Ulvinen, T. 2015: Suomen sammalien levinneisyys eliömaakunnissa. – Suomen ympäristökeskus. 27.3.2015' "></textarea>
	<@portletFooter />	
	
</div>
</#if>

<#if fullView>
<div class="column">
	
	<#if taxon.alternativeVernacularNames.empty>
		<@portletHeader "Other vernacular names" "initiallyClosed" "multirowSection" />
	<#else>
		<@portletHeader "Other vernacular names" "" "multirowSection" />
	</#if>
		<table>
			<thead>
				<tr>
					<th>Name</th> 
					<th>Language</th>
				</tr>
			</thead>
			<tbody>
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
			</tbody>
		</table>
	<@portletFooter />	
	
	<#if taxon.obsoleteVernacularNames.empty>
		<@portletHeader "Obsolete vernacular names" "initiallyClosed" "multirowSection" />
	<#else>
		<@portletHeader "Obsolete vernacular names" "" "multirowSection" />
	</#if>
		<table>
			<thead>
				<tr>
					<th>Name</th> 
					<th>Language</th>
				</tr>
			</thead>
			<tbody>
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
			</tbody>
		</table>
	<@portletFooter />	

	<#if taxon.tradeNames.empty>
		<@portletHeader "Trade names" "initiallyClosed" "multirowSection" />
	<#else>
		<@portletHeader "Trade names" "" "multirowSection" />
	</#if>
		<table>
			<thead>
				<tr>
					<th>Trade name</th> 
					<th>Language</th>
				</tr>
			</thead>
			<tbody>
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
			</tbody>
		</table>
	<@portletFooter />
	
	<@portletHeader "AKA names (observation names)" "initiallyClosed" "multirowSection" />
		<p class="info">AKA names are names that for some reason have been used to report observations but are not valid names (not correct vernacular names, species codes, etc).
		   For example 'jokin sieni' or 'talitintti'. Adding the name to one taxon as an AKA name allows those observations to be linked with that taxon. 
		   Unlike other names, AKA names are not used for taxon search.</p>
		<table>
			<thead>
				<tr>
					<th>AKA name</th> 
				</tr>
			</thead>
			<tbody>
				<#list taxon.alsoKnownAsNames as name>
				<tr>
					<td><@input "MX.alsoKnownAs" "off" name /></td>
				</tr>
				</#list>
				<tr>
					<td><@input "MX.alsoKnownAs" "off" "" /></td>
				</tr>
			</tbody>
		</table>
	<@portletFooter />	

	<@portletHeader "Observation linking override" "initiallyClosed" "multirowSection" />
		<p class="info">This feature allows the taxon expert to specify how observations reported using a certain name are linked to the taxonomy.
		   The name for example can be a valid species name in taxonomy and has had many other alternative meanings → The editor wants to define that the observations
		   are linked with a higher taxon group instead of the species.
		   Overriding names are not used for taxon search.</p>
		<table>
			<thead>
				<tr>
					<th>Overriding name</th> 
				</tr>
			</thead>
			<tbody>
				<#list taxon.overridingTargetNames as name>
				<tr>
					<td><@input "MX.overridingTargetName" "off" name /></td>
				</tr>
				</#list>
				<tr>
					<td><@input "MX.overridingTargetName" "off" "" /></td>
				</tr>
			</tbody>
		</table>
	<@portletFooter />
	
	<@portletHeader "Species codes" "initiallyClosed" />
		<@labeledInput "MX.euringCode" "off" />
		<@labeledInput "MX.euringNumber" "off" />
		<@labeledInput "MX.birdlifeCode" "off" />
	<@portletFooter />	
	
	  	<@portletHeader "Informal groups" />
		<#assign headerPrinted = false>
		<#list taxon.informalTaxonGroups as groupQname>
			<#if !taxon.explicitlySetInformalTaxonGroups?seq_contains(groupQname)>
				<#if !headerPrinted>
					<label>Inherited groups</label>
					<ul class="inheritedValues">
					<#assign headerPrinted = true>
				</#if>
				<li>${informalGroups[groupQname.toString()].name.forLocale("fi")!""} - ${informalGroups[groupQname.toString()].name.forLocale("en")!groupQnameString}</li>
			</#if>	
		</#list>
		<#if headerPrinted>
			</ul>
			<br/>
		</#if>
		
		<input type="hidden" name="MX.isPartOfInformalTaxonGroup" value="" />
		<select name="MX.isPartOfInformalTaxonGroup" data-placeholder="Select group" multiple="multiple" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list informalGroups?keys as groupQnameString>
				<option value="${groupQnameString}" <#if taxon.hasExplicitlySetInformalTaxonGroup(groupQnameString)>selected="selected"</#if> >${informalGroups[groupQnameString].name.forLocale("fi")!""} - ${informalGroups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
			</#list>
		</select>
		
		<p class="info">When adding a new informal group it is only required to add the 'lowest' group. 'Parent groups' are added automatically. (If you want to see them after saving, reload this view by clicking this taxon in the tree again.)</p>
    <@portletFooter />
    
</div>

<div class="clear"></div>
</#if>

<#if fullView>
<div class="column">

  <#if taxon.occurrences.hasOccurrences()>
	<@portletHeader "Biogeographical province occurrences" "" "biogeographicalProvinceOccurrences"/>
  <#else>
	<@portletHeader "Biogeographical province occurrences" "initiallyClosed" "biogeographicalProvinceOccurrences"/>
  </#if>
		<table id="biogeographicalProvinceTable">
			<tr>
				<th colspan="2">Area</th>
				<th>Status</th>
				<th>Notes</th>
				<th>Year</th>
				<th>Specimen URI</th>
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
						<input class="notes" name="MO.occurrence___${area.qname}___notes" <@checkPermissions/> value="${((taxon.occurrences.getOccurrence(area.qname).notes)!"")?html}" >
					</td>
					<td>
						<input class="year" name="MO.occurrence___${area.qname}___year" <@checkPermissions/> value="${((taxon.occurrences.getOccurrence(area.qname).year)!"")?html}" autocomplete="off" >
					</td>
					<td>
						<input class="specimenURI" name="MO.occurrence___${area.qname}___specimenURI" <@checkPermissions/> value="${((taxon.occurrences.getOccurrence(area.qname).specimenURI)!"")?html}" autocomplete="off" >
					</td>
				</tr>
			</#list>
		</table>
	<@portletFooter />
	
</div>

<div class="column">
  <#if taxon.primaryHabitat??>
	<@portletHeader "Habitats" "" "habitats multirowSection"/>
  <#else>
	<@portletHeader "Habitats" "initiallyClosed" "habitats multirowSection"/>
  </#if>
  		<label>Primary habitat</label> <br />
		<@habitatEdit "MKV.primaryHabitat___0" taxon.primaryHabitat /> <br />
  		<label>Secondary habitats</label>
  		<table>
  			<tbody>
				<#list taxon.secondaryHabitats as secondaryHabitat>
					<tr><td><@habitatEdit "MKV.secondaryHabitat___"+secondaryHabitat_index secondaryHabitat /></td></tr>
  				</#list>
  				<tr><td><@habitatEdit "MKV.secondaryHabitat___"+taxon.secondaryHabitats?size/></td></tr>
  			</tbody>
  		</table>
  <@portletFooter />
</div>

<#macro habitatEdit name habitat="">
	<select name="${name}___MKV.habitat" <@checkPermissions/> class="chosen" data-placeholder="Select habitat">
		<option value=""></option>
		<#list habitatProperties.getProperty("MKV.habitat").range.values as prop>
			<#if habitat != "" && habitat.habitat == prop.qname>
				<option value="${prop.qname}" selected="selected">${prop.label.forLocale("fi")!prop.qname}</option>
			<#else>
				<option value="${prop.qname}">${prop.label.forLocale("fi")!prop.qname}</option>
			</#if>
		</#list>
	</select>
	<select name="${name}___MKV.habitatSpecificType" <@checkPermissions/> multiple="multiple" class="chosen" data-placeholder="Select specifiers">
		<option value=""></option>
		<#list habitatProperties.getProperty("MKV.habitatSpecificType").range.values as prop>
			<#if habitat != "" && habitat.habitatSpecificTypes?seq_contains(prop.qname)>
				<option value="${prop.qname}" selected="selected">${prop.label.forLocale("fi")!prop.qname}</option>
			<#else>
				<option value="${prop.qname}">${prop.label.forLocale("fi")!prop.qname}</option>
			</#if>
		</#list>
	</select>
</#macro>

<div class="clear"></div>
</#if>

<#if fullView>
<div class="column">

  <#if taxon.explicitlySetExperts?has_content || taxon.explicitlySetEditors?has_content>
	<@portletHeader "Editors and Experts" />
  <#else>
	<@portletHeader "Editors and Experts" "initiallyClosed" />
  </#if>
		<div class="info">
			Here you can explicitly set or change the editors and experts of this taxon. This will affect this taxon and all child taxa. However, if a child taxon 
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
		<input type="hidden" name="MX.taxonEditor" value="" />
		<select name="MX.taxonEditor" data-placeholder="Select person" multiple="multiple" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}" <#if taxon.hasExplicitlySetEditor(personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname} ${personQnameString}</option>
			</#list>
		</select>

		<p><label>Experts</label></p>
		<input type="hidden" name="MX.taxonExpert" value="" />
		<select name="MX.taxonExpert" data-placeholder="Select person" multiple="multiple" class="chosen" <@checkPermissions/> >
			<option value=""></option>
			<#list persons?keys as personQnameString>
				<option value="${personQnameString}" <#if taxon.hasExplicitlySetExpert(personQnameString)>selected="selected"</#if> >${persons[personQnameString].fullname} ${personQnameString}</option>
			</#list>
		</select>
		
		<div class="info">
			Note that after saving the changes you must close and re-open the affected branches in the taxonomy tree to be able to see the changes.  
		</div>
		
	<@portletFooter />	
	
</div>				
<div class="column">

	<@portletHeader "Red List Finland" />
		<table class="redListTable">
		<#list evaluationYears as year>
			<tr>
				<th>${year}</th>
				<td>
					<#if taxon.getRedListStatusForYear(year)??>
						${redListStatusProperty.range.getValueFor(taxon.getRedListStatusForYear(year)).label.forLocale("fi")}
					<#else>
						-
					</#if>
				</td>
			</tr>
		</#list>
		</table>
		<p class="info">Red list statuses are modified using the IUCN editor.</p>
	<@portletFooter />	
	
	<@portletHeader "Administrative statuses (Admin only)" "initiallyClosed" "multirowSection" />
		<table>
		<tbody>
			<#list taxon.administrativeStatuses as status>
				<tr><td><@select "MX.hasAdminStatus" status "requireAdminPermissions" /></td></tr>
			</#list>
			<tr><td><@select "MX.hasAdminStatus" status "requireAdminPermissions" /></td></tr>
		</tbody>
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
		<@labeledSelect "MX.invasiveSpeciesEarlyWarning" "taxonValue" "requireAdminPermissions" />
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
</#if>
<div class="column">
	<@portletHeader "Frequency scoring points" />
		<@labeledInput "MX.frequencyScoringPoints" />
	<@portletFooter />
	
	<@portletHeader "Identifiers" "" "multirowSection" />
		<label>Taxonid.org</label>
		<#list taxon.taxonConceptIds as id>
			<a target="_blank" href="${id.toURI()}">${id}</a>
		</#list>
		<#if !taxon.taxonConceptIds?has_content>
			none
		</#if>
		<hr />
		<table>
			<thead>
				<tr>
					<th>Identifier</th> 
				</tr>
			</thead>
			<tbody>
				<#list taxon.additionalIds as id>
					<tr>
						<td><@input "MX.additionalID" "off" id /></td>
					</tr>
				</#list>
				<tr>
					<td><@input "MX.additionalID" "off" "" /></td>
				</tr>
			</tbody>
		</table>
	<@portletFooter />
</div>
<div class="clear"></div>


<script>
$(function() {

	$("textarea").not('.newPublicationInput').attr("placeholder", "In english");
	
	<@taxonImageButton />
	
});
</script>
</div>
