<#macro findApp key>
  <#assign application = "Generic">
  <#if key?is_sequence>
    <#list key as item>
      <#if item.key?? >
	    <#if item.key == "it.service" >
			<#assign application = item.value >
		    <#break>
		</#if>
	  </#if>
    </#list>
  </#if>
</#macro>
<#macro findHost key>
  <#if key?is_sequence>
    <#list key as item>
      <#if item.entityId.id?? >
	    <#if item.entityId.id?starts_with("HOST-") >
			<#assign host = item.name >
		    <#break>
		</#if>
	  </#if>
    </#list>
  </#if>
</#macro>
<#macro flatten hash prefix="">
  <#if hash?is_hash>
    <#list hash?keys as key>
	    <#assign value = hash[key]>
	    <#if value?is_hash>
	      <@flatten hash=value prefix=prefix + key + "." />
	    <#else>
		    <#if value?is_sequence>
		      "ARRAY"
		    <#else>
${prefix}${key}: ${value?string}
		    </#if>
	    </#if>
    </#list>
  </#if>
  <#if hash?is_sequence>
    <#list hash as item>
  		<#if item?is_hash>
	      <@flatten hash=item prefix=prefix + item?counter + "." />
	    <#else>
${item}
		</#if>
    </#list>
  </#if>
</#macro>
{"events":[
{"event": {
<#if startTime gt 0>
  "start": "${(startTime?number_to_datetime?string("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))}",
</#if>
<#if endTime gt 0>
  "end": "${(endTime?c)?number_to_datetime?string("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")}",
</#if>
<#if status??>
  <#switch status>
    <#case "RESOLVED"> "status": "INPROGRESS", <#break>
    <#default>  "status": "OPEN",
  </#switch>
<#else>
  "status": "NEW"
</#if>
<#if severityLevel??>
  <#switch severityLevel>
    <#case "CUSTOM_ALERT"          >  "severity": "SEVERE",
    <#break>
    <#case "AVAILABILITY"          >  "severity": "ERROR",  
    <#break>
    <#case "ERROR"                 >  "severity": "ERROR",  
    <#break>
    <#case "INFO"                  >  "severity": "INFO",   
    <#break>
    <#case "MONITORING_UNAVAILABLE">  "severity": "WARN",   
    <#break>
    <#case "PERFORMANCE"           >  "severity": "WARN",   
    <#break>
    <#case "RESOURCE_CONTENTION"   >  "severity": "WARB",   
    <#break>
    <#default                      >  "severity": "ERROR",  
    <#break>
  </#switch>
<#else>
  "severity": "ERROR"
</#if>
<#if entityTags?is_sequence>
	<@findApp key=entityTags />
  "application": "${application}",
</#if>
<#if host??>
<#else>
	<#if affectedEntities?is_sequence>
		<@findHost key=affectedEntities />
	</#if>
</#if>
<#if host??>
<#else>
	<#if impactedEntities?is_sequence>
		<@findHost key=impactedEntities />
	</#if>
</#if>
<#if host??>
<#else>
   <#assign host = "localhost" >
</#if>
  "host": "${host}",
  "message": "${title}",
  "id": "${displayId}"
}}
]}
