[中文](../06-前端配置驱动设计.md) | English

# 06 · Frontend Configuration-Driven Design

> Goal: **describe 80% of standard list pages with configuration, handle 15% of differences with slots, and honestly hand-write the 5% of complex pages.**
> Status: **Implemented** (`components/Crud/` + `hooks/useCrud.ts`); the user management and role management pages have already been built this way.

---

## 1. Why "Full Configuration" Is Not an Option

The core contradiction of configuration-driven design: **the more generic the configuration, the more it looks like writing a new language; the more specialized the configuration, the less generic it is.**

So only one class of pages is made configurable: **"search + table + pagination + create/edit dialog + delete"**.
These pages account for 80% of back-office systems, and their shape is highly consistent.

### Three-Layer Strategy

| Layer | Coverage | Approach |
|---|---|---|
| **Configuration** | Standard CRUD list pages | One `CrudPage` component + one schema |
| **Slots** | Needs local customization (custom columns, extra buttons, linkage) | `render` functions / named slots are allowed in the schema |
| **Hand-written** | Dashboards, monitoring, tree editors, multi-step forms | Write normal `.vue` files |

🔴 **Hard rule**: functions are allowed in a schema, but module-branching code such as `if (module === 'user')` is **not**.
The moment it appears, that page should escape to hand-written code.

---

## 2. Schema Structure

```ts
// types/crud.ts
export interface CrudSchema<T = any> {
  /** Unique identifier, used for keep-alive and error locating */
  key: string

  /** Data source: four methods; missing one disables the corresponding capability */
  api: {
    list: (params: any) => Promise<R<PageResult<T>>>
    detail?: (id: string | number) => Promise<R<T>>
    create?: (data: any) => Promise<R<unknown>>
    update?: (data: any) => Promise<R<unknown>>
    remove?: (ids: string) => Promise<R<unknown>>
  }

  /** Primary key field name */
  rowKey: keyof T & string

  /** Permission prefix; automatically composed into :add / :edit / :remove / :export */
  permissionPrefix?: string

  search?: SearchField[]
  columns: Column<T>[]
  form?: FormField[]

  toolbar?: ToolbarAction[]
  rowActions?: RowAction<T>[]

  pagination?: { pageSize?: number; pageSizes?: number[] }
  dialog?: { width?: number; mode?: 'dialog' | 'drawer'; labelWidth?: string }

  /** Rewrite data before submit; return false to block submission */
  beforeSubmit?: (model: any, mode: 'create' | 'edit') => any | false
  /** After a successful submit */
  afterSubmit?: (mode: 'create' | 'edit', refresh: () => void) => void
}
```

### Search Fields

```ts
export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'date' | 'daterange' | 'number'
  placeholder?: string
  /** Dictionary type; automatically requests /system/dict/data/type/{dict} */
  dict?: string
  /** Static options; takes precedence over dict */
  options?: Option[]
  width?: number
}
```

### Table Columns

```ts
export interface Column<T> {
  prop: keyof T & string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right'
  sortable?: boolean
  /** Render a tag with a dictionary: pass dictType */
  dict?: string
  /** Date formatting, e.g. 'YYYY-MM-DD HH:mm:ss' */
  dateFormat?: string
  /** Fully custom rendering */
  render?: (row: T, index: number) => VNode | string
  /** Whether the value is copyable */
  copyable?: boolean
  showOverflowTooltip?: boolean
}
```

### Form Fields

```ts
export interface FormField {
  prop: string
  label: string
  type: FieldType
  required?: boolean
  rules?: FormItemRule[]
  dict?: string
  options?: Option[]
  placeholder?: string
  /** Grid width; 24 means the full row */
  span?: number
  /** Shown only on create / shown only on edit */
  only?: 'create' | 'edit'
  /** Dynamic visibility */
  visible?: (model: any) => boolean
  /** Dynamic disabling */
  disabled?: (model: any, mode: 'create' | 'edit') => boolean
  /** Helper text below the field */
  tip?: string
  /** Not part of the form, but required on submit (e.g. a hidden id) */
  hidden?: boolean
}
```

### Field Type Registry

```ts
export type FieldType =
  | 'input' | 'textarea' | 'password' | 'number'
  | 'select' | 'radio' | 'checkbox' | 'switch'
  | 'date' | 'datetime' | 'daterange' | 'time'
  | 'tree-select' | 'cascader' | 'upload' | 'editor'
```

Each type maps to a render component, registered in `components/Crud/fields/index.ts`:

```ts
export const fieldRenderers: Record<FieldType, Component> = {
  input: ElInput,
  textarea: ElInput,
  number: ElInputNumber,
  select: DictSelect,      // Built-in dictionary loading
  radio: DictRadio,
  date: ElDatePicker,
  // ...
}
```

**Extending with a new type** only requires adding an entry to this map; `CrudForm` itself does not need to change.

---

## 3. Runtime Wrapper: useCrud

```ts
export function useCrud<T>(schema: CrudSchema<T>) {
  const query = reactive({ pageNum: 1, pageSize: schema.pagination?.pageSize ?? 10 })
  const list = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)

  const dialog = reactive({
    visible: false,
    mode: 'create' as 'create' | 'edit',
    model: {} as any,
    title: ''
  })

  async function fetchList() { /* ... */ }
  function openCreate() { /* ... */ }
  function openEdit(row: T) { /* ... */ }
  async function submit() { /* ... */ }
  function openRemove(rows: T[]) { /* ... */ }

  return { query, list, total, loading, dialog, fetchList, openCreate, openEdit, submit, openRemove }
}
```

The consumer (the user management page) roughly looks like this:

```vue
<script setup lang="ts">
import { userSchema } from './schema'

const crud = useCrud(userSchema)
</script>

<template>
  <CrudPage :schema="userSchema" :crud="crud" />
</template>
```

**One standard page = one schema file + one line of component invocation**; that is the effect configuration-driven design is meant to achieve.

---

## 4. Component Breakdown

| Component | Responsibility | Replaceable |
|---|---|---|
| `CrudPage` | Orchestration: search area + toolbar + table + pagination + dialog | No |
| `CrudSearch` | Renders the query form from `search[]` | Yes |
| `CrudTable` | Renders the table from `columns[]`, handles multi-select | Yes |
| `CrudForm` | Renders the form from `form[]`, handles validation | Yes |
| `DictSelect` / `DictTag` | Dictionary loading and rendering | Yes |

---

## 5. Permission Integration

After `schema.permissionPrefix = 'system:user'`:

| Element | Automatically applied permission |
|---|---|
| Toolbar "Add" | `system:user:add` |
| Row action "Edit" | `system:user:edit` |
| Row action "Delete" | `system:user:remove` |
| Toolbar "Export" | `system:user:export` |

Buttons without permission are **not rendered** (not `disabled`, to avoid users clicking repeatedly).

> 🔴 Hiding on the frontend is only a UX optimization; every backend endpoint must still have `@PreAuthorize`.

---

## 6. Three Things the Backend Must Provide

| Requirement | Status |
|---|---|
| Unified pagination `{ total, list }` and `pageNum/pageSize` parameters | ✅ Done in M1 (`PageQuery` / `PageResult`) |
| Dictionary endpoint `/system/dict/data/type/{dictType}` | ⏳ M4 |
| Return the latest data or null after create/update | ✅ Returning `R<Void>` is enough; the frontend refreshes the list |

**Not doing**: having the backend deliver column definitions (headers, widths, validation rules). In the first phase, configuration stays on the frontend,
to avoid sliding into a low-code platform too early — that would turn debugging into "jumping back and forth between JSON and code".

---

## 7. Which Pages Should Not Be Configurable

| Page | Reason |
|---|---|
| Dashboard | The layout is charts, not tables |
| Service monitoring | Real-time refresh + ECharts |
| Menu management | Tree table + icon picker + complex parent-child validation |
| Role permission assignment | Transfer/tree checkboxes; non-standard interaction |
| Personal center | Form-dominated, no list |
| Import flow | Upload → preview → validate → confirm, multi-step |

Menu management can be built as a hybrid of "configuration-driven + inline slots", but it is not recommended to force-fit it from the start.

---

## 8. Rollout Order (Important)

🔴 **Do not write generic components before writing the pages.** The schema designed that way will certainly be missing things.

The correct order:

```
1. Hand-write the "User Management" page (the most typical CRUD)
2. Hand-write the "Role Management" page
3. Compare the two pages and find the 80% shared structure
4. Abstract the schema type + CrudPage
5. Rewrite these two pages with the schema to validate whether the abstraction holds
6. Use "Department Management" to validate a third scenario (tree table — it will expose the schema's weaknesses)
7. Then roll out to dictionary/parameters/notices
```

Step 6 is the key: **if the tree table does not fit, admit that it does not belong to configuration-driven design**, rather than turning the schema into a universal interpreter.

---

## 9. Anti-Pattern Checklist

| Anti-pattern | Consequence |
|---|---|
| Writing `if (module === 'xxx')` in a schema | Configuration degrades into code, harder to maintain than hand-writing |
| Adding 5 schema fields for one page | Abstraction leak — a sign that the page should escape |
| Making every page configurable | Complex pages become "configuration hell" |
| Banning functions entirely | The 80% is covered, but the remaining 20% forces you into hacks |
| No slot mechanism | A single custom column forces you to abandon the whole component |

---

## 10. Acceptance Criteria

After M3-3 is complete, the user management page should satisfy:

- [ ] Page file < 30 lines (the schema lives in a separate `schema.ts`)
- [ ] Adding a field only requires changing the schema, not the components
- [ ] Deleting the user management schema leaves the other three modules working as usual
- [ ] The number of buttons seen by an account without permissions matches the backend permissions
