<script setup lang="ts">
import CrudPage from '@/components/Crud/CrudPage.vue'
import { useCrud } from '@/hooks/useCrud'
import { noticeSchema } from './schema'

const crud = useCrud(noticeSchema)
</script>

<template>
  <CrudPage :crud="crud" />
</template>
