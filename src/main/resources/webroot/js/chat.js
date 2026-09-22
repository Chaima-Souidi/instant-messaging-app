document.addEventListener('DOMContentLoaded', () => {
  const chatForm = document.getElementById('chat-form');
  const listMessages = document.getElementById('list-messages');

  const escapeHTML = (str) => {
    if (!str) return '';
    return str.toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  if (chatForm) {
    chatForm.addEventListener('submit', e => {
      e.preventDefault();
      const input = document.getElementById('content');

      fetch('/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        credentials: 'same-origin',
        body: 'content=' + encodeURIComponent(input.value)
      })
        .then(() => input.value = '')
        .catch(console.error);
    });
  }

  try {
    const eb = new EventBus('/eventbus');

    eb.onopen = function() {
      this.registerHandler('chat.to.client', (error, message) => {
        if (error) return console.error("EventBus reception error:", error);

        const data = message.body;
        const isMe = (data.author === window.CHAT_CONFIG.username);
        const safeAuthor = escapeHTML(data.author);
        const safeId = escapeHTML(data.id);
        const safeContent = data.content;

        let bubbleHtml = '';

        if (isMe) {
          bubbleHtml = `
            <div class="align-self-end mb-3 message-wrapper" data-id="${safeId || ''}">
              <div class="d-flex align-items-center justify-content-end gap-2">
                <button class="btn btn-sm btn-outline-danger border-0 delete-btn rounded-circle" title="Delete" style="padding: 0px 6px;">&times;</button>
                <div class="bg-primary text-white p-2 px-3 rounded-4 shadow-sm">${safeContent}</div>
              </div>
              <small class="text-muted d-block text-end mt-1">${window.CHAT_CONFIG.meText}</small>
            </div>
          `;
        } else {
          bubbleHtml = `
            <div class="align-self-start mb-3" style="max-width: 75%;">
              <div class="bg-light text-dark p-2 px-3 rounded-4 shadow-sm border">${safeContent}</div>
              <small class="text-muted d-block mt-1">${safeAuthor}</small>
            </div>
          `;
        }

        listMessages.insertAdjacentHTML('beforeend', bubbleHtml);
        window.scrollTo(0, document.body.scrollHeight);
      });
    };
  } catch (err) {
    console.error("EventBus initialization error:", err);
  }

  if (listMessages) {
    listMessages.addEventListener('click', function(e) {
      const btn = e.target.closest('.delete-btn');

      if (btn) {
        const messageContainer = btn.closest('.message-wrapper');
        const messageId = messageContainer.getAttribute('data-id');

        if (!messageId) {
          alert("Error: Message ID not found. Please refresh the page.");
          return;
        }

        if (confirm("Are you sure you want to delete this message?")) {
          fetch('/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            credentials: 'same-origin',
            body: 'id=' + encodeURIComponent(messageId)
          })
            .then(response => {
              if (response.ok || response.redirected) {
                messageContainer.style.transition = "opacity 0.3s";
                messageContainer.style.opacity = "0";
                setTimeout(() => messageContainer.remove(), 300);
              } else {
                console.error("Failed to delete message");
              }
            })
            .catch(console.error);
        }
      }
    });
  }
});
