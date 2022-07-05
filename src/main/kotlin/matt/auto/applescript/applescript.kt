package matt.auto.applescript

import matt.kjlib.shell.execReturn
import matt.kjlib.shell.proc
import java.io.BufferedWriter
import java.net.URL
import kotlin.reflect.full.createInstance


@Suppress("unused") fun applescript(script: String, args: Array<String> = arrayOf(), functions: String = "") =
  osascript(script, args, functions = functions)

fun osascript(
  script: String, args: Array<String> = arrayOf(), functions: String = ""
): String {
  var realScript = "on run argv\n$script\nend run"
  if (functions.isNotBlank()) {
	realScript = realScript + "\n\n" + functions
  }

  return execReturn(null, "osascript", "-e", realScript, *args)
}

fun interactiveOsascript(script: String): Pair<BufferedWriter, Process> {
  val p = proc(null, "bash", "-c", "osascript 3<&0 <<'APPLESCRIPT'")
  val writer = p.outputStream.bufferedWriter()
  writer.write("$script\nAPPLESCRIPT")
  return writer to p
}

@DslMarker annotation class AppleScriptDSL

sealed class AppleScriptElement {
  var script: String = ""
}

@AppleScriptDSL
class AppleScript(op: AppleScript.()->Unit): AppleScriptElement() {
  inline fun <reified A: AppleScriptApplication> tell(op: A.()->Unit) {
	val app = A::class.createInstance()
	script += "tell application \"${app.name}\"\n"
	app.op()
	script += app.script
	script += "\n"
	script += "end tell"
  }

  init {
	op()
  }
}

@AppleScriptDSL
sealed class AppleScriptApplication(val name: String): AppleScriptElement() {
  fun activate() {
	script += "activate"
  }
}

class Spotify: AppleScriptApplication("Spotify") {
  fun openLocation(location: URL) {
	script += "open location \"$location\""
  }
}


private val TO_RECYCLE = """
  compiled: Boolean = true,
  
  if (compiled) {
  /*var f = scptMap[realScript]
	if (f == null) {
	  val scpt = TEMP_DIR["scpt"].apply { mkdirs() }[]
	  exec(null,"osacompile","-e",realScript,"-o")
	}*/
  }
""".trimIndent()