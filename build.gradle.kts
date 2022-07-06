apis(
  ":k:file".auto()
)
implementations {
  kjlibSocket
  kjlibShell
  kjlib
  key
  async
}
implementations(
  dependencies.kotlin("reflect")
)

plugins {
  kotlin("plugin.serialization")
}