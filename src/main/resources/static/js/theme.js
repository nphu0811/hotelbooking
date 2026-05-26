(function () {
    var storageKey = "hotelbooking-theme";
    var root = document.documentElement;
    var toggle = document.querySelector("[data-theme-toggle]");
    var label = document.querySelector("[data-theme-toggle-label]");

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

    setTheme(currentTheme(), false);

    if (toggle) {
        toggle.addEventListener("click", function () {
            setTheme(currentTheme() === "dark" ? "light" : "dark", true);
        });
    }
})();
