(function () {
    function onReady(callback) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback);
        } else {
            callback();
        }
    }

    function findPromptField(button) {
        var form = button.closest("form");
        if (form) {
            return form.querySelector("textarea");
        }
        return document.querySelector("#ai-prompt, [data-recommend-prompt], textarea[name='prompt']");
    }

    function bindSuggestions() {
        document.querySelectorAll("[data-suggest-prompt]").forEach(function (button) {
            button.addEventListener("click", function () {
                var field = findPromptField(button);
                if (!field) {
                    return;
                }
                field.value = button.dataset.suggestPrompt || "";
                field.focus();
            });
        });
    }

    function clearNode(node) {
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    }

    function setLoading(container) {
        container.hidden = false;
        clearNode(container);

        var loader = document.createElement("div");
        loader.className = "ai-loader-container";

        var icon = document.createElement("div");
        icon.className = "ai-loader-icon";
        icon.textContent = "✦";

        var title = document.createElement("h4");
        title.textContent = "AI đang phân tích và tìm phòng phù hợp...";

        var copy = document.createElement("p");
        copy.className = "muted";
        copy.textContent = "Quá trình này có thể mất vài giây do hệ thống đang đối chiếu vị trí, tiện nghi và ngân sách.";

        loader.append(icon, title, copy);
        container.appendChild(loader);
        container.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function setEmpty(container) {
        container.hidden = false;
        clearNode(container);

        var empty = document.createElement("div");
        empty.className = "ai-empty-card";
        empty.textContent = "AI chưa tìm thấy phòng phù hợp hoàn toàn. Hãy thử mô tả linh hoạt hơn về khu vực, ngân sách hoặc tiện nghi.";
        container.appendChild(empty);
    }

    function setError(container, message) {
        container.hidden = false;
        clearNode(container);

        var error = document.createElement("div");
        error.className = "ai-error-card";

        var title = document.createElement("strong");
        title.textContent = "Lỗi gợi ý AI";

        var copy = document.createElement("p");
        copy.textContent = message || "Yêu cầu thất bại. Vui lòng thử lại.";

        error.append(title, copy);
        container.appendChild(error);
    }

    function addText(parent, tag, className, text) {
        var element = document.createElement(tag);
        if (className) {
            element.className = className;
        }
        element.textContent = text || "";
        parent.appendChild(element);
        return element;
    }

    function renderResults(container, data) {
        if (!Array.isArray(data) || data.length === 0) {
            setEmpty(container);
            return;
        }

        container.hidden = false;
        clearNode(container);

        var heading = document.createElement("div");
        heading.className = "section-heading ai-results-heading";
        var titleWrap = document.createElement("div");
        addText(titleWrap, "p", "page-kicker", "AI Curated Picks");
        addText(titleWrap, "h3", "", "Đề xuất tốt nhất cho bạn");
        heading.appendChild(titleWrap);

        var scroller = document.createElement("div");
        scroller.className = "room-scroll-container";

        data.forEach(function (rec) {
            var card = document.createElement("article");
            card.className = "room-card ai-curated-card ai-result-card";

            var imageWrap = document.createElement("div");
            imageWrap.className = "ai-card-img-wrapper";
            var image = document.createElement("img");
            image.src = rec.primaryImageUrl || "/css/room-placeholder.svg";
            image.alt = rec.name || "Hotel room";
            var badge = document.createElement("span");
            badge.className = "ai-card-badge";
            badge.textContent = "AI Pick";
            imageWrap.append(image, badge);

            var body = document.createElement("div");
            body.className = "room-card-body";
            addText(body, "h3", "ai-card-title", rec.name);
            addText(body, "p", "muted ai-card-subtitle", [rec.hotelName, rec.hotelCity].filter(Boolean).join(" • "));

            var reason = document.createElement("div");
            reason.className = "ai-reason-bubble";
            addText(reason, "span", "ai-reason-icon", "i");
            addText(reason, "p", "", rec.reason);
            body.appendChild(reason);

            var footer = document.createElement("div");
            footer.className = "ai-card-footer";
            var price = addText(footer, "p", "price", new Intl.NumberFormat("vi-VN").format(rec.pricePerNight || 0) + " VND");
            var unit = document.createElement("span");
            unit.className = "price-unit";
            unit.textContent = " / đêm";
            price.appendChild(unit);
            var link = document.createElement("a");
            link.className = "button ai-book-btn";
            link.href = "/rooms/" + encodeURIComponent(rec.roomId);
            link.textContent = "Đặt ngay";
            footer.appendChild(link);
            body.appendChild(footer);

            card.append(imageWrap, body);
            scroller.appendChild(card);
        });

        container.append(heading, scroller);
    }

    function bindHomeAiForm() {
        var form = document.getElementById("ai-form");
        var prompt = document.getElementById("ai-prompt");
        var token = document.getElementById("csrf-token");
        var results = document.getElementById("ai-results");

        if (!form || !prompt || !results) {
            return;
        }

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            setLoading(results);

            var body = new URLSearchParams();
            body.append("prompt", prompt.value);

            fetch("/api/recommend", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                    "X-CSRF-Token": token ? token.value : ""
                },
                body: body
            })
                .then(function (response) {
                    if (!response.ok) {
                        return response.json().then(function (payload) {
                            throw new Error(payload.error || "Yêu cầu thất bại");
                        });
                    }
                    return response.json();
                })
                .then(function (data) {
                    renderResults(results, data);
                })
                .catch(function (error) {
                    setError(results, error.message);
                });
        });
    }

    onReady(function () {
        bindSuggestions();
        bindHomeAiForm();
    });
})();
