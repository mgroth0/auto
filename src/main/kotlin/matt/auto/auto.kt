package matt.auto

import matt.auto.interapp.InterAppInterface
import matt.kjlib.commons.ROOT_FOLDER
import matt.kjlib.shell.exec
import matt.kjlib.shell.execReturn
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URL
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
fun IntelliJNavAction(file: String, linenum_or_searchstring: Any? = null): ProcessBuilder {
  val args = mutableListOf(
	ROOT_FOLDER.resolve("bin/ide_open").absolutePath,
	file
  )
  if (linenum_or_searchstring != null) {
	val encoded = Base64.getUrlEncoder().encodeToString(linenum_or_searchstring.toString().toByteArray())
	args[1] = args[1] + ":${encoded}"
  }
  return ProcessBuilder(
	args
  )
}

@ExperimentalContracts
fun File.openInIntelliJ() = IntelliJNavAction(absolutePath).start()

fun URL.open() = InterAppInterface["webd"].open(this.toString())

val desktop: Desktop = Desktop.getDesktop()

@Suppress("unused")
fun kmscript(
  id: String,
  param: String? = null
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

@Suppress("unused")
fun applescript(script: String, nonblocking: Boolean = false) = osascript(script, nonblocking)
fun osascript(script: String, nonblocking: Boolean = false): String? {
  return if (nonblocking) {
	thread {
	  exec(null, "osascript", "-matt.kjlib.jmath.e", script)
	}
	null
  } else {
	execReturn(null, "osascript", "-matt.kjlib.jmath.e", script)
  }

}


object SublimeText {
  fun open(file: File) {
	exec(null, "/usr/local/bin/subl", file.absolutePath)
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

fun activateByPid(pid: Any) = osascript(
  """
        tell application "System Events"
            set frontmost of the first process whose unix id is $pid to true
        end tell
""",
  nonblocking = true
)