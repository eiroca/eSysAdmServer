<#if state??><#switch state>
<#case "NEW"       >OPEN Dynatrace_${tags.application!"application"} ${id} module=${tags.module!"module"} host=${tags.host!"host"} message=${message!"message"}<#break>
<#case "INPROGRESS">PROGRESS Dynatrace_${tags.application!"application"} ${id} module=${tags.module!"module"} host=${tags.host!"host"} message=${message!"message"}<#break>
<#case "CLOSED"    >CLOSE Dynatrace_${tags.application!"application"} ${id} module=${tags.module!"module"} host=${tags.host!"host"} message=${message!"message"}<#break>
</#switch></#if>