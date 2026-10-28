<script setup lang="ts">
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { configSchema } from './schema'

const crud = useCrud(configSchema)
</script>

<template>
  <CrudPage :crud="crud" />
</template>
