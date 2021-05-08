package matt.auto

import matt.auto.interapp.InterAppInterface
import matt.kjlib.commons.ROOT_FOLDER
import java.io.File
import java.net.URL
import java.util.Base64
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