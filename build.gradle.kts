modtype = LIB

dependencies {
  api(projects.kj.json)
  implementation(projects.kj.kjlib.socket)
  implementation(projects.kj.kjlib.shell)
}