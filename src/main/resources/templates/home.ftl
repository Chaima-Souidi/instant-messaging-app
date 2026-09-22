<#include "header.ftl">
<link rel="stylesheet" href="/assets/css/chat.css">

<div class="d-flex flex-column min-vh-100">

  <div id="list-messages" class="d-flex flex-column mb-4 flex-grow-1">
    <#list messages>
      <#items as message>
        <#if message.AUTHOR == username>
          <div class="align-self-end mb-3 message-wrapper" data-id="${message.ID}">
            <div class="d-flex align-items-center justify-content-end gap-2">
              <button class="btn btn-sm btn-outline-danger border-0 delete-btn rounded-circle" title="Delete" style="padding: 0px 6px;">
                &times;
              </button>

              <div class="bg-primary text-white p-2 px-3 rounded-4 shadow-sm">
                ${message.CONTENT?html}
              </div>
            </div>
            <small class="text-muted d-block text-end mt-1">${(i18n["chat.message.me"])!"Me"}</small>
          </div>
        <#else>
          <div class="align-self-start mb-3" style="max-width: 75%;">
            <div class="bg-light text-dark p-2 px-3 rounded-4 shadow-sm border">
              ${message.CONTENT?html}
            </div>
            <small class="text-muted d-block mt-1">${message.AUTHOR?html}</small>
          </div>
        </#if>
      </#items>
    <#else>
      <p class="text-muted mt-3">${(i18n["chat.empty_state"])!"Start the conversation!"}</p>
    </#list>
  </div>

  <div class="bg-white p-3 border-top mt-auto sticky-bottom">
    <form id="chat-form" action="/create" method="POST" class="d-flex gap-2">
      <input type="text" id="content" name="content" class="form-control" placeholder="${(i18n["chat.input.placeholder"])!"Write your thoughts!"}" required autofocus>
      <button type="submit" class="btn btn-primary">${(i18n["chat.input.button"])!"Send"}</button>
    </form>
  </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1.5.2/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/vertx3-eventbus-client@3.9.4/vertx-eventbus.min.js"></script>

<script>
  window.CHAT_CONFIG = {
    username: "${username?js_string}",
    meText: "${((i18n["chat.message.me"])!"Me")?js_string}"
  };
</script>

<script src="/assets/js/chat.js"></script>

<#include "footer.ftl">
