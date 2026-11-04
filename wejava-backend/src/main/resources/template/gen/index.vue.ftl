<script setup lang="ts">
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { ${table.businessName}Schema } from './schema'

const crud = useCrud(${table.businessName}Schema)
</script>

<template>
  <CrudPage :crud="crud" />
</template>
