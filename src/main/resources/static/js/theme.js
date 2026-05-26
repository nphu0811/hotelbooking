(function () {
    var storageKey = "hotelbooking-theme";
    var root = document.documentElement;

    function savedTheme() {
        try {
            return localStorage.getItem(storageKey);
        } catch (error) {
            return null;
        }
    }

    function currentTheme() {
        return root.dataset.theme === "dark" ? "dark" : "light";
    }

    function persistTheme(theme) {
        try {
            localStorage.setItem(storageKey, theme);
        } catch (error) {
            return;
        }
    }

    function setTheme(theme, persist) {
        var nextTheme = theme === "dark" ? "dark" : "light";
        root.dataset.theme = nextTheme;

        var toggle = document.querySelector("[data-theme-toggle]");
        var label = document.querySelector("[data-theme-toggle-label]");

        if (toggle) {
            var isDark = nextTheme === "dark";
            toggle.setAttribute("aria-checked", String(isDark));
            toggle.setAttribute("aria-label", isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối");
        }

        if (label) {
            label.textContent = nextTheme === "dark" ? "Chế độ tối" : "Chế độ sáng";
        }

        if (persist) {
            persistTheme(nextTheme);
        }
    }

    setTheme(savedTheme(), false);

    function bindToggle() {
        var toggle = document.querySelector("[data-theme-toggle]");

        if (!toggle) {
            return;
        }

        setTheme(currentTheme(), false);
        toggle.addEventListener("click", function () {
            setTheme(currentTheme() === "dark" ? "light" : "dark", true);
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", bindToggle);
    } else {
        bindToggle();
    }
})();
