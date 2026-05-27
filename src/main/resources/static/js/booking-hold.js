(function () {
    function normalizeInstant(value) {
        if (!value) {
            return "";
        }
        return value.replace(/\.(\d{3})\d*(Z|[+-]\d{2}:?\d{2})$/, ".$1$2");
    }

    function formatRemaining(milliseconds) {
        var totalSeconds = Math.max(0, Math.ceil(milliseconds / 1000));
        var minutes = Math.floor(totalSeconds / 60);
        var seconds = totalSeconds % 60;
        return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
    }

    function formatAbsolute(expiresAt) {
        try {
            return new Intl.DateTimeFormat("vi-VN", {
                hour: "2-digit",
                minute: "2-digit",
                day: "2-digit",
                month: "2-digit",
                year: "numeric"
            }).format(expiresAt);
        } catch (error) {
            return "";
        }
    }

    function disableActions(actions) {
        if (!actions || actions.length === 0) {
            return;
        }
        actions.forEach(function (action) {
            action.setAttribute("aria-disabled", "true");
            action.classList.add("is-disabled");
            if (action.tagName === "BUTTON") {
                action.disabled = true;
            }
            action.addEventListener("click", function (event) {
                event.preventDefault();
            });
        });
    }

    function bindHold(element) {
        var expiresAt = new Date(normalizeInstant(element.dataset.expiresAt));
        var countdown = element.querySelector("[data-hold-countdown]");
        var absolute = element.querySelector("[data-hold-absolute]");
        var actions = element.querySelectorAll("[data-hold-action]");
        if (actions.length === 0) {
            actions = document.querySelectorAll("[data-hold-action]");
        }

        if (Number.isNaN(expiresAt.getTime()) || !countdown) {
            return;
        }

        if (absolute) {
            var formatted = formatAbsolute(expiresAt);
            if (formatted) {
                absolute.textContent = formatted;
            }
        }

        function render() {
            var remaining = expiresAt.getTime() - Date.now();
            countdown.textContent = formatRemaining(remaining);
            if (remaining <= 0) {
                countdown.textContent = "00:00";
                element.classList.add("is-expired");
                disableActions(actions);
                window.clearInterval(timer);
            }
        }

        var timer = window.setInterval(render, 1000);
        render();
    }

    document.querySelectorAll("[data-hold-expiry]").forEach(bindHold);
})();
