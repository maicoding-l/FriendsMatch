<template>
  <div id="canvas-bg" ref="container"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';
import * as THREE from 'three';

const container = ref<HTMLDivElement | null>(null);
let renderer: THREE.WebGLRenderer | null = null;
let scene: THREE.Scene | null = null;
let camera: THREE.PerspectiveCamera | null = null;
let mesh: THREE.Points | null = null;
let animationId: number | null = null;

const initThree = () => {
  if (!container.value) return;

  scene = new THREE.Scene();
  camera = new THREE.PerspectiveCamera(75, window.innerWidth / window.innerHeight, 0.1, 1000);
  
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);
  container.value.appendChild(renderer.domElement);

  const geometry = new THREE.BufferGeometry();
  const count = 600;
  const pos = new Float32Array(count * 3);
  for(let i=0; i<count*3; i++) {
    pos[i] = (Math.random()-0.5)*15;
  }
  geometry.setAttribute('position', new THREE.BufferAttribute(pos, 3));
  
  const material = new THREE.PointsMaterial({ 
    size: 0.04, 
    color: 0x764ba2, 
    transparent: true, 
    opacity: 0.5 
  });
  
  mesh = new THREE.Points(geometry, material);
  scene.add(mesh);
  camera.position.z = 5;

  const animate = () => {
    animationId = requestAnimationFrame(animate);
    if (mesh) {
      mesh.rotation.y += 0.0005;
      mesh.rotation.x += 0.0002;
    }
    if (renderer && scene && camera) {
      renderer.render(scene, camera);
    }
  };
  animate();
};

const onResize = () => {
  if (camera && renderer) {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  }
};

onMounted(() => {
  initThree();
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  if (animationId) cancelAnimationFrame(animationId);
  if (renderer) renderer.dispose();
  if (scene) scene.clear();
});
</script>

<style scoped>
#canvas-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: -1;
  background: radial-gradient(circle at center, #1e1e2f 0%, #000000 100%);
  pointer-events: none;
}
</style>
