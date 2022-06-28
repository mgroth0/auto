modtype = LIB
apis(
  project(":k:file")
)
implementations(
  projects.k.kjlib.kjlibSocket,
  projects.k.kjlib.kjlibShell,
  projects.k.kjlib,
  projects.k.key,
  projects.k.async
)
