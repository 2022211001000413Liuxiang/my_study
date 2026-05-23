# Vue 2 核心语法复习

## 1. 响应式系统

### 数据绑定
```html
<div id="app">
  <p>{{ message }}</p>          <!-- 文本插值 -->
  <p v-once>{{ msg }}</p>        <!-- 只渲染一次 -->
  <p v-html="rawHtml"></p>       <!-- 输出 HTML -->
</div>
```

```js
new Vue({
  el: '#app',
  data: {
    message: 'Hello Vue',
    rawHtml: '<span>HTML内容</span>'
  }
})
```

### 绑定方式
| 指令 | 写法 | 作用 |
|------|------|------|
| `v-bind` | `:title="msg"` | 动态绑定属性 |
| `v-on` | `@click="handle"` | 绑定事件 |
| `v-model` | `v-model="input"` | 双向绑定 |
| `v-show` | `v-show="show"` | display 控制 |
| `v-if` | `v-if="show"` | 条件渲染 |
| `v-for` | `v-for="item in list"` | 列表渲染 |

---

## 2. 条件渲染

```html
<div v-if="type === 'A'">A</div>
<div v-else-if="type === 'B'">B</div>
<div v-else>Other</div>
```

- `v-show`：始终渲染，只是切换 `display`
- `v-if`：`true` 时才渲染，适合不频繁切换的场景

---

## 3. 列表渲染

```html
<!-- 遍历数组 -->
<li v-for="(item, index) in items" :key="index">
  {{ item.name }}
</li>

<!-- 遍历对象 -->
<li v-for="(value, key, index) in object">
  {{ index }}. {{ key }}: {{ value }}
</li>
```

> **注意**：`:key` 用于就地复用策略，提升渲染性能。

---

## 4. 事件处理

```html
<button @click="handle('arg', $event)">点击</button>
```

```js
methods: {
  handle(arg, event) {
    console.log(arg, event.target.innerText)
  }
}
```

- 事件修饰符：`.stop`、`.prevent`、`.capture`、`.self`、`.once`、`.passive`
- 按键修饰符：`.enter`、`.tab`、`.delete`、`.esc`、`.space`
- 组合键：`.ctrl`、`.alt`、`.shift`、`.meta`

```html
<!-- 阻止冒泡 + 阻止默认行为 -->
<a @click.stop.prevent="doSomething">链接</a>
```

---

## 5. 双向绑定（v-model）

```html
<!-- 文本 -->
<input v-model="message" />

<!-- 复选框 -->
<input type="checkbox" v-model="checked" :true-value="1" :false-value="0" />

<!-- 单选 -->
<input type="radio" v-model="pick" value="A" />

<!-- 选择框 -->
<select v-model="selected">
  <option value="a">A</option>
</select>
```

- 修饰符：`.lazy`（change 事件同步）、`.number`（转数字）、`.trim`（去空格）

---

## 6. 计算属性与侦听器

### 计算属性（computed）
调用直接{{fullName}},比起methods多了缓存，适用与常变动的数据
computed 的"缓存"是：如果依赖没变，多次使用返回同一个计算结果，不会重复执行 getter 函数。
##### 在vscode的项目中流程如下：
- 读取 `this.firstName` → Vue 记录：fullName 依赖 firstName
- 读取 `this.lastName` → Vue 记录：fullName 依赖 lastName
- 当 firstName **变化** → Vue 通知 fullName 重新执行
- 当 lastName **变化** → Vue 通知 fullName 重新执行
```js
computed: {
  // 完整写法
  fullName: {
    get() { return this.firstName + ' ' + this.lastName },
    set(val) { /* ... */ }
  },
  // 简写
  reversedMessage() {
    return this.message.split('').reverse().join('')
  }
}
```

### 侦听器（watch）
```js
watch: {
  // 简写
  message(newVal, oldVal) { /* ... */ },
  // 完整写法（ immediate / deep ）
  obj: {
    handler(newVal) { /* ... */ },
    immediate: true,
    deep: true
  }
}
```

- `computed`：适合依赖多个响应式属性计算结果，**有缓存**
- `watch`：适合异步或开销大的操作

---

## 7. Class 与 Style 绑定

：属性<==>v-bind:
```html
<!-- class -->
<div :class="{ active: isActive, 'text-danger': hasError }"></div>
<div :class="[activeClass, errorClass]"></div>

<!-- style -->
<div :style="{ color: activeColor, fontSize: fontSize + 'px' }"></div>
```

---

## 8. 组件基础

### 注册与使用
```js
// 全局注册
Vue.component('my-component', {
  template: '<div>组件内容</div>',
  data() { return { count: 0 } },  // data 必须是函数
  methods: { /* ... */ },
  props: ['title', 'likes'],       // 接收父组件传来的值
  computed: { /* ... */ },
  watch: { /* ... */ }
})

// 局部注册
const Child = { template: '<p>Child</p>' }
new Vue({
  components: { Child }
})
```

### 父子组件通信

**父 → 子（Props Down）**
```html
<!-- 父组件 -->
<child-component :message="parentMsg" @event="handleEvent"></child-component>
```
```js
// 子组件
props: {
  message: String,
  // 或
  message: {
    type: String,
    required: true,
    default: '默认值'
  }
}
```

**子 → 父（Events Up）**
```js
// 子组件
this.$emit('event', payload)

// 父组件
<child-component @event="handle"></child-component>
methods: {
  handle(payload) { /* ... */ }
}
```

### refs
```js
// 获取子组件实例或原生 DOM
this.$refs.myRef    // 组件 → 访问子组件数据/方法
this.$refs.myInput  // DOM → this.$refs.myInput.focus()
```

---

## 9. 插槽（Slot）

在父组件中对子组件的内容进行插入或修改再引用，可以方便定制化组件，子组件中存的是模板。

```html
<!-- 父组件 -->
<my-component>
  <p>默认插槽内容</p>
  <template #header>具名插槽header</template>
</my-component>

<!-- 子组件 my-component -->
<slot>后备内容</slot>
<slot name="header"></slot>
```

- **具名插槽**：`v-slot:header` 或 `#header`
- **作用域插槽**：子组件通过 slot 传递数据给父组件

```html
<!-- 子组件 -->
<slot :user="userData"></slot>

<!-- 父组件 -->
<my-component>
  <template #default="slotProps">
    {{ slotProps.user.name }}
  </template>
</my-component>
```

---

## 10. 生命周期（8个钩子）

```
beforeCreate  →  created  →  beforeMount  →  mounted
                ↓
           beforeUpdate  →  updated
                ↓
          beforeDestroy  →  destroyed
```

| 阶段   | 钩子              | 用途                              |
| ---- | --------------- | ------------------------------- |
| 实例创建 | `beforeCreate`  | 实例刚创建，data/methods 不可用          |
|      | `created`       | 实例创建完成，可访问 data/methods，常用于接口请求 |
| 挂载   | `beforeMount`   | 模板编译完成，即将挂载                     |
|      | `mounted`       | DOM 已挂载，可操作 `$el`，可获取子组件 ref    |
| 更新   | `beforeUpdate`  | 数据变化，DOM 更新前                    |
|      | `updated`       | DOM 更新完成，避免在此更改数据导致死循环          |
| 销毁   | `beforeDestroy` | 实例即将销毁，清理定时器/事件绑定               |
|      | `destroyed`     | 实例已销毁                           |

---

## 11. 过渡与动画

```html
<transition name="fade">
  <p v-if="show">Hello</p>
</transition>
```

```css
/* CSS 类名约定 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.5s; }
.fade-enter, .fade-leave-to { opacity: 0; }
```

---

## 12. 过滤器（Filters）--->可废除

```html
<!-- 使用 -->
{{ message | capitalize }}
{{ message | filterA | filterB }}
{{ message | filterA('arg1', arg2) }}
```

```js
// 全局定义
Vue.filter('capitalize', function(value) {
  return value.charAt(0).toUpperCase() + value.slice(1)
})

// 局部定义
filters: {
  capitalize(value) { return value.toUpperCase() }
}
```

> Vue 3 已移除过滤器，建议用计算属性或方法替代。

---

## 13. 混入（Mixins）

```js
// mixin
const myMixin = {
  data() { return { shared: 'shared data' } },
  created() { console.log('mixin created') }
}

// 使用
new Vue({
  mixins: [myMixin],
  created() { console.log('main created') }
})
```

- 组件选项会与 mixin 选项合并，冲突时**组件优先**
- 生命周期钩子会**合并执行**

---

## 14. 动态组件

```html
<component :is="currentView"></component>
```

配合 `<keep-alive>` 缓存组件状态：
```html
<keep-alive include="Home,About">
  <component :is="currentView"></component>
</keep-alive>
```

---

## 15. 边界处理

### $root / $parent / $children
```js
this.$root    // 根 Vue 实例
this.$parent  // 父实例
this.$children[0]  // 第一个子组件实例
```

### 依赖注入（Provide / Inject）
```js
// 祖先组件
provide() {
  return { theme: this.theme }
}

// 后代组件
inject: ['theme']
```

---

## 16. 常用实例属性与方法

```js
vm.$data     // 数据对象
vm.$el       // 根 DOM 元素
vm.$props    // props
vm.$options  // 实例构造器传入的选项
vm.$refs     // 注册过的 ref
vm.$root     // 根实例
vm.$parent   // 父实例

vm.$watch('msg', (newVal, oldVal) => { /* ... */ })
vm.$emit('event', payload)
vm.$on('event', callback)
vm.$once('event', callback)
vm.$forceUpdate()   // 强制更新
vm.$destroy()        // 销毁实例
```

---

## 17. Vue 2 响应式原理

- **原理**：`Object.defineProperty`（ES5），对 data 中的所有属性进行递归劫持
- **不足**：
  - 对象新增/删除属性**不会**触发响应式（用 `Vue.set` / `Vue.delete`）
  - 数组通过索引修改**不会**触发响应式（用 splice 或 `Vue.set`）

```js
Vue.set(target, key, value)
Vue.delete(target, key)
this.$set(target, key, value)
this.$delete(target, key)
```

- **数组响应式**：Vue 重写了 `push`、`pop`、`shift`、`unshift`、`splice`、`sort`、`reverse`，直接修改数组触发响应式

---

## 18. Vue Router 基础

```js
// 定义路由
const routes = [
  { path: '/', component: Home },
  { path: '/user/:id', component: User }
]

// 路由跳转
this.$router.push('/user/123')
this.$router.replace('/user/123')
this.$router.go(-1)

// 获取路由参数
this.$route.params.id
this.$route.query.name
```

---

## 19. Vuex 核心概念

```
State → Getter → Mutation → Action → State
```

| 概念 | 同步/异步 | 用途 |
|------|------|------|
| State | - | 存储状态 |
| Getter | - | 计算属性 |
| Mutation | 同步 | 改状态（`store.commit`） |
| Action | 异步 | 提交 mutation（`store.dispatch`） |

---

## 速查表

- **模板中**：`{{ }}` 插值、`v-bind:` 绑定属性、`@` 事件、`#` 插槽
- **组件通信**：props down, events up，refs / $parent / provide-inject
- **响应式**：对象用 set/delete，数组用 splice
- **生命周期**：created 拉数据，mounted 操作 DOM，beforeDestroy 清理
