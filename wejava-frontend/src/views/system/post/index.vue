<script setup lang="ts">
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { postSchema } from './schema'

const crud = useCrud(postSchema)
</script>

<template>
  <CrudPage :crud="crud" />
</template>
