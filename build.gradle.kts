modtype = LIB
apis(
  ":k:file".auto()
)
implementations(
  projects.k.kjlib.kjlibSocket,
  projects.k.kjlib.kjlibShell,
  projects.k.kjlib,
  projects.k.key,
  projects.k.async
)

plugins {
  kotlin("plugin.serialization")
}