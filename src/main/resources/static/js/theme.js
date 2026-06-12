(function () {
    var storageKey = "lumiere-theme";
    var languageStorageKey = "lumiere-lang";
    var root = document.documentElement;
    var googleLanguageCodes = {
        "ar": "ar",
        "cs": "cs",
        "da": "da",
        "de": "de",
        "en": "en",
        "es": "es",
        "fr": "fr",
        "he": "iw",
        "id": "id",
        "it": "it",
        "ja": "ja",
        "ko": "ko",
        "nl": "nl",
        "no": "no",
        "pl": "pl",
        "pt-br": "pt",
        "ru": "ru",
        "sv": "sv",
        "th": "th",
        "tr": "tr",
        "vi": "vi",
        "zh-cn": "zh-CN",
        "zh-hk": "zh-TW",
        "zh-tw": "zh-TW"
    };
    var translateChromeInterval = null;

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

    function savedLanguage() {
        try {
            var language = localStorage.getItem(languageStorageKey) || "vi";
            return googleLanguageCodes[language] ? language : "vi";
        } catch (error) {
            return "vi";
        }
    }

    function persistLanguage(language) {
        try {
            localStorage.setItem(languageStorageKey, language);
        } catch (error) {
            return;
        }
    }

    function googleLanguageCode(language) {
        return googleLanguageCodes[language] || "vi";
    }

    function cookieDomain() {
        var hostname = window.location.hostname;
        var isLocalOrIp = hostname === "localhost" ||
            hostname === "127.0.0.1" ||
            hostname === "0.0.0.0" ||
            /^(\d{1,3}\.){3}\d{1,3}$/.test(hostname);
        return isLocalOrIp ? "" : "; domain=" + hostname;
    }

    function writeTranslationCookie(value, expires) {
        var cookie = "googtrans=" + value + "; path=/; SameSite=Lax";
        if (expires) {
            cookie += "; expires=" + expires;
        }
        document.cookie = cookie;
        document.cookie = cookie + cookieDomain();
    }

    function setTranslationCookie(language) {
        if (language === "vi") {
            writeTranslationCookie("", "Thu, 01 Jan 1970 00:00:00 GMT");
            return;
        }
        writeTranslationCookie("/vi/" + googleLanguageCode(language));
    }

    function hideGoogleTranslateChrome() {
        if (document.body) {
            if (document.body.style.top !== "0px") {
                document.body.style.top = "0px";
            }
            if (document.body.style.position) {
                document.body.style.position = "";
            }
            document.body.classList.add("lumiere-translate-clean");
        }

        var banners = document.querySelectorAll(
            ".goog-te-banner-frame, .goog-te-banner-frame.skiptranslate, body > .skiptranslate, iframe.skiptranslate, #goog-gt-tt, .goog-te-balloon-frame"
        );
        banners.forEach(function (banner) {
            if (banner.style.display !== "none") {
                banner.style.display = "none";
            }
            if (banner.style.visibility !== "hidden") {
                banner.style.visibility = "hidden";
            }
        });
    }

    function watchGoogleTranslateChrome() {
        hideGoogleTranslateChrome();

        if (translateChromeInterval) {
            return;
        }

        var runs = 0;
        translateChromeInterval = setInterval(function () {
            hideGoogleTranslateChrome();
            if (++runs > 20) {
                clearInterval(translateChromeInterval);
                translateChromeInterval = null;
            }
        }, 250);
    }

    function loadGoogleTranslate() {
        var translateHost = document.getElementById("google_translate_element");
        if (!translateHost) return;

        window.googleTranslateElementInit = function () {
            if (!window.google || !window.google.translate || translateHost.dataset.initialized === "true") {
                return;
            }

            new window.google.translate.TranslateElement({
                pageLanguage: "vi",
                includedLanguages: "ar,cs,da,de,en,es,fr,iw,id,it,ja,ko,nl,no,pl,pt,ru,sv,th,tr,vi,zh-CN,zh-TW",
                autoDisplay: false
            }, "google_translate_element");
            translateHost.dataset.initialized = "true";
            watchGoogleTranslateChrome();
            document.dispatchEvent(new CustomEvent("lumiere:translate-ready"));
        };

        if (window.google && window.google.translate) {
            window.googleTranslateElementInit();
            return;
        }

        if (document.getElementById("google-translate-script")) return;

        var script = document.createElement("script");
        script.id = "google-translate-script";
        script.src = "https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit";
        script.async = true;
        document.head.appendChild(script);
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
        
        var savedLang = savedLanguage();
        root.dataset.language = savedLang;
        updateLangUI(savedLang);
        setTranslationCookie(savedLang);
        loadGoogleTranslate();
        watchGoogleTranslateChrome();
        
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
            if (!googleLanguageCodes[langCode]) return;

            persistLanguage(langCode);
            root.dataset.language = langCode;
            updateLangUI(langCode);
            setTranslationCookie(langCode);

            if (langCode === 'vi') {
                window.location.reload();
            } else {
                var translateSelect = document.querySelector('.goog-te-combo');
                if (translateSelect) {
                    translateSelect.value = googleLanguageCode(langCode);
                    translateSelect.dispatchEvent(new Event('change'));
                    watchGoogleTranslateChrome();
                } else {
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

        function syncGoogleTranslate() {
            var translateSelect = document.querySelector('.goog-te-combo');
            if (!translateSelect) return false;

            var language = savedLanguage();
            var googleCode = googleLanguageCode(language);
            if (translateSelect.value !== googleCode && language !== 'vi') {
                translateSelect.value = googleCode;
                translateSelect.dispatchEvent(new Event('change'));
            }
            return true;
        }

        document.addEventListener("lumiere:translate-ready", syncGoogleTranslate, { once: true });

        var checkCount = 0;
        var checkInterval = setInterval(function() {
            if (syncGoogleTranslate()) {
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
