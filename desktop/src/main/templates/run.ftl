<#-- template to create the runner script -->
<#if osName?upper_case?contains("WIN")>
@echo off
pushd %~dp0
set script_dir=%CD%
popd
cd %script_dir%
cd ..
  <#if mainModule != "">
<#-- modular application -->
bin\java --add-opens 'java.base/java.lang=ALL-UNNAMED' --add-opens 'java.base/java.lang=weld.core.impl' ${(modulePath!="")?then(" -p " + modulePath, "")}${(classPath!="")?then(" -cp " + classPath, "")} -m ${mainModule}/${mainClass}

  <#else>
<#-- classpath application -->
bin\java --add-opens 'java.base/java.lang=ALL-UNNAMED' --add-opens 'java.base/java.lang=weld.core.impl' -cp ${classPath} ${mainClass}
  </#if>
<#else>
#!/bin/sh
  <#if osName?upper_case?contains("MAC")>
abs_path() {
  echo "$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
}
cd "$(dirname "$(dirname "$(abs_path "$0")")")"
  <#else>
cd "$(dirname "$(dirname "$(readlink -f "$0")")")"
  </#if>
  <#if mainModule != "">
<#-- modular application -->
bin/java --add-opens 'java.base/java.lang=ALL-UNNAMED' --add-opens 'java.base/java.lang=weld.core.impl' ${(modulePath!="")?then(" -p " + modulePath, "")}${(classPath!="")?then(" -cp " + classPath, "")} -m ${mainModule}/${mainClass} --add-opens 'java.base/java.lang=ALL-UNNAMED' --add-opens 'java.base/java.lang=weld.core.impl'
  <#else>
<#-- classpath application -->
bin/java --add-opens 'java.base/java.lang=ALL-UNNAMED' --add-opens 'java.base/java.lang=weld.core.impl' -cp ${classPath} ${mainClass}
  </#if>
</#if>