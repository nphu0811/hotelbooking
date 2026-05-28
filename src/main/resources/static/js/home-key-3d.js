(function() {
    const container = document.getElementById('threejs-container');
    const heroSection = document.getElementById('home');
    
    if (!container || !heroSection) return;
    
    // Check WebGL support
    function hasWebGL() {
        try {
            const canvas = document.createElement('canvas');
            return !!(window.WebGLRenderingContext && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')));
        } catch(e) {
            return false;
        }
    }
    
    if (!hasWebGL()) return;
    
    const scene = new THREE.Scene();
    
    // Camera
    const camera = new THREE.PerspectiveCamera(75, heroSection.clientWidth / heroSection.clientHeight, 0.1, 1000);
    camera.position.z = 5;
    
    // Renderer
    const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    renderer.setSize(heroSection.clientWidth, heroSection.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);
    
    // Group for composition
    const compositionGroup = new THREE.Group();
    scene.add(compositionGroup);
    
    // Materials
    const platinumMaterial = new THREE.MeshPhysicalMaterial({
        color: 0xe5e7eb, // Platinum/Silver
        metalness: 0.9,
        roughness: 0.15,
        clearcoat: 1.0,
        clearcoatRoughness: 0.1,
        reflectivity: 1.0
    });
    
    // Key Factory
    function createKey() {
        const keyGroup = new THREE.Group();
        
        // Head (Ring of the key)
        const headGeom = new THREE.TorusGeometry(0.4, 0.1, 16, 100);
        const head = new THREE.Mesh(headGeom, platinumMaterial);
        head.position.y = 1.3;
        keyGroup.add(head);
        
        // Shaft
        const shaftGeom = new THREE.CylinderGeometry(0.08, 0.08, 1.8, 16);
        const shaft = new THREE.Mesh(shaftGeom, platinumMaterial);
        shaft.position.y = 0.3;
        keyGroup.add(shaft);
        
        // Teeth
        const tooth1Geom = new THREE.BoxGeometry(0.25, 0.15, 0.08);
        const tooth1 = new THREE.Mesh(tooth1Geom, platinumMaterial);
        tooth1.position.set(0.15, -0.2, 0);
        keyGroup.add(tooth1);
        
        const tooth2Geom = new THREE.BoxGeometry(0.35, 0.15, 0.08);
        const tooth2 = new THREE.Mesh(tooth2Geom, platinumMaterial);
        tooth2.position.set(0.2, -0.5, 0);
        keyGroup.add(tooth2);
        
        return keyGroup;
    }
    
    const key = createKey();
    key.rotation.z = -Math.PI / 4; // 45 degrees, ring head pointing top-right
    compositionGroup.add(key);
    
    // Position group strategically on the right side
    function updateLayout() {
        if (window.innerWidth > 1024) {
            compositionGroup.position.set(2.2, -0.2, -1);
            compositionGroup.scale.set(1.4, 1.4, 1.4);
        } else if (window.innerWidth > 768) {
            compositionGroup.position.set(1.5, -0.2, -2);
            compositionGroup.scale.set(1.2, 1.2, 1.2);
        } else {
            compositionGroup.position.set(0, 0.8, -3.5);
            compositionGroup.scale.set(1.0, 1.0, 1.0);
        }
    }
    
    updateLayout();
    
    // Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
    scene.add(ambientLight);
    
    const directionalLight1 = new THREE.DirectionalLight(0xffffff, 1.8);
    directionalLight1.position.set(5, 5, 5);
    scene.add(directionalLight1);
    
    const directionalLight2 = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight2.position.set(-5, 5, -5);
    scene.add(directionalLight2);
    
    const pointLight = new THREE.PointLight(0xffffff, 2, 10);
    pointLight.position.set(-2, 2, 2);
    scene.add(pointLight);
    
    // Mouse Interaction
    let mouseX = 0;
    let mouseY = 0;
    let targetX = 0;
    let targetY = 0;
    const windowHalfX = window.innerWidth / 2;
    const windowHalfY = window.innerHeight / 2;
    
    document.addEventListener('mousemove', (event) => {
        mouseX = (event.clientX - windowHalfX) * 0.001;
        mouseY = (event.clientY - windowHalfY) * 0.001;
    });
    
    // Animation Loop
    const clock = new THREE.Clock();
    
    function animate() {
        requestAnimationFrame(animate);
        const elapsedTime = clock.getElapsedTime();
        
        // Smooth mouse follow for group (parallax)
        targetX = mouseX * 0.6;
        targetY = mouseY * 0.6;
        
        compositionGroup.rotation.x += 0.05 * (targetY - compositionGroup.rotation.x);
        compositionGroup.rotation.y += 0.05 * (targetX - compositionGroup.rotation.y);
        
        // Soft floating animation
        compositionGroup.position.y += Math.sin(elapsedTime * 1.5) * 0.003;
        
        // Gentle swing for key
        key.rotation.x = Math.sin(elapsedTime * 1.2) * 0.08;
        key.rotation.y = Math.cos(elapsedTime * 0.8) * 0.08;
        
        renderer.render(scene, camera);
    }
    
    animate();
    
    // Add active class to indicate Three.js has initialized successfully
    document.documentElement.classList.add('threejs-active');
    
    // Handle Resize
    window.addEventListener('resize', () => {
        if (heroSection && camera && renderer) {
            camera.aspect = heroSection.clientWidth / heroSection.clientHeight;
            camera.updateProjectionMatrix();
            renderer.setSize(heroSection.clientWidth, heroSection.clientHeight);
            updateLayout();
        }
    });
})();
