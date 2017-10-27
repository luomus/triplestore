<!DOCTYPE html>

<#include "macro.ftl">

<!--[if lt IE 9 ]> <body class="oldie"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="${locale}"> <!--<![endif]-->

<head>
	<meta charset="utf-8" />

	<!-- Set the viewport width to device width for supporting devices (iOS, Android etc) -->
	<meta name="viewport" content="initial-scale=1.0" />

	<title>Taxon Editor | ${text["title_"+page]} | Lajitietokeskus</title>
	<link href="${staticURL}/favicon.ico?${staticContentTimestamp}" type="image/ico" rel="shortcut icon" />

	<script src="${staticURL}/jquery-1.11.3.min.js"></script>
	
	<link href="${staticURL}/jquery-ui/css/cupertino/jquery-ui-1.10.3.custom.min.css" rel="stylesheet" /> 
	<script src="${staticURL}/jquery-ui/js/jquery-ui-1.10.2.min.js"></script>
	
	<script src="${staticURL}/jquery.validate.min.js?${staticContentTimestamp}"></script>

	<script src="${staticURL}/jquery.cookie.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/jsPlumb/jquery.jsPlumb-1.5.4-min.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/jquery-placeholder/jquery.placeholder.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/chosen/chosen.jquery.min.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/chosen/chosen.min.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<script src="${staticURL}/collapse/collapse.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/collapse/collapse.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<script src="${staticURL}/jquery-switchButton/jquery.switchButton.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/jquery-switchButton/jquery.switchButton.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<#if page == "taxonDescriptions">
		<script src="${staticURL}/tinymce-4.7.1/tinymce.min.js"></script>
		<script src="${staticURL}/tinymce-4.7.1/jquery.tinymce.min.js"></script>
	</#if>
	
	<link href="${staticURL}/luomus.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/triplestore.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/taxonomy.css?${staticContentTimestamp}" rel="stylesheet" />
	<#if page == "taxonEditMain" || page == "taxonDescriptions">
		<link href="${staticURL}/taxonTrees.css?${staticContentTimestamp}" rel="stylesheet" />
	</#if>
	<#if page?starts_with("iucn")>
		<link href="${staticURL}/iucn.css?${staticContentTimestamp}" rel="stylesheet" />
		<#include "javascript-iucn.ftl">
	</#if>
	
	<#if page == "publications">
		<script src="${staticURL}/listjs/list.min.js"></script>
	</#if>
	<#include "javascript.ftl">
	<#if page == "taxonEditMain" || page == "taxonDescriptions">
		<#include "javascript-taxonTree.ftl">
		<#include "javascript-taxonEdit.ftl">
	</#if>

	<#if restartMessage??>
		<script>
			alert('${restartMessage}');
		</script>
	</#if>
</head>

<body>

	<div id="masthead" role="banner">
		<div id="masthead-inner">
			
			<#if user??>
				<div id="userinfo">
					<ul>
             			<li><a href="${baseURL}/logout" class="button">Logout</a></li>
             			<#if user.role == "DESCRIPTION_WRITER">
              				<li style="color: rgb(150, 225, 230); font-weight: bold;">[Description writer]</li>
              			</#if>
              			<#if user.role == "ADMIN">
              				<li style="color: green; font-weight: bold;">[Admin]</li>
              			</#if>
              			<li>Logged in as <span class="name">${user.fullname}</span></li>
          			</ul>
				</div>
			</#if>
			
			<a href="${baseURL}">
				<div id="logo">&nbsp;</div>
			</a>

			<a href="${baseURL}">
				<div id="sitetitle">
					Taxon Editor
				</div>
			</a>
			
			<#if inStagingMode>     <span class="devmode">TEST ENVIROMENT</span></#if>	
    		<#if inDevelopmentMode> <span class="devmode">DEV ENVIROMENT</span></#if>	
			
			<div id="navigation-wrap" role="navigation">
				<nav id="mainmenu" role="navigation">
					<ul class="nav-bar" role="menu">
						<#if user??>
							<#if user.role == "DESCRIPTION_WRITER">
								<li role="menuitem"><a href="${baseURL}/taxon-descriptions">Descriptions</a></li>
							<#else>
								<li role="menuitem"><a href="${baseURL}">Taxonomy tree</a></li>
								<li role="menuitem"><a href="${baseURL}/taxon-descriptions">Descriptions</a></li>
								<li role="menuitem"><a href="${baseURL}/iucn">IUCN</a></li>
								<li role="menuitem"><a href="${baseURL}/publications">Publications</a></li>
								<li role="menuitem"><a href="${baseURL}/informalGroups">Informal groups</a></li>
								<li role="menuitem"><a href="${baseURL}/checklists">Checklists</a></li>
								<li role="menuitem"><a href="${baseURL}/invasive">Invasive</a></li>
								<li role="menuitem"><a href="${baseURL}/help">Help</a></li>
							</#if>
						<#else>
							<li role="menuitem"><a href="${baseURL}">Taxonomy editor</a></li>
						</#if>
					</ul>
				</nav>
		    </div>
		</div>
	</div>
	
	<!-- Content section -->
	<div id="main-area" role="main">
		<div id="content-wrapper">
		
		<div id="content" class="page-content">
