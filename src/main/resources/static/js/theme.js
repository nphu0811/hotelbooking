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
        
        // Initial setup from localStorage
        var savedLang = localStorage.getItem("lumiere-lang") || "vi";
        updateLangUI(savedLang);
        
        function updateLangUI(langCode) {
            var activeItem = modal.querySelector('.lang-item[data-lang="' + langCode + '"]');
            if (activeItem) {
                var langItems = modal.querySelectorAll(".lang-item");
                langItems.forEach(function(el) {
                    el.classList.remove("active");
                    var check = el.querySelector(".checkmark");
                    if (check) check.style.visibility = "hidden";
                });
                
                var matches = modal.querySelectorAll('.lang-item[data-lang="' + langCode + '"]');
                matches.forEach(function(match) {
                    match.classList.add("active");
                    var check = match.querySelector(".checkmark");
                    if (check) check.style.visibility = "visible";
                });

                var btnImg = langBtn.querySelector("img");
                var clickedImg = activeItem.querySelector("img");
                if (btnImg && clickedImg) {
                    btnImg.src = clickedImg.src;
                    btnImg.alt = activeItem.querySelector(".lang-name").textContent;
                }
            }
        }

        function setLanguage(langCode) {
            localStorage.setItem("lumiere-lang", langCode);
            updateLangUI(langCode);

            var googleLangCode = langCode;
            if (langCode === 'zh-cn') googleLangCode = 'zh-CN';
            if (langCode === 'zh-tw') googleLangCode = 'zh-TW';
            if (langCode === 'zh-hk') googleLangCode = 'zh-TW';
            if (langCode === 'pt-br') googleLangCode = 'pt';

            // Determine the domain for cookie
            var hostDomain = window.location.hostname;

            if (langCode === 'vi') {
                // Clear googtrans cookies on both path and domain
                document.cookie = "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/";
                document.cookie = "googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/; domain=" + hostDomain;
                // Also try removing the Google Translate frame
                var gtFrame = document.querySelector('.skiptranslate');
                if (gtFrame) {
                    var restoreBtn = document.querySelector('.goog-te-banner-frame');
                    if (restoreBtn) {
                        try {
                            restoreBtn.contentDocument.querySelector('.goog-close-link').click();
                        } catch(e) { /* cross-origin, fallback to reload */ }
                    }
                }
                window.location.reload();
            } else {
                // Set googtrans on both root path and with domain
                var cookieVal = "/vi/" + googleLangCode;
                document.cookie = "googtrans=" + cookieVal + "; path=/";
                document.cookie = "googtrans=" + cookieVal + "; path=/; domain=" + hostDomain;

                var translateSelect = document.querySelector('.goog-te-combo');
                if (translateSelect) {
                    translateSelect.value = googleLangCode;
                    translateSelect.dispatchEvent(new Event('change'));
                } else {
                    // Google Translate widget not yet loaded, reload so it picks up the cookie
                    window.location.reload();
                }
            }
        }
        
        var langItems = modal.querySelectorAll(".lang-item");
        langItems.forEach(function(item) {
            item.addEventListener("click", function() {
                var langCode = item.getAttribute("data-lang");
                setLanguage(langCode);
                setTimeout(closeModal, 300);
            });
        });

        // Sync with Google Translate Combo when it loads
        var checkCount = 0;
        var checkInterval = setInterval(function() {
            var translateSelect = document.querySelector('.goog-te-combo');
            if (translateSelect) {
                var googleLangCode = savedLang;
                if (savedLang === 'zh-cn') googleLangCode = 'zh-CN';
                if (savedLang === 'zh-tw') googleLangCode = 'zh-TW';
                if (savedLang === 'zh-hk') googleLangCode = 'zh-TW';
                if (savedLang === 'pt-br') googleLangCode = 'pt';

                if (translateSelect.value !== googleLangCode && savedLang !== 'vi') {
                    translateSelect.value = googleLangCode;
                    translateSelect.dispatchEvent(new Event('change'));
                }
                clearInterval(checkInterval);
            }
            if (++checkCount > 30) {
                clearInterval(checkInterval);
            }
        }, 500);
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
