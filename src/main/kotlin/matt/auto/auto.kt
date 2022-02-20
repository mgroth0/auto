package matt.auto

import matt.auto.interapp.InterAppInterface
import matt.kjlib.commons.FLOW_FOLDER
import matt.kjlib.file.get
import matt.kjlib.shell.exec
import matt.kjlib.shell.execReturn
import matt.kjlib.shell.proc
import matt.reflect.isNewMac
import java.awt.Desktop
import java.io.BufferedWriter
import java.io.File
import java.net.URI
import java.net.URL
import java.util.Base64
import kotlin.concurrent.thread

fun IntelliJNavAction(file: String, linenum_or_searchstring: Any? = null): ProcessBuilder {
  val args = mutableListOf(
	FLOW_FOLDER.resolve("bin/ide_open").absolutePath, file
  )
  if (linenum_or_searchstring != null) {
	val encoded = Base64.getUrlEncoder().encodeToString(linenum_or_searchstring.toString().toByteArray())
	args[1] = args[1] + ":${encoded}"
  }
  return ProcessBuilder(
	args
  )
}

fun File.openInIntelliJ() = IntelliJNavAction(absolutePath).start()

fun URL.open() = InterAppInterface["webd"].open(this.toString())

val desktop: Desktop = Desktop.getDesktop()

@Suppress("unused")
fun kmscript(
  id: String, param: String? = null
) {
  val url = if (param == null) {
	"kmtrigger://macro=$id"
  } else {
	"kmtrigger://macro=$id&value=${URI(Base64.getEncoder().encodeToString(param.toByteArray()))}"
  }

  println("km url1: $url")
  val uri = URI(url)

  println("km url2: $uri")

  desktop.browse(uri)

  println("not sure what to do with nonblocking in kotlin")
}

val APPLESCRIPT_FOLDER = FLOW_FOLDER["applescript"].apply { mkdirs() }
fun compileAndOrRunApplescript(name: String, vararg args: String): String {
  val scpt = APPLESCRIPT_FOLDER["$name.scpt"]
  println(
	"WARNING: eventually use a .json to track compile time with modification times, for now need to manually delete .scpt files to update"
  )
  val aScript = APPLESCRIPT_FOLDER["$name.applescript"]
  if (!scpt.exists()) {
	println("COMPILE:" + execReturn(null, "osacompile", "-o", scpt.absolutePath, aScript.absolutePath))
  }
  return execReturn(null, "osascript", scpt.absolutePath, *args)
}

@Suppress("unused")
fun applescript(script: String, args: Array<String> = arrayOf(), compiled: Boolean = true) =
  osascript(script, args, compiled)

fun osascript(script: String, args: Array<String> = arrayOf(), compiled: Boolean = true): String {
  val realScript = "on run argv\n$script\nend run"
  if (compiled) {	/*var f = scptMap[realScript]
	if (f == null) {
	  val scpt = TEMP_DIR["scpt"].apply { mkdirs() }[]
	  exec(null,"osacompile","-e",realScript,"-o")
	}*/
	return execReturn(null, "osascript", "-e", realScript, *args)
  } else {
	return execReturn(null, "osascript", "-e", realScript, *args)
  }
}

fun interactiveOsascript(script: String, compiled: Boolean = true): Pair<BufferedWriter, Process> {
  if (compiled) {	/*var f = scptMap[realScript]
	if (f == null) {
	  val scpt = TEMP_DIR["scpt"].apply { mkdirs() }[]
	  exec(null,"osacompile","-e",realScript,"-o")
	}*/
	val p = proc(null, "bash", "-c", "osascript 3<&0 <<'APPLESCRIPT'")
	val writer = p.outputStream.bufferedWriter()
	writer.write("$script\nAPPLESCRIPT")
	return writer to p
  } else {
	val p = proc(null, "bash", "-c", "osascript 3<&0 <<'APPLESCRIPT'")
	val writer = p.outputStream.bufferedWriter()
	writer.write("$script\nAPPLESCRIPT")
	return writer to p
  }
}


object SublimeText {
  fun open(file: File) {
	val subl = if (isNewMac) "/Applications/Sublime Text.app/Contents/SharedSupport/bin/subl" else "/usr/local/bin/subl"
	exec(null, subl, file.absolutePath)
  }
}


object Finder {
  fun open(f: File) = Desktop.getDesktop().open(if (f.isDirectory) f else f.parentFile)
  fun open(f: String) = open(File(f))
}

class WebBrowser(val name: String) {
  fun open(u: URL) = exec(null, "open", "-a", name, u.toString())
  fun open(u: String) = open(URI(u).toURL())
  fun open(f: File) = open(f.toURI().toURL())
}

val VIVALDI = WebBrowser("Vivaldi")
val CHROME = WebBrowser("Chrome")

fun activateByPid(pid: Any) = thread {
  osascript(
	"""
        tell application "System Events"
            set frontmost of the first process whose unix id is $pid to true
        end tell
"""
  )
}