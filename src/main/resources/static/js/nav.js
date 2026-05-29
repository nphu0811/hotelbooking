(function () {
    var sectionIds = ["home", "ai-recommendation", "popular-hotels", "hanoi", "hcm", "danang"];

    function onReady(callback) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback);
        } else {
            callback();
        }
    }

    function headerOffset() {
        var topbar = document.querySelector(".topbar");
        return topbar ? Math.ceil(topbar.getBoundingClientRect().height + 14) : 0;
    }

    function normalizePath(pathname) {
        return pathname.replace(/\/+$/, "") || "/";
    }

    function sameHomePath(link) {
        return normalizePath(link.pathname) === "/" && normalizePath(window.location.pathname) === "/";
    }

    function scrollToHash(hash, replaceHistory) {
        if (!hash || hash.length < 2) {
            return false;
        }

        var target = document.getElementById(decodeURIComponent(hash.slice(1)));
        if (!target) {
            return false;
        }

        var top = target.getBoundingClientRect().top + window.scrollY - headerOffset();
        window.scrollTo({ top: Math.max(0, top), behavior: "smooth" });

        if (replaceHistory) {
            history.replaceState(null, "", hash);
        } else {
            history.pushState(null, "", hash);
        }

        return true;
    }

    function setActiveLink(id) {
        document.querySelectorAll(".nav-links a[href*='#']").forEach(function (link) {
            var isActive = link.hash === "#" + id;
            link.classList.toggle("is-active", isActive);
            if (isActive) {
                link.setAttribute("aria-current", "true");
            } else {
                link.removeAttribute("aria-current");
            }
        });
    }

    function bindScrollSpy() {
        if (!("IntersectionObserver" in window)) {
            return;
        }

        var sections = sectionIds
            .map(function (id) { return document.getElementById(id); })
            .filter(Boolean);

        if (!sections.length) {
            return;
        }

        var visibleSet = {};

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                visibleSet[entry.target.id] = entry.isIntersecting;
            });

            // Pick the first visible section in DOM order
            for (var i = 0; i < sectionIds.length; i++) {
                if (visibleSet[sectionIds[i]]) {
                    setActiveLink(sectionIds[i]);
                    return;
                }
            }
        }, {
            rootMargin: "-" + headerOffset() + "px 0px -40% 0px",
            threshold: [0, 0.05, 0.1]
        });

        sections.forEach(function (section) {
            observer.observe(section);
        });
    }

    function bindStickyState() {
        var topbar = document.querySelector(".topbar");
        if (!topbar) {
            return;
        }

        function sync() {
            topbar.classList.toggle("is-scrolled", window.scrollY > 12);
        }

        sync();
        window.addEventListener("scroll", sync, { passive: true });
    }

    onReady(function () {
        var toggle = document.querySelector("[data-nav-toggle]");
        var panel = document.querySelector("[data-nav-panel]");
        var backButtons = document.querySelectorAll("[data-history-back]");
        var compactNavQuery = window.matchMedia ? window.matchMedia("(max-width: 1100px)") : null;

        function hasPersistentNavControls() {
            return panel && panel.querySelector('form[action="/logout"]');
        }

        function syncMenuA11y(open) {
            if (!panel) {
                return;
            }

            var compact = compactNavQuery ? compactNavQuery.matches : false;
            var hidePanel = compact && !open && !hasPersistentNavControls();

            if (hidePanel) {
                panel.setAttribute("aria-hidden", "true");
                panel.inert = true;
            } else {
                panel.removeAttribute("aria-hidden");
                panel.inert = false;
            }
        }

        function setMenu(open) {
            if (!toggle || !panel) {
                return;
            }
            toggle.setAttribute("aria-expanded", String(open));
            toggle.setAttribute("aria-label", open ? "Đóng menu" : "Mở menu");
            panel.classList.toggle("is-open", open);
            document.documentElement.classList.toggle("nav-open", open);
            syncMenuA11y(open);
        }

        if (toggle && panel) {
            setMenu(false);

            if (compactNavQuery) {
                if (compactNavQuery.addEventListener) {
                    compactNavQuery.addEventListener("change", function () { setMenu(false); });
                } else if (compactNavQuery.addListener) {
                    compactNavQuery.addListener(function () { setMenu(false); });
                }
            }

            toggle.addEventListener("click", function () {
                setMenu(toggle.getAttribute("aria-expanded") !== "true");
            });

            panel.addEventListener("click", function (event) {
                var link = event.target.closest("a");
                if (link) {
                    setMenu(false);
                }
            });

            document.addEventListener("click", function (event) {
                if (!panel.classList.contains("is-open")) {
                    return;
                }
                if (!event.target.closest(".topbar")) {
                    setMenu(false);
                }
            });

            document.addEventListener("keydown", function (event) {
                if (event.key === "Escape") {
                    setMenu(false);
                }
            });
        }

        backButtons.forEach(function (button) {
            button.addEventListener("click", function () {
                if (window.history.length > 1) {
                    window.history.back();
                    return;
                }
                window.location.assign("/");
            });
        });

        document.querySelectorAll(".nav-links a[href*='#']").forEach(function (link) {
            link.addEventListener("click", function (event) {
                if (!sameHomePath(link)) {
                    return;
                }
                event.preventDefault();
                scrollToHash(link.hash);
            });
        });

        bindStickyState();
        bindScrollSpy();

        if (window.location.hash) {
            window.setTimeout(function () {
                scrollToHash(window.location.hash, true);
            }, 40);
        } else {
            setActiveLink("home");
        }
    });
})();
