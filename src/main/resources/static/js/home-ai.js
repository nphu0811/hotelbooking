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

    function loadScript(src, callback) {
        var s = document.createElement("script");
        s.src = src;
        s.async = true;
        s.onload = callback;
        s.onerror = function () {
            console.warn("Failed to load CDN script: " + src);
        };
        document.head.appendChild(s);
    }

    function initThreeJsKey() {
        var heroSection = document.querySelector(".home-shell .hero");
        var keyStage = document.querySelector(".home-shell .hero-key-stage");
        if (!heroSection || !keyStage) {
            return;
        }

        // Dynamically load Three.js from a robust CDN
        loadScript("https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js", function () {
            try {
                if (typeof THREE === "undefined") {
                    return;
                }

                // Create container overlay inside hero
                var container = document.createElement("div");
                container.id = "threejs-container";
                heroSection.appendChild(container);

                // Setup Scene
                var scene = new THREE.Scene();

                // Camera
                var camera = new THREE.PerspectiveCamera(75, heroSection.clientWidth / heroSection.clientHeight, 0.1, 1000);
                camera.position.z = 5;

                // Renderer
                var renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
                renderer.setSize(heroSection.clientWidth, heroSection.clientHeight);
                renderer.setPixelRatio(window.devicePixelRatio || 1);
                container.appendChild(renderer.domElement);

                // Group
                var compositionGroup = new THREE.Group();
                scene.add(compositionGroup);

                // Materials - High-fidelity shiny Platinum/Silver
                var platinumMaterial = new THREE.MeshPhysicalMaterial({
                    color: 0xe5e7eb,
                    metalness: 0.95,
                    roughness: 0.15,
                    clearcoat: 1.0,
                    clearcoatRoughness: 0.05,
                    reflectivity: 1.0
                });

                // Key Factory
                function createKey() {
                    var keyGroup = new THREE.Group();

                    // Head (Torus Ring)
                    var headGeom = new THREE.TorusGeometry(0.45, 0.11, 16, 100);
                    var head = new THREE.Mesh(headGeom, platinumMaterial);
                    head.position.y = 1.3;
                    keyGroup.add(head);

                    // Shaft (Cylinder)
                    var shaftGeom = new THREE.CylinderGeometry(0.085, 0.085, 1.8, 16);
                    var shaft = new THREE.Mesh(shaftGeom, platinumMaterial);
                    shaft.position.y = 0.3;
                    keyGroup.add(shaft);

                    // Teeth (Boxes)
                    var tooth1Geom = new THREE.BoxGeometry(0.26, 0.16, 0.08);
                    var tooth1 = new THREE.Mesh(tooth1Geom, platinumMaterial);
                    tooth1.position.set(0.16, -0.2, 0);
                    keyGroup.add(tooth1);

                    var tooth2Geom = new THREE.BoxGeometry(0.36, 0.16, 0.08);
                    var tooth2 = new THREE.Mesh(tooth2Geom, platinumMaterial);
                    tooth2.position.set(0.21, -0.5, 0);
                    keyGroup.add(tooth2);

                    return keyGroup;
                }

                // Add 1 Key
                var key = createKey();
                key.rotation.z = -Math.PI / 4;
                compositionGroup.add(key);

                // Position group strategically on the right side
                function updateLayout() {
                    if (window.innerWidth > 768) {
                        compositionGroup.position.set(2.8, -0.1, -1.8);
                        compositionGroup.scale.set(1.5, 1.5, 1.5);
                    } else {
                        compositionGroup.position.set(0, 0.8, -3.2);
                        compositionGroup.scale.set(1.15, 1.15, 1.15);
                    }
                }
                updateLayout();

                // Lighting
                var ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
                scene.add(ambientLight);

                var directionalLight = new THREE.DirectionalLight(0xffffff, 1.8);
                directionalLight.position.set(5, 5, 5);
                scene.add(directionalLight);

                var pointLight = new THREE.PointLight(0xe0dfdf, 2.5, 10);
                pointLight.position.set(-2, 2, 2);
                scene.add(pointLight);

                // Mouse interaction coordinates
                var mouseX = 0;
                var mouseY = 0;
                var targetX = 0;
                var targetY = 0;
                var windowHalfX = window.innerWidth / 2;
                var windowHalfY = window.innerHeight / 2;

                document.addEventListener("mousemove", function (event) {
                    mouseX = (event.clientX - windowHalfX) * 0.001;
                    mouseY = (event.clientY - windowHalfY) * 0.001;
                });

                // Clock for delta animation
                var clock = new THREE.Clock();

                // Animation loop
                function animate() {
                    requestAnimationFrame(animate);
                    var elapsedTime = clock.getElapsedTime();

                    // Parallax cursor tracking
                    targetX = mouseX * 0.55;
                    targetY = mouseY * 0.55;

                    compositionGroup.rotation.x += 0.05 * (targetY - compositionGroup.rotation.x);
                    compositionGroup.rotation.y += 0.05 * (targetX - compositionGroup.rotation.y);

                    // Float y-axis bounciness
                    compositionGroup.position.y = Math.sin(elapsedTime * 1.5) * 0.18;

                    // Rotational key swings
                    key.rotation.x = Math.sin(elapsedTime * 1.2) * 0.08;
                    key.rotation.y = Math.cos(elapsedTime * 0.8) * 0.08;

                    renderer.render(scene, camera);
                }

                animate();

                // WebGL renderer compiles successfully, hide static fallback key stage safely
                keyStage.classList.add("threejs-active");

                // Handle resize event
                window.addEventListener("resize", function () {
                    if (heroSection && camera && renderer) {
                        camera.aspect = heroSection.clientWidth / heroSection.clientHeight;
                        camera.updateProjectionMatrix();
                        renderer.setSize(heroSection.clientWidth, heroSection.clientHeight);
                        updateLayout();
                    }
                });

            } catch (err) {
                console.warn("WebGL is unsupported or failed, relying on CSS fallback key:", err);
            }
        });
    }

    onReady(function () {
        bindSuggestions();
        bindHomeAiForm();
        initThreeJsKey();
    });
})();
