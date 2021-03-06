package matt.auto.exception

import matt.auto.SublimeText
import matt.auto.exception.MyDefaultUncaughtExceptionHandler.ExceptionResponse.EXIT
import matt.auto.exception.MyDefaultUncaughtExceptionHandler.ExceptionResponse.IGNORE
import matt.file.commons.LOG_FOLDER
import matt.file.MFile
import matt.klib.weak.MemReport
import java.lang.Thread.UncaughtExceptionHandler
import kotlin.random.Random.Default.nextDouble
import kotlin.system.exitProcess

val runtimeID = nextDouble()

class MyDefaultUncaughtExceptionHandler(
  val extraShutdownHook: ((
	t: Thread,
	e: Throwable,
	shutdown: (()->Unit)?,
	st: String,
	exception_file: MFile
  )->ExceptionResponse),
  val shutdown: (()->Unit)? = null,
): UncaughtExceptionHandler {

  var gotOne = false

  override fun uncaughtException(t: Thread?, e: Throwable?) {

	if (gotOne) {
	  println("wow, got an error in the error handler")
	  exitProcess(1)
	}
	gotOne = true

	e?.printStackTrace()

	/*dont delete until I find source of disappearing exceptions*/
	println("in uncaughtException for $e")

	val exceptionFolder = LOG_FOLDER["exceptions"]
	exceptionFolder.mkdirs()


	val exceptionFile = exceptionFolder.getNextSubIndexedFileWork("exception.txt", 100)()



	require(e != null) {
	  "I didn't know throwable could be null"
	}
	require(t != null) {
	  "I didn't know thread could be null"
	}
	println("got exception: ${e::class.simpleName}: ${e.message}")
	var ee = e
	while (ee!!.cause != null) {
	  println("caused by: ${ee.cause!!::class.simpleName}: ${ee.cause!!.message}")
	  ee = ee.cause
	}
	val st = e.stackTraceToString()

	val bugReport = "runtimeID:$runtimeID\n\n${MemReport()}\n\n${st}"

	e.printStackTrace()
	exceptionFile.text = bugReport
	println("trying to show exception dialog")
	val response = extraShutdownHook(t, e, shutdown, bugReport, exceptionFile)

	e.printStackTrace()

	// FIXME: exception_file.openInIntelliJ()
	SublimeText.open(exceptionFile)
	shutdown?.invoke()
	when (response) {
	  EXIT   -> {
		println("ok really exiting")
		exitProcess(1)
	  }

	  IGNORE -> {
		println("ignoring that exception")
	  }
	}
  }

  enum class ExceptionResponse { EXIT, IGNORE }
}
