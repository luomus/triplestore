<!DOCTYPE html>

<!--[if lt IE 9 ]> <body class="oldie"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="${locale}"> <!--<![endif]-->

<head>
	<meta charset="utf-8" />

	<!-- Set the viewport width to device width for supporting devices (iOS, Android etc) -->
	<meta name="viewport" content="initial-scale=1.0" />

	<title>Triplestore Editor | ${text["title_"+page]} | LUOMUS</title>
	<link href="${staticURL}/favicon.ico?${staticContentTimestamp}" type="image/ico" rel="shortcut icon" />
	
	<script src="${staticURL}/jquery-1.11.3.min.js?${staticContentTimestamp}"></script>
	
	<link href="${staticURL}/jquery-ui/css/cupertino/jquery-ui-1.10.3.custom.min.css?${staticContentTimestamp}" rel="stylesheet" /> 
	<script src="${staticURL}/jquery-ui/js/jquery-ui-1.10.3.custom.min.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/jquery-placeholder/jquery.placeholder.js?${staticContentTimestamp}"></script>
	
	<script src="${staticURL}/chosen/chosen.jquery.min.js?${staticContentTimestamp}"></script>
	<link href="${staticURL}/chosen/chosen.min.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<link href="${staticURL}/luomus.css?${staticContentTimestamp}" rel="stylesheet" />
	<link href="${staticURL}/triplestore.css?${staticContentTimestamp}" rel="stylesheet" />
	
	<#include "javascript.ftl">
	
</head>

<body>

	<div id="masthead" role="banner">
		<div id="masthead-inner">
		
			<#if user??>
				<div id="userinfo">
					<ul>
             			<li><a href="${baseURL}/logout" class="button">Logout</a></li>
              			<#if user.admin><li style="color: green; font-weight: bold;">ADMIN</li></#if>
              			<li>Logged in as <span class="name">${user.fullname}</span> (${user.qname})</li>
          			</ul>
				</div>
			</#if>
			
			<div id="logo">&nbsp;</div>

			<div id="sitetitle">
				Triplestore
			</div>
			
			<#if inStagingMode>     <span class="devmode">TEST ENVIROMENT</span></#if>	
    		<#if inDevelopmentMode> <span class="devmode">DEV ENVIROMENT</span></#if>	
			
			<div id="navigation-wrap" role="navigation">
				<nav id="mainmenu" role="navigation">
					<ul class="nav-bar" role="menu">
						<li role="menuitem"><a href="${taxonomyEditorBaseURL}">Taxon Editor</a></li>
						<#if user?? && user.isAdmin()>
							<li role="menuitem"><a href="${baseURL}/editor">Triplestore editor</a></li>
							<li role="menuitem"><a href="${baseURL}/it">Information Systems</a></li>
							<li><a target="_blank" href="${lajiETLBaseURL}">Laji-ETL</a></li>
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
