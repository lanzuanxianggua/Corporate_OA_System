<template>
  <div
    ref="pupilRef"
    class="rounded-full transition-transform duration-100 ease-out"
    :style="{
      width: `${size}px`,
      height: `${size}px`,
      backgroundColor: pupilColor,
      transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`,
    }"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";

interface Props {
  size?: number;
  maxDistance?: number;
  pupilColor?: string;
  forceLookX?: number;
  forceLookY?: number;
}

const props = withDefaults(defineProps<Props>(), {
  size: 12,
  maxDistance: 5,
  pupilColor: "black",
  forceLookX: undefined,
  forceLookY: undefined
});

const pupilRef = ref<HTMLDivElement>();
const mouseX = ref(0);
const mouseY = ref(0);

const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
};

const pupilPosition = computed(() => {
  if (!pupilRef.value) return { x: 0, y: 0 };

  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY };
  }

  const pupil = pupilRef.value.getBoundingClientRect();
  const pupilCenterX = pupil.left + pupil.width / 2;
  const pupilCenterY = pupil.top + pupil.height / 2;

  const deltaX = mouseX.value - pupilCenterX;
  const deltaY = mouseY.value - pupilCenterY;
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
