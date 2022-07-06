package matt.auto.applescript

import kotlinx.serialization.Serializable
import matt.kjlib.shell.ExecReturner
import matt.kjlib.shell.Shell
import matt.kjlib.shell.proc
import java.io.BufferedWriter
import java.net.URL
import kotlin.reflect.full.createInstance


@Suppress("unused") fun applescript(script: String, args: Array<String> = arrayOf(), functions: String = "") =
  osascript(script, args, functions = functions)


fun osascript(
  script: String,
  args: Array<String> = arrayOf(),
  functions: String = ""
) = osascript(ExecReturner, script, args, functions)

fun <R> osascript(
  shell: Shell<R>,
  script: String,
  args: Array<String> = arrayOf(),
  functions: String = ""
): R {
  val realScript = wrapOsascript(script = script, functions = functions)
  return shell.osascriptE(realScript, *args)
}

fun <R> Shell<R>.osascriptE(script: String, vararg args: String): R = sendCommand("osascript", "-e", script, *args)

private fun wrapOsascript(script: String, functions: String = ""): String {
  var realScript = "on run argv\n$script\nend run"
  if (functions.isNotBlank()) {
	realScript = realScript + "\n\n" + functions
  }
  return realScript
}

fun interactiveOsascript(script: String): Pair<BufferedWriter, Process> {
  val p = proc(null, "bash", "-c", "osascript 3<&0 <<'APPLESCRIPT'")
  val writer = p.outputStream.bufferedWriter()
  writer.write("$script\nAPPLESCRIPT")
  return writer to p
}

@DslMarker annotation class AppleScriptDSL

@AppleScriptDSL
sealed class AppleScriptElement {
  val script get() = scriptLines.joinToString(separator = "\n")
  val scriptLines = mutableListOf<String>()
  inline fun <reified A: AppleScriptElement> A.repeat(times: Int, op: A.()->Unit) {
	scriptLines += "repeat $times times"
	scriptLines += A::class.createInstance().let {
	  it.op()
	  it.scriptLines
	}
	scriptLines += "end repeat"
  }
  inline fun <reified A: AppleScriptElement> A.repeatWhile(condition: String, op: A.()->Unit) {
	scriptLines += "repeat while $condition"
	scriptLines += A::class.createInstance().let {
	  it.op()
	  it.scriptLines
	}
	scriptLines += "end repeat"
  }

  fun delay(secs: Number) {
	scriptLines += "delay $secs"
  }
  fun doShellScript(script: String) {
	scriptLines += "do shell script \"$script\""
  }
}

@AppleScriptDSL
class AppleScript(op: AppleScript.()->Unit): AppleScriptElement() {
  inline fun <reified A: AppleScriptApplication> tell(op: A.()->Unit) {
	val app = A::class.createInstance()
	scriptLines += "tell application \"${app.name}\""
	app.op()
	scriptLines += app.scriptLines
	scriptLines += "end tell"
  }

  init {
	op()
  }
}

@AppleScriptDSL
sealed class AppleScriptApplication(val name: String): AppleScriptElement() {
  fun activate() {
	scriptLines += "activate"
  }
}


@Serializable
sealed interface SpotifyURI {
  val id: String
}

@Serializable
class SpotifyPlaylistURI(override val id: String): SpotifyURI {

  init {
	require(':' !in id)
  }

  override fun toString(): String = "spotify:playlist:$id"
  override fun equals(other: Any?): Boolean {
	return other is SpotifyPlaylistURI && other.id == id
  }

  override fun hashCode(): Int = id.hashCode()
}

class Spotify: AppleScriptApplication("Spotify") {
  fun openLocation(location: URL) {
	scriptLines += "open location \"$location\""
  }

  fun playTrack(id: SpotifyURI, inContext: SpotifyURI? = null) {
	var line = "play track \"$id\""
	if (inContext != null) line += " in context $inContext\""
	scriptLines += line
  }

  val pause: Unit
	get() {
	  scriptLines += "pause"
	}
  val play: Unit
	get() {
	  scriptLines += "play"
	}
  val playpause: Unit
	get() {
	  scriptLines += "playpause"
	}
  val nextTrack: Unit
	get() {
	  scriptLines += "next track"
	}
  val previousTrack: Unit
	get() {
	  scriptLines += "previous track"
	}
  var shuffling: Boolean?
	get() {
	  scriptLines += "log shuffling as text" /*GOES TO ERR!*/
	  return null
	}
	set(value) {
	  scriptLines += "set shuffling to $value"
	}
  var repeating: Boolean?
	get() {
	  scriptLines += "log repeating as text" /*GOES TO ERR!*/
	  return null
	}
	set(value) {
	  scriptLines += "set repeating to $value"
	}
}


enum class PlayerState { stopped, playing, paused }


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