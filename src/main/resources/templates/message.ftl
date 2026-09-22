<#include "header.ftl">

<div class="container p-4 min-vh-100">
  <p><strong>${(i18n["message.details.author"])!"Author:"}</strong> ${author!""}</p>
  <p><strong>${(i18n["message.details.raw_content"])!"Raw Content:"}</strong> ${rawContent!""}</p>
  <p><strong>${(i18n["message.details.content"])!"Content:"}</strong> ${content!""}</p>
  <p><strong>${(i18n["message.details.date"])!"Date:"}</strong> ${timestamp!""}</p>
</div>

<#include "footer.ftl">
