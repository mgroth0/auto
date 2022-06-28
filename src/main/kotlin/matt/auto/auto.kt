package matt.auto

import matt.kjlib.shell.exec
import matt.kjlib.shell.execReturn
import matt.kjlib.shell.proc
import matt.kjlib.socket.InterAppInterface
import matt.file.commons.APPLESCRIPT_FOLDER
import matt.file.commons.exceptionFolder
import matt.klib.commons.thisMachine
import matt.file.ApplescriptFile
import matt.file.CodeFile
import matt.file.DSStoreFile
import matt.file.DataFile
import matt.file.Folder
import matt.file.JsonFile
import matt.file.MFile
import matt.file.ShellFile
import matt.file.mFile
import matt.file.UnknownFile
import matt.klib.log.warn
import matt.klib.str.taball
import matt.klib.sys.NEW_MAC
import java.awt.Desktop
import java.io.BufferedWriter
import java.net.URI
import java.net.URL
import java.util.Base64
import kotlin.concurrent.thread


class Action(val name: String, val op: ()->Unit) {

}

class YesUsingAuto()

fun writeErrReport(name: String, report: String) {
  exceptionFolder.mkdirs()
  val file = exceptionFolder["$name.txt"]
  warn("writing error report to $file")
  file.writeText(report)
  SublimeText.open(file)
}

fun openInIntelliJ(file: String, linenum_or_searchstring: Any? = null) {
  var arg = file

  if (linenum_or_searchstring != null) {
	val encoded = Base64.getUrlEncoder().encodeToString(linenum_or_searchstring.toString().toByteArray())
	arg += ":${encoded}"
  }
  ideOpen(arg)
  /*  return ProcessBuilder(
	  args
	)*/
}

fun MFile.openInIntelliJ() = thread { openInIntelliJ(absolutePath) }
fun MFile.openInFinder(): Unit =
  if (this.isDirectory) desktop.browse(this.toURI()) else this.parentFile!!.openInFinder()

fun MFile.openInSublime() = SublimeText.open(this)
fun MFile.subl() = openInSublime()

@JvmName("subl1")
fun subl(file: MFile) = file.subl()

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
fun applescript(script: String, args: Array<String> = arrayOf(), compiled: Boolean = true, functions: String = "") =
  osascript(script, args, compiled, functions = functions)

fun osascript(
  script: String,
  args: Array<String> = arrayOf(),
  compiled: Boolean = true,
  functions: String = ""
): String {
  var realScript = "on run argv\n$script\nend run"
  if (functions.isNotBlank()) {
	realScript = realScript + "\n\n" + functions
  }
  if (compiled) {    /*var f = scptMap[realScript]
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
  if (compiled) {    /*var f = scptMap[realScript]
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
  fun open(file: MFile) {
	val subl =
	  if (thisMachine == NEW_MAC) "/Applications/Sublime Text.app/Contents/SharedSupport/bin/subl" else "/usr/local/bin/subl"
	exec(null, subl, file.absolutePath)
  }
}


object Finder {
  fun open(f: MFile): Unit = Desktop.getDesktop().open(if (f.isDirectory) f else f.parentFile)
  fun open(f: String) = open(mFile(f))
}

class WebBrowser(val name: String) {
  fun open(u: URL) = exec(null, "open", "-a", name, u.toString())
  fun open(u: String) = open(URI(u).toURL())
  fun open(f: MFile) = open(f.toURI().toURL())
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


fun ideOpen(weirdArg: String) {
  val pids =
	ProcessBuilder("/bin/bash", "-c", "/bin/ps -A | /usr/bin/grep idea | /usr/bin/awk \'{print $1}\'")
	  .start()
	  .inputStream
	  .bufferedReader()
	  .readText()
	  .lines()
	  .filter { it.isNotBlank() }
	  .map { it.toInt() }
  taball("pids", pids)
  pids.forEach {
	/*in old zsh script for some reason I only activated the first one or something. Not sure why.*/
	activateByPid(it)
  }
  InterAppInterface["ide"].open(weirdArg)
}

const val SUBL = "/Applications/Sublime Text.app/Contents/SharedSupport/bin/subl"


fun MFile.actions() = listOf(
  Action("open in intelliJ") {
	openInIntelliJ()
  },
  Action("open in Sublime") {
	openInSublime()
  },
  Action("open in finder") {
	matt.auto.Finder.open(this)
  },
  Action("open in chrome") {
	matt.auto.CHROME.open(this)
  },
  Action("open in vivaldi") {
	matt.auto.VIVALDI.open(this)
  },
  Action("open with webd") {
	java.net.URL(this.toURI().toURL().toString()).open()
  },
)


fun MFile.open() {
  when (this) {
	is JsonFile, is CodeFile                               -> openInSublime()
	is UnknownFile, is DSStoreFile, is Folder, is DataFile -> openInFinder()
  }
}