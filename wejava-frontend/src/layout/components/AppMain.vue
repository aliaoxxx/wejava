<template>
  <router-view v-slot="{ Component, route }">
    <keep-alive :max="10">
      <component :is="Component" :key="route.path" />
    </keep-alive>
  </router-view>
</template>
