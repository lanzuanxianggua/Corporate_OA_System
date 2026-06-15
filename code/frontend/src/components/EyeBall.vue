<template>
  <div
    ref="eyeRef"
    class="rounded-full flex items-center justify-center transition-all duration-150 overflow-hidden"
    :style="{
      width: `${size}px`,
      height: isBlinking ? '2px' : `${size}px`,
      backgroundColor: eyeColor,
    }"
  >
    <div
      v-if="!isBlinking"
      class="rounded-full transition-transform duration-100 ease-out"
      :style="{
        width: `${pupilSize}px`,
        height: `${pupilSize}px`,
        backgroundColor: pupilColor,
        transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`,
      }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";

interface Props {
  size?: number;
  pupilSize?: number;
  maxDistance?: number;
  eyeColor?: string;
  pupilColor?: string;
  isBlinking?: boolean;
  forceLookX?: number;
  forceLookY?: number;
}

const props = withDefaults(defineProps<Props>(), {
  size: 48,
  pupilSize: 16,
  maxDistance: 10,
  eyeColor: "white",
  pupilColor: "black",
  isBlinking: false,
  forceLookX: undefined,
  forceLookY: undefined
});

const eyeRef = ref<HTMLDivElement>();
const mouseX = ref(0);
const mouseY = ref(0);

const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
};

const pupilPosition = computed(() => {
  if (!eyeRef.value) return { x: 0, y: 0 };

  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY };
  }

  const eye = eyeRef.value.getBoundingClientRect();
  const eyeCenterX = eye.left + eye.width / 2;
  const eyeCenterY = eye.top + eye.height / 2;

  const deltaX = mouseX.value - eyeCenterX;
  const deltaY = mouseY.value - eyeCenterY;
  const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), props.maxDistance);

  const angle = Math.atan2(deltaY, deltaX);
  const x = Math.cos(angle) * distance;
  const y = Math.sin(angle) * distance;

  return { x, y };
});

onMounted(() => {
  window.addEventListener("mousemove", handleMouseMove);
});

onUnmounted(() => {
  window.removeEventListener("mousemove", handleMouseMove);
});
</script>
