<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Crosshair, GitFork, Network, RotateCcw, Search } from '@lucide/vue';
import type { GraphData, GraphEdge, GraphNode, Note } from '../types';
import { formatDate, statusLabel } from '../utils';

const props = defineProps<{
  graph: GraphData;
  selectedId: string;
  selectedCategory: string;
  query: string;
  note: Note | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  open: [id: string];
  refresh: [];
}>();

type Density = 'neighbors' | 'category' | 'all';
type PositionedNode = GraphNode & {
  x: number;
  y: number;
  radius: number;
  degree: number;
  color: string;
  active: boolean;
  highlighted: boolean;
  label: string;
};

const density = ref<Density>('neighbors');
const selectedGraphId = ref('');
const svgRef = ref<SVGSVGElement | null>(null);
const manualPositions = ref<Record<string, { x: number; y: number }>>({});
const draggingId = ref('');
const dragOrigin = ref<{ x: number; y: number; nodeX: number; nodeY: number } | null>(null);
const draggedDistance = ref(0);

const nodeMap = computed(() => new Map(props.graph.nodes.map((node) => [node.id, node])));
const selectedNodeId = computed(() => selectedGraphId.value || props.selectedId || props.graph.nodes[0]?.id || '');
const activeNode = computed(() => nodeMap.value.get(selectedNodeId.value) || props.graph.nodes[0] || null);

const degreeMap = computed(() => {
  const degrees = new Map<string, number>();
  for (const edge of props.graph.edges) {
    degrees.set(edge.source, (degrees.get(edge.source) || 0) + 1);
    degrees.set(edge.target, (degrees.get(edge.target) || 0) + 1);
  }
  return degrees;
});

const neighborIds = computed(() => {
  const ids = new Set<string>();
  if (!activeNode.value) return ids;
  ids.add(activeNode.value.id);
  for (const edge of props.graph.edges) {
    if (edge.source === activeNode.value.id) ids.add(edge.target);
    if (edge.target === activeNode.value.id) ids.add(edge.source);
  }
  return ids;
});

const visibleNodes = computed(() => {
  if (density.value === 'all') {
    return props.graph.nodes;
  }
  if (density.value === 'category') {
    const category = props.selectedCategory !== 'all'
      ? props.selectedCategory
      : activeNode.value?.category;
    return props.graph.nodes.filter((node) => !category || node.category === category || node.category.startsWith(`${category}/`));
  }
  return props.graph.nodes.filter((node) => neighborIds.value.has(node.id));
});

const visibleNodeIds = computed(() => new Set(visibleNodes.value.map((node) => node.id)));

const visibleEdges = computed(() => props.graph.edges.filter(
  (edge) => visibleNodeIds.value.has(edge.source) && visibleNodeIds.value.has(edge.target)
));

const maxWords = computed(() => Math.max(1, ...visibleNodes.value.map((node) => node.wordCount)));
const maxDegree = computed(() => Math.max(1, ...visibleNodes.value.map((node) => degreeMap.value.get(node.id) || 0)));
const searchTerm = computed(() => props.query.trim().toLowerCase());

const positionedNodes = computed<PositionedNode[]>(() => {
  const nodes = visibleNodes.value;
  if (!nodes.length) return [];
  const centerX = 600;
  const centerY = 390;
  const categories = [...new Set(nodes.map((node) => node.category.split('/')[0] || 'inbox'))];
  const sorted = [...nodes].sort((a, b) => {
    if (a.id === activeNode.value?.id) return -1;
    if (b.id === activeNode.value?.id) return 1;
    return (degreeMap.value.get(b.id) || 0) - (degreeMap.value.get(a.id) || 0) || a.title.localeCompare(b.title);
  });

  if (density.value === 'neighbors' && activeNode.value) {
    const rest = sorted.filter((node) => node.id !== activeNode.value?.id);
    return [
      buildNode(activeNode.value, centerX, centerY, categories),
      ...rest.map((node, index) => {
        const angle = (Math.PI * 2 * index) / Math.max(1, rest.length) - Math.PI / 2;
        const ring = rest.length > 8 && index % 2 ? 245 : 170;
        return buildNode(node, centerX + Math.cos(angle) * ring, centerY + Math.sin(angle) * ring, categories);
      })
    ];
  }

  return sorted.map((node, index) => {
    const ringIndex = Math.floor(index / 12);
    const ringPosition = index % 12;
    const itemsInRing = Math.min(12, sorted.length - ringIndex * 12);
    const angle = (Math.PI * 2 * ringPosition) / Math.max(1, itemsInRing) - Math.PI / 2 + ringIndex * 0.28;
    const radius = 118 + ringIndex * 120;
    return buildNode(node, centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, categories);
  });
});

const positionedMap = computed(() => new Map(positionedNodes.value.map((node) => [node.id, node])));

const relatedEdges = computed(() => {
  if (!activeNode.value) return [];
  return props.graph.edges
    .filter((edge) => edge.source === activeNode.value?.id || edge.target === activeNode.value?.id)
    .sort((a, b) => b.weight - a.weight)
    .slice(0, 8);
});

const relatedNodes = computed(() => relatedEdges.value
  .map((edge) => {
    const id = edge.source === activeNode.value?.id ? edge.target : edge.source;
    return { node: nodeMap.value.get(id), edge };
  })
  .filter((item): item is { node: GraphNode; edge: GraphEdge } => Boolean(item.node)));

watch(
  () => props.selectedId,
  (id) => {
    if (id) selectedGraphId.value = id;
  },
  { immediate: true }
);

function buildNode(node: GraphNode, x: number, y: number, categories: string[]): PositionedNode {
  const manual = manualPositions.value[node.id];
  const finalX = manual?.x ?? x;
  const finalY = manual?.y ?? y;
  const degree = degreeMap.value.get(node.id) || 0;
  const sizeScore = node.wordCount / maxWords.value;
  const degreeScore = degree / maxDegree.value;
  const highlighted = Boolean(searchTerm.value) && (
    node.title.toLowerCase().includes(searchTerm.value)
    || node.category.toLowerCase().includes(searchTerm.value)
    || node.tags.some((tag) => tag.toLowerCase().includes(searchTerm.value))
  );
  return {
    ...node,
    x: finalX,
    y: finalY,
    degree,
    radius: 16 + Math.round(sizeScore * 14 + degreeScore * 10),
    color: colorFor(node.category, categories),
    active: node.id === activeNode.value?.id,
    highlighted,
    label: shortenLabel(node.title)
  };
}

function shortenLabel(value: string) {
  if (value.length <= 10) return value;
  return `${value.slice(0, 9)}…`;
}

function colorFor(category: string, categories: string[]) {
  const palette = ['#62dac6', '#e7c16f', '#ff6f91', '#83df8f', '#8ab4ff', '#f29d72', '#b4e06b'];
  const root = category.split('/')[0] || 'inbox';
  const index = Math.max(0, categories.indexOf(root));
  return palette[index % palette.length];
}

function edgePath(edge: GraphEdge) {
  const source = positionedMap.value.get(edge.source);
  const target = positionedMap.value.get(edge.target);
  if (!source || !target) return '';
  const dx = target.x - source.x;
  const dy = target.y - source.y;
  const curve = Math.min(52, Math.hypot(dx, dy) * 0.18);
  const cx = (source.x + target.x) / 2 - (dy / Math.max(1, Math.hypot(dx, dy))) * curve;
  const cy = (source.y + target.y) / 2 + (dx / Math.max(1, Math.hypot(dx, dy))) * curve;
  return `M ${source.x} ${source.y} Q ${cx} ${cy} ${target.x} ${target.y}`;
}

function edgeActive(edge: GraphEdge) {
  return edge.source === activeNode.value?.id || edge.target === activeNode.value?.id;
}

function selectGraphNode(id: string) {
  selectedGraphId.value = id;
}

function openNode(id: string) {
  emit('open', id);
}

function resetLayout() {
  manualPositions.value = {};
  draggingId.value = '';
  dragOrigin.value = null;
  draggedDistance.value = 0;
}

function toSvgPoint(event: PointerEvent) {
  const svg = svgRef.value;
  if (!svg) return { x: 0, y: 0 };
  const point = svg.createSVGPoint();
  point.x = event.clientX;
  point.y = event.clientY;
  const matrix = svg.getScreenCTM();
  if (!matrix) return { x: 0, y: 0 };
  const transformed = point.matrixTransform(matrix.inverse());
  return { x: transformed.x, y: transformed.y };
}

function clampNodePosition(x: number, y: number, radius: number) {
  const padding = radius + 24;
  return {
    x: Math.max(padding, Math.min(1200 - padding, x)),
    y: Math.max(padding, Math.min(780 - padding, y))
  };
}

function onNodePointerDown(node: PositionedNode, event: PointerEvent) {
  if (event.button !== 0) return;
  selectGraphNode(node.id);
  draggingId.value = node.id;
  dragOrigin.value = { ...toSvgPoint(event), nodeX: node.x, nodeY: node.y };
  draggedDistance.value = 0;
  (event.currentTarget as SVGGraphicsElement | null)?.setPointerCapture?.(event.pointerId);
}

function onSvgPointerMove(event: PointerEvent) {
  if (!draggingId.value || !dragOrigin.value) return;
  const next = toSvgPoint(event);
  const dx = next.x - dragOrigin.value.x;
  const dy = next.y - dragOrigin.value.y;
  draggedDistance.value = Math.max(draggedDistance.value, Math.abs(dx) + Math.abs(dy));
  const node = positionedMap.value.get(draggingId.value);
  if (!node) return;
  const moved = clampNodePosition(dragOrigin.value.nodeX + dx, dragOrigin.value.nodeY + dy, node.radius);
  manualPositions.value = {
    ...manualPositions.value,
    [draggingId.value]: moved
  };
}

function onSvgPointerUp() {
  draggingId.value = '';
  dragOrigin.value = null;
}

function onNodeClick(node: PositionedNode) {
  if (draggedDistance.value > 6) {
    draggedDistance.value = 0;
    return;
  }
  selectGraphNode(node.id);
}
</script>

<template>
  <section class="graph-pane">
    <header class="graph-header">
      <div>
        <p class="eyebrow">Knowledge Graph</p>
        <h2>知识关联图谱</h2>
      </div>
      <div class="graph-controls" aria-label="图谱密度">
        <button :class="{ active: density === 'neighbors' }" type="button" @click="density = 'neighbors'">
          <Crosshair :size="16" /> 邻居
        </button>
        <button :class="{ active: density === 'category' }" type="button" @click="density = 'category'">
          <GitFork :size="16" /> 当前分类
        </button>
        <button :class="{ active: density === 'all' }" type="button" @click="density = 'all'">
          <Network :size="16" /> 全部
        </button>
        <button type="button" @click="resetLayout">
          <RotateCcw :size="16" /> 重置
        </button>
      </div>
    </header>

    <div class="graph-layout">
      <section class="graph-canvas" :class="{ loading }">
        <div class="graph-summary">
          <span>{{ graph.nodes.length }} 个节点</span>
          <span>{{ graph.edges.length }} 条关联</span>
          <span v-if="query"><Search :size="14" /> 高亮 “{{ query }}”</span>
        </div>

        <svg
          ref="svgRef"
          viewBox="0 0 1200 780"
          role="img"
          aria-label="知识关联图谱"
          @pointermove="onSvgPointerMove"
          @pointerup="onSvgPointerUp"
          @pointercancel="onSvgPointerUp"
          @pointerleave="onSvgPointerUp"
        >
          <path
            v-for="edge in visibleEdges"
            :key="`${edge.source}-${edge.target}`"
            class="graph-edge"
            :class="{ active: edgeActive(edge) }"
            :d="edgePath(edge)"
            :stroke-width="Math.min(6, 1 + edge.weight * 0.35)"
          />
          <g
            v-for="node in positionedNodes"
            :key="node.id"
            class="graph-node"
            :class="{
              active: node.active,
              highlighted: node.highlighted,
              due: node.dueReview,
              dragging: draggingId === node.id
            }"
            :transform="`translate(${node.x}, ${node.y})`"
            role="button"
            tabindex="0"
            @pointerdown.stop="onNodePointerDown(node, $event)"
            @click="onNodeClick(node)"
            @dblclick="openNode(node.id)"
            @keydown.enter="openNode(node.id)"
          >
            <circle class="node-halo" :r="node.radius + 8" :fill="node.color" />
            <circle class="node-core" :r="node.radius" :fill="node.color" />
            <text :y="node.radius + 18" text-anchor="middle">{{ node.label }}</text>
          </g>
        </svg>

        <div v-if="loading" class="loading-sheen" aria-hidden="true" />
        <div v-if="!graph.nodes.length && !loading" class="soft-empty">还没有可展示的笔记节点</div>
      </section>

      <aside class="graph-detail">
        <template v-if="activeNode">
          <div class="graph-card">
            <small>当前节点</small>
            <h3>{{ activeNode.title }}</h3>
            <div class="graph-facts">
              <span>{{ activeNode.category }}</span>
              <span>{{ activeNode.wordCount.toLocaleString() }} 字</span>
              <span>{{ statusLabel[activeNode.status] }}</span>
              <span v-if="activeNode.dueReview">今日待复习</span>
            </div>
            <div class="graph-tags">
              <span v-for="tag in activeNode.tags.slice(0, 8)" :key="tag">{{ tag }}</span>
            </div>
            <button class="open-note-button" type="button" @click="openNode(activeNode.id)">打开笔记</button>
          </div>

          <div class="graph-card">
            <small>关联笔记</small>
            <div class="related-list">
              <button
                v-for="{ node, edge } in relatedNodes"
                :key="node.id"
                type="button"
                @click="selectGraphNode(node.id)"
                @dblclick="openNode(node.id)"
              >
                <strong>{{ node.title }}</strong>
                <span>{{ node.category }} / 权重 {{ edge.weight }}</span>
                <em>{{ edge.reasons.join('；') }}</em>
              </button>
              <p v-if="!relatedNodes.length" class="soft-empty">这个节点暂时没有明显关联</p>
            </div>
          </div>

          <div class="graph-card graph-mini">
            <small>节点信息</small>
            <p>更新于 {{ formatDate(activeNode.updatedAt) }}</p>
            <p>连接数 {{ degreeMap.get(activeNode.id) || 0 }}</p>
          </div>
        </template>
      </aside>
    </div>
  </section>
</template>
