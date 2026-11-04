<script setup lang="ts">
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { contractSchema } from './schema'

const crud = useCrud(contractSchema)
</script>

<template>
  <CrudPage :crud="crud" />
</template>
