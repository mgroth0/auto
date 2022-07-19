package matt.auto

import matt.auto.applescript.AppleScript
import matt.auto.applescript.AppleScriptApplication
import matt.auto.applescript.applescript
import matt.auto.applescript.osascript
import matt.auto.applescript.runAppleScript
import matt.file.CodeFile
import matt.file.DataFile
import matt.file.Folder
import matt.file.LogFile
import matt.file.MFile
import matt.file.MarkupLanguageFile
import matt.file.TxtFile
import matt.file.UnknownFile
import matt.file.Zip
import matt.file.commons.APPLESCRIPT_FOLDER
import matt.file.commons.exceptionFolder
import matt.file.mFile
import matt.file.makeFileSeparatorsCompatibleWith
import matt.file.recursiveChildren
import matt.kjlib.shell.exec
import matt.kjlib.shell.shell
import matt.kjlib.socket.InterAppInterface
import matt.klib.commons.thisMachine
import matt.klib.commons.wrapWindowsBashCmd
import matt.klib.lang.inlined
import matt.klib.log.warn
import matt.klib.str.taball
import matt.klib.sys.NEW_MAC
import matt.klib.sys.Unix
import matt.klib.sys.Windows
import matt.klib.todo
import java.awt.Desktop
import java.net.URI
import java.net.URL
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance


class Action(val name: String, val icon: String? = null, val op: ()->Unit) {

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

/*must be lazy bc this can throw an error if process is headless*/
val desktop: Desktop by lazy { Desktop.getDesktop() }

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
  val aScript = APPLESCRIPT_FOLDER["$name.matt.auto.applescript.applescript"]
  if (!scpt.exists()) {
	println("COMPILE:" + shell("osacompile", "-o", scpt.absolutePath, aScript.absolutePath))
  }
  return shell("matt.auto.applescript.osascript", scpt.absolutePath, *args)
}

sealed class MacApp(val a: KClass<out AppleScriptApplication>) {
  fun activate() = runAppleScript {
	tell(a.createInstance()) {
	  activate()
	}
  }
}

object SublimeText: MacApp(matt.auto.applescript.SublimeText::class) {
  fun open(file: MFile) {
	val subl =
	  if (thisMachine == NEW_MAC) "/Applications/Sublime Text.app/Contents/SharedSupport/bin/subl" else "/usr/local/bin/subl"
	exec(null, subl, file.absolutePath)
  }
}


object Finder: MacApp(matt.auto.applescript.Finder::class) {
  fun open(f: MFile): Unit = Desktop.getDesktop().open(if (f.isDirectory) f else f.parentFile)
  fun open(f: String) = open(mFile(f))
}


class WebBrowser(a: KClass<out AppleScriptApplication>): MacApp(a) {
  fun open(u: URL) = exec(null, "open", "-a", a.createInstance().name, u.toString())
  fun open(u: String) = open(URI(u).toURL())
  fun open(f: MFile) = open(f.toURI().toURL())
}

val VIVALDI = WebBrowser(matt.auto.applescript.Vivaldi::class)
val CHROME = WebBrowser(matt.auto.applescript.Chrome::class)

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
  Action("open in finder", icon = "finder") {
	Finder.open(this)
  },
  Action("open in chrome") {
	CHROME.open(this)
  },
  Action("open in vivaldi") {
	VIVALDI.open(this)
  },
  Action("open with webd") {
	URL(this.toURI().toURL().toString()).open()
  },
)


fun MFile.open() {
  when (this) {
	is DataFile                                                -> if (binary) openInFinder() else openInSublime()
	is CodeFile, is LogFile, is TxtFile, is MarkupLanguageFile -> openInSublime()
	is UnknownFile, is Folder, is Zip                          -> openInFinder()
  }
}

fun MFile.moveToTrash() = desktop.moveToTrash(this)


/*what a SHAM. This can take over 10 times as long as cp*/
/*shadowJar.copyTo(dest, overwrite = true)*/
/*REMINDER: I am using mac cp, not gnu copy (which who knows, might be slower) so --target-directory isn't an option. there may be an equiavalent flag but i could not find it*/
fun MFile.copyToFast(target: MFile): String {
  todo("enforce having shell functions that are multiplatform")
  return when (thisMachine) {
	is Unix    -> target.parentFile!!.mkdirs().run { shell("cp", "-rf", absolutePath, target.absolutePath) }
	is Windows -> target.parentFile!!.mkdirs().run {
	  shell(
		*wrapWindowsBashCmd(
		  "cp", "-rf", absolutePath.makeFileSeparatorsCompatibleWith(NEW_MAC),
		  target.absolutePath.makeFileSeparatorsCompatibleWith(NEW_MAC)
		)
	  )
	}
  }
}


fun jumpToKotlinSourceString(
  rootProject: MFile,
  s: String,
  packageFilter: String?
): Pair<MFile, Int>? {
  println("matt.kjlib.jumpToKotlinSourceString:${s}:${packageFilter}")
  val packFolder = packageFilter?.replace(".", "/")
  var pair: Pair<MFile, Int>? = null
  inlined {
	rootProject["settings.gradle.kts"]
	  .readLines()
	  .asSequence()
	  .filterNot { it.isBlank() }
	  .map { it.trim() }
	  .filterNot { it.startsWith("//") }
	  .map { it.replace("include(\"", "").replace("\")", "") }
	  .map { it.replace(":", "/") }
	  .map { rootProject[it]["src"] }
	  .toList().forEach search@{ src ->
		println("searching source folder: $src")
		src.recursiveChildren()
		  .filter {
			(packageFilter == null || packFolder!! in it.absolutePath)
		  }
		  .filter { maybekt ->
			maybekt.extension == "kt"
		  }
		  .forEach kt@{ kt ->
			print("searching ${kt}... ")
			var linenum = 0 // I guess ide_open uses indices??
			kt.bufferedReader().lines().use { lines ->
			  for (line in lines) {
				if (s in line) {
				  println("found!")

				  pair = kt to linenum
				  return@inlined
				}
				linenum += 1

			  }
			}
			println("not here.")
		  }
	  }
  }
  println("matt.kjlib.jumpToKotlinSourceString: dur:${System.currentTimeMillis()}ms worked?: ${pair != null}")
  return pair
}


fun markTrashEmailsAsRead() {
  applescript(
	"""
	tell application "Mail"
		--	check for new mail
		--	return first item of messages of mailbox "Trash" of account "Google"
		set read status of messages of mailbox "Trash" of account "Google" to true
	end tell
  """.trimIndent()
  )
}