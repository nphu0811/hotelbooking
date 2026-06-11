(function () {
    "use strict";

    function onReady(callback) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback);
            return;
        }
        callback();
    }

    function all(selector, scope) {
        return Array.prototype.slice.call((scope || document).querySelectorAll(selector));
    }

    function first(selector, scope) {
        return (scope || document).querySelector(selector);
    }

    function inFirstViewport(element) {
        var rect = element.getBoundingClientRect();
        return rect.top < window.innerHeight * 0.82;
    }

    function visibleTargets(selector) {
        return all(selector).filter(function (element) {
            return element.offsetParent !== null && !inFirstViewport(element);
        });
    }

    function animateIn(gsap, targets, vars) {
        if (!targets || !targets.length) {
            return;
        }

        gsap.from(targets, Object.assign({
            autoAlpha: 0,
            y: 24,
            duration: 0.72,
            ease: "power3.out",
            stagger: 0.06,
            clearProps: "transform,opacity,visibility"
        }, vars || {}));
    }

    function initPageIntro(gsap) {
        var topbar = first(".topbar");
        var tl = gsap.timeline({
            defaults: {
                duration: 0.72,
                ease: "power3.out",
                clearProps: "transform,opacity,visibility"
            }
        });

        if (topbar) {
            tl.from(topbar, { autoAlpha: 0, y: -18, duration: 0.5 }, 0);
        }

        if (first(".home-shell")) {
            tl.from(".home-shell .hero", { autoAlpha: 0, y: 30, scale: 0.985, duration: 0.88 }, 0.08)
                .from(".home-shell .hero-copy > *", { autoAlpha: 0, y: 30, stagger: 0.09 }, 0.28)
                .from(".home-shell .hero-key-stage", { autoAlpha: 0, y: 34, rotationY: -8, duration: 0.82 }, 0.34)
                .from(".home-shell .hero-search-wrap", { autoAlpha: 0, y: 26, scale: 0.98, duration: 0.72 }, 0.48);
            return;
        }

        if (first(".auth-page")) {
            tl.from(".auth-visual", { autoAlpha: 0, x: -28, duration: 0.78 }, 0.08)
                .from(".auth-visual-content > *", { autoAlpha: 0, y: 28, stagger: 0.08 }, 0.28)
                .from(".auth-panel > *", { autoAlpha: 0, x: 24, stagger: 0.055 }, 0.22);
            return;
        }

        animateIn(gsap, all(".page-header > *"), { y: 24, stagger: 0.07 });
        animateIn(gsap, all(".toolbar, .detail-image, .detail-copy, .booking-box, .summary, .hold-status, .payment-method-form"), {
            y: 22,
            stagger: 0.075,
            delay: 0.12
        });
    }

    function revealWithObserver(gsap, targets) {
        if (!("IntersectionObserver" in window)) {
            gsap.to(targets, { autoAlpha: 1, y: 0, duration: 0.72, stagger: 0.04 });
            return;
        }

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (!entry.isIntersecting) {
                    return;
                }

                observer.unobserve(entry.target);
                gsap.to(entry.target, {
                    autoAlpha: 1,
                    y: 0,
                    duration: 0.68,
                    ease: "power3.out",
                    clearProps: "transform,opacity,visibility"
                });
            });
        }, { rootMargin: "0px 0px -12% 0px", threshold: 0.08 });

        targets.forEach(function (target) {
            observer.observe(target);
        });
    }

    function initScrollReveal(gsap, ScrollTrigger) {
        var revealSelector = [
            ".home-shell .section-heading > *",
            ".home-shell .ai-recommendation-panel",
            ".home-shell .room-card",
            ".page .room-row",
            ".page .table-wrap",
            ".page .premium-panel",
            ".page .review",
            ".page .metric-card",
            ".page .ops-panel",
            ".page .admin-tile",
            ".page .payment-method-card",
            ".page .verification-item",
            ".page .otp-inline",
            ".page .empty",
            ".page .alert",
            ".page .success",
            ".site-footer .footer-col",
            ".site-footer .footer-bottom"
        ].join(",");
        var targets = visibleTargets(revealSelector);

        if (!targets.length) {
            return;
        }

        gsap.set(targets, { autoAlpha: 0, y: 30, willChange: "transform,opacity" });

        if (!ScrollTrigger) {
            revealWithObserver(gsap, targets);
            return;
        }

        ScrollTrigger.batch(targets, {
            start: "top 88%",
            once: true,
            interval: 0.08,
            batchMax: 8,
            onEnter: function (batch) {
                gsap.to(batch, {
                    autoAlpha: 1,
                    y: 0,
                    duration: 0.72,
                    ease: "power3.out",
                    stagger: { each: 0.055, from: "start" },
                    clearProps: "transform,opacity,visibility,willChange"
                });
            }
        });
    }

    function initAdminMotion(gsap, ScrollTrigger) {
        all(".chart-svg path[stroke]").forEach(function (path) {
            if (typeof path.getTotalLength !== "function") {
                return;
            }

            var length = path.getTotalLength();
            var vars = {
                strokeDashoffset: 0,
                duration: 1.45,
                ease: "power2.out"
            };

            gsap.set(path, {
                strokeDasharray: length,
                strokeDashoffset: length
            });

            if (ScrollTrigger) {
                vars.scrollTrigger = {
                    trigger: path.closest(".ops-panel") || path,
                    start: "top 82%",
                    once: true
                };
            }

            gsap.to(path, vars);
        });

        all(".signal-meter span").forEach(function (bar) {
            var vars = {
                scaleX: 1,
                duration: 0.9,
                ease: "power3.out",
                clearProps: "transform"
            };

            gsap.set(bar, {
                scaleX: 0,
                transformOrigin: "left center"
            });

            if (ScrollTrigger) {
                vars.scrollTrigger = {
                    trigger: bar.closest(".ops-panel") || bar,
                    start: "top 86%",
                    once: true
                };
            }

            gsap.to(bar, vars);
        });
    }

    function initMicroInteractions(gsap, finePointer) {
        var pressSelector = ".button, .oauth-btn, .lang-selector-btn, .theme-toggle, .verify-badge.not-verified";

        document.addEventListener("pointerdown", function (event) {
            var target = event.target.closest(pressSelector);
            if (!target || target.disabled || target.getAttribute("aria-disabled") === "true") {
                return;
            }

            gsap.to(target, { scale: 0.985, duration: 0.12, ease: "power2.out", overwrite: "auto" });
        }, true);

        ["pointerup", "pointercancel", "pointerleave"].forEach(function (eventName) {
            document.addEventListener(eventName, function (event) {
                var target = event.target.closest(pressSelector);
                if (!target) {
                    return;
                }

                gsap.to(target, {
                    scale: 1,
                    duration: 0.22,
                    ease: "power2.out",
                    clearProps: "scale"
                });
            }, true);
        });

        var navToggle = first("[data-nav-toggle]");
        var navPanel = first("[data-nav-panel]");
        if (navToggle && navPanel) {
            navToggle.addEventListener("click", function () {
                window.setTimeout(function () {
                    if (!navPanel.classList.contains("is-open")) {
                        return;
                    }

                    animateIn(gsap, all(".nav-links a, .nav-auth > *", navPanel), {
                        y: -8,
                        duration: 0.34,
                        stagger: 0.035
                    });
                }, 0);
            });
        }

        if (!finePointer) {
            return;
        }

        all(".room-card, .room-row, .payment-method-card, .metric-card, .admin-tile, .oauth-btn").forEach(function (card) {
            card.addEventListener("mouseenter", function () {
                gsap.to(card, { y: -5, duration: 0.28, ease: "power2.out", overwrite: "auto" });
            });
            card.addEventListener("mouseleave", function () {
                gsap.to(card, { y: 0, duration: 0.32, ease: "power2.out", clearProps: "transform" });
            });
        });
    }

    function initDynamicResults(gsap) {
        var container = first("#ai-results");
        if (!container || !("MutationObserver" in window)) {
            return;
        }

        var queued = false;
        var observer = new MutationObserver(function () {
            if (queued) {
                return;
            }

            queued = true;
            window.requestAnimationFrame(function () {
                queued = false;
                if (container.hidden) {
                    return;
                }

                animateIn(gsap, all(".ai-loader-container, .ai-empty-card, .ai-error-card, .ai-results-heading > *, .ai-result-card", container), {
                    y: 18,
                    duration: 0.58,
                    stagger: 0.055
                });
            });
        });

        observer.observe(container, { childList: true, subtree: true });
    }

    function initModalMotion(gsap) {
        var button = first("#lang-btn");
        var modal = first("#language-modal");

        if (!button || !modal) {
            return;
        }

        function animateModal() {
            if (modal.hidden || window.getComputedStyle(modal).display === "none") {
                return;
            }

            gsap.fromTo(modal,
                { autoAlpha: 0 },
                { autoAlpha: 1, duration: 0.22, ease: "power2.out", clearProps: "opacity,visibility" }
            );
            animateIn(gsap, all(".modal-container", modal), { y: 26, scale: 0.985, duration: 0.38, stagger: 0 });
            animateIn(gsap, all(".lang-item", modal), { y: 12, duration: 0.34, stagger: 0.012 });
        }

        button.addEventListener("click", function () {
            window.setTimeout(animateModal, 0);
        });
    }

    function initUserDropdown() {
        /* UI toggle logic that gracefully degrades if GSAP is unavailable or motion is reduced */
        document.addEventListener('click', function (e) {
            var toggleBtn = e.target.closest('[data-user-menu-toggle]');

            all('.user-dropdown-menu').forEach(function (menu) {
                var isRelated = toggleBtn && toggleBtn.nextElementSibling === menu;
                var isOpen = menu.classList.contains('is-open');
                
                var prefersReducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
                var useGsap = window.gsap && !prefersReducedMotion;

                if (isRelated && isOpen) {
                    /* Close menu */
                    if (useGsap) {
                        var tlClose = window.gsap.timeline({
                            defaults: { duration: 0.18, ease: "power2.in" },
                            onComplete: function () {
                                menu.classList.remove('is-open');
                                menu.style.display = 'none';
                                window.gsap.set(menu, { clearProps: "all" });
                            }
                        });
                        tlClose
                            .to(all('.dropdown-link, .dropdown-section-label, .dropdown-divider, .dropdown-logout', menu), {
                                autoAlpha: 0, y: -4, stagger: 0.01, duration: 0.1
                            }, 0)
                            .to(menu, { autoAlpha: 0, y: -8, scale: 0.96 }, 0.02);
                    } else {
                        menu.classList.remove('is-open');
                        menu.style.display = 'none';
                    }

                } else if (isRelated && !isOpen) {
                    /* Open menu */
                    menu.style.display = 'flex';
                    menu.classList.add('is-open');

                    if (useGsap) {
                        var items = all('.dropdown-link, .dropdown-section-label, .dropdown-divider, .dropdown-logout', menu);
                        var tlOpen = window.gsap.timeline({
                            defaults: { duration: 0.32, ease: "power3.out" }
                        });
                        tlOpen
                            .fromTo(menu,
                                { autoAlpha: 0, y: -12, scale: 0.96 },
                                { autoAlpha: 1, y: 0, scale: 1, clearProps: "transform" }
                            )
                            .fromTo(items,
                                { autoAlpha: 0, y: -6 },
                                { autoAlpha: 1, y: 0, stagger: 0.025, duration: 0.22, clearProps: "transform,opacity,visibility" },
                                "<0.06"
                            );
                    }

                } else if (!isRelated && isOpen) {
                    /* Close when clicking outside */
                    if (useGsap) {
                        window.gsap.to(menu, {
                            autoAlpha: 0, y: -8, scale: 0.96,
                            duration: 0.15, ease: "power2.in",
                            onComplete: function () {
                                menu.classList.remove('is-open');
                                menu.style.display = 'none';
                                window.gsap.set(menu, { clearProps: "all" });
                            }
                        });
                    } else {
                        menu.classList.remove('is-open');
                        menu.style.display = 'none';
                    }
                }
            });
        }, true);
    }

    // Initialize UI that must work regardless of GSAP
    initUserDropdown();

    onReady(function () {
        if (!window.gsap) {
            return;
        }

        var gsap = window.gsap;
        var ScrollTrigger = window.ScrollTrigger;
        if (ScrollTrigger) {
            gsap.registerPlugin(ScrollTrigger);
        }

        document.documentElement.classList.add("gsap-motion");
        gsap.defaults({
            duration: 0.7,
            ease: "power3.out",
            overwrite: "auto"
        });

        var mm = gsap.matchMedia();
        mm.add({
            reduceMotion: "(prefers-reduced-motion: reduce)",
            finePointer: "(pointer: fine)"
        }, function (context) {
            var reduceMotion = context.conditions.reduceMotion;
            var finePointer = context.conditions.finePointer;

            if (reduceMotion) {
                return;
            }

            initPageIntro(gsap);
            initScrollReveal(gsap, ScrollTrigger);
            initAdminMotion(gsap, ScrollTrigger);
            initMicroInteractions(gsap, finePointer);
            initDynamicResults(gsap);
            initModalMotion(gsap);

            if (ScrollTrigger) {
                window.addEventListener("load", function () {
                    ScrollTrigger.refresh();
                }, { once: true });
            }
        });
    });
})();
