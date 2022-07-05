modtype = LIB
apis(
  ":k:file".auto()
)
implementations(
  projects.k.kjlib.socket,
  projects.k.kjlib.shell,
  projects.k.kjlib,
  projects.k.key,
  projects.k.async,
  dependencies.kotlin("reflect")
)

plugins {
  kotlin("plugin.serialization")
}