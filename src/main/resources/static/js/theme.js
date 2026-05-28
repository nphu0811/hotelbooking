(function () {
    var storageKey = "lumiere-theme";
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
        var isDark = nextTheme === "dark";
        root.dataset.theme = nextTheme;

        var toggle = document.querySelector("[data-theme-toggle]");
        var label = document.querySelector("[data-theme-toggle-label]");

        if (toggle) {
            toggle.setAttribute("aria-checked", String(isDark));
            toggle.setAttribute("aria-label", isDark ? "Chuyển sang chế độ sáng" : "Chuyển sang chế độ tối");
        }

        if (label) {
            label.textContent = isDark ? "Chế độ tối" : "Chế độ sáng";
        }

        if (persist) {
            persistTheme(nextTheme);
        }
    }

    function initialTheme() {
        var saved = savedTheme();
        if (saved) {
            return saved;
        }
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            return "dark";
        }
        return "light";
    }

    setTheme(initialTheme(), false);

    function injectThemeToggle() {
        var navAuth = document.querySelector(".nav-auth");
        if (!navAuth) return;
        
        var existingToggle = document.querySelector("[data-theme-toggle]");
        if (existingToggle) return;
        
        var btn = document.createElement("button");
        btn.className = "theme-toggle";
        btn.setAttribute("data-theme-toggle", "");
        btn.setAttribute("aria-checked", "false");
        btn.setAttribute("type", "button");
        
        var track = document.createElement("div");
        track.className = "theme-toggle-track";
        
        var stars = document.createElement("span");
        stars.className = "theme-toggle-stars";
        for (var i = 1; i <= 5; i++) {
            var star = document.createElement("span");
            star.className = "star star-" + i;
            stars.appendChild(star);
        }
        
        var clouds = document.createElement("span");
        clouds.className = "theme-toggle-clouds";
        for (var j = 1; j <= 3; j++) {
            var cloud = document.createElement("span");
            cloud.className = "cloud cloud-" + j;
            clouds.appendChild(cloud);
        }
        
        track.appendChild(stars);
        track.appendChild(clouds);
        
        var thumb = document.createElement("div");
        thumb.className = "theme-toggle-thumb";
        
        for (var k = 1; k <= 3; k++) {
            var crater = document.createElement("span");
            crater.className = "crater crater-" + k;
            thumb.appendChild(crater);
        }
        
        btn.appendChild(track);
        btn.appendChild(thumb);
        
        navAuth.insertBefore(btn, navAuth.firstChild);
    }

    function initLanguageSelector() {
        var langBtn = document.getElementById("lang-btn");
        var modal = document.getElementById("language-modal");
        var closeBtn = document.getElementById("modal-close-btn");
        
        if (!langBtn || !modal) return;
        
        langBtn.addEventListener("click", function(e) {
            e.preventDefault();
            e.stopPropagation();
            modal.removeAttribute("hidden");
            modal.style.display = "flex";
        });
        
        function closeModal() {
            modal.setAttribute("hidden", "");
            modal.style.display = "none";
        }
        
        if (closeBtn) {
            closeBtn.addEventListener("click", function(e) {
                e.stopPropagation();
                closeModal();
            });
        }
        
        modal.addEventListener("click", function(e) {
            if (e.target === modal) {
                closeModal();
            }
        });
        
        document.addEventListener("keydown", function(e) {
            if (e.key === "Escape" && modal.style.display === "flex") {
                closeModal();
            }
        });
        
        var langItems = modal.querySelectorAll(".lang-item");
        langItems.forEach(function(item) {
            item.addEventListener("click", function() {
                langItems.forEach(function(el) {
                    el.classList.remove("active");
                    var check = el.querySelector(".checkmark");
                    if (check) check.style.visibility = "hidden";
                });
                
                item.classList.add("active");
                var check = item.querySelector(".checkmark");
                if (check) check.style.visibility = "visible";
                
                var langCode = item.getAttribute("data-lang");
                var matches = modal.querySelectorAll('.lang-item[data-lang="' + langCode + '"]');
                matches.forEach(function(match) {
                    match.classList.add("active");
                    var check = match.querySelector(".checkmark");
                    if (check) check.style.visibility = "visible";
                });

                var btnImg = langBtn.querySelector("img");
                var clickedImg = item.querySelector("img");
                if (btnImg && clickedImg) {
                    btnImg.src = clickedImg.src;
                    btnImg.alt = item.querySelector(".lang-name").textContent;
                }
                
                setTimeout(closeModal, 300);
            });
        });
    }

    function bindToggle() {
        injectThemeToggle();
        var toggle = document.querySelector("[data-theme-toggle]");

        initLanguageSelector();

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
