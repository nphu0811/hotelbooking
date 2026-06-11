(function() {
    var chatHistory = [];
    var isOpen = false;

    function initChatbot() {
        var toggleBtn = document.getElementById("lumi-chatbot-toggle");
        var windowEl = document.getElementById("lumi-chatbot-window");
        var closeBtn = document.getElementById("lumi-chat-close");
        var inputEl = document.getElementById("lumi-chat-input");
        var sendBtn = document.getElementById("lumi-chat-send");
        var messagesEl = document.getElementById("lumi-chat-messages");

        if (!toggleBtn || !windowEl) return;

        function toggleChat() {
            isOpen = !isOpen;
            if (isOpen) {
                windowEl.removeAttribute("hidden");
                setTimeout(function() { inputEl.focus(); }, 300);
            } else {
                windowEl.setAttribute("hidden", "");
            }
        }

        toggleBtn.addEventListener("click", toggleChat);
        closeBtn.addEventListener("click", toggleChat);

        function appendMessage(role, text) {
            var msgDiv = document.createElement("div");
            msgDiv.className = "lumi-msg " + (role === 'user' ? "lumi-msg-user" : "lumi-msg-bot");
            msgDiv.textContent = text;
            messagesEl.appendChild(msgDiv);
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }

        function showTyping() {
            var typingDiv = document.createElement("div");
            typingDiv.className = "lumi-msg lumi-msg-bot lumi-typing-indicator";
            typingDiv.innerHTML = "<div class='lumi-typing'><span></span><span></span><span></span></div>";
            messagesEl.appendChild(typingDiv);
            messagesEl.scrollTop = messagesEl.scrollHeight;
            return typingDiv;
        }

        function sendMessage() {
            var text = inputEl.value.trim();
            if (!text) return;

            // Add user msg to UI
            appendMessage('user', text);
            inputEl.value = "";
            
            // Add to history
            chatHistory.push({ role: "user", content: text });

            // Show typing
            var typingIndicator = showTyping();

            // Send request
            var token = document.querySelector('meta[name="_csrf"]');
            var header = document.querySelector('meta[name="_csrf_header"]');
            
            var headers = { "Content-Type": "application/json" };
            if (token && header) {
                headers[header.content] = token.content;
            }

            fetch("/api/chat", {
                method: "POST",
                headers: headers,
                body: JSON.stringify({ messages: chatHistory })
            })
            .then(function(res) { return res.json(); })
            .then(function(data) {
                messagesEl.removeChild(typingIndicator);
                var reply = data.reply || "Xin lỗi, tôi không thể trả lời lúc này.";
                appendMessage('assistant', reply);
                chatHistory.push({ role: "assistant", content: reply });
            })
            .catch(function(err) {
                messagesEl.removeChild(typingIndicator);
                appendMessage('assistant', "Đã xảy ra lỗi kết nối. Vui lòng thử lại sau.");
            });
        }

        sendBtn.addEventListener("click", sendMessage);
        inputEl.addEventListener("keypress", function(e) {
            if (e.key === "Enter") {
                e.preventDefault();
                sendMessage();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initChatbot);
    } else {
        initChatbot();
    }
})();
