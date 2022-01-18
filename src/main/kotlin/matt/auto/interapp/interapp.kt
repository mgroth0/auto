package matt.auto.interapp

import matt.json.lang.get
import matt.json.prim.parseJson
import matt.kjlib.byte.readWithTimeout
import matt.kjlib.commons.VAL_JSON
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.Socket
import java.util.concurrent.Semaphore


fun port(name: String): Int {
  val e = VAL_JSON.parseJson()
  val map: Map<String, Number> = e["PORT"]!!

  return map[name]!!.toInt()


}

val MY_INTER_APP_SEM = Semaphore(1)

class Sender(
  val key: String
) {
  val sem = MY_INTER_APP_SEM
  fun send(message: String, use_sem: Boolean = true): String? { // return channel
	val response: String?
	try {


	  val kkSocket = Socket("localhost", port(key))
	  val out = PrintWriter(kkSocket.getOutputStream(), true)
	  val inReader = BufferedReader(
		InputStreamReader(kkSocket.getInputStream())
	  )
	  if (use_sem) sem.acquire()
	  /*out.println(message)*/
	  out.print(message.trim())
	  response = inReader.readWithTimeout(2000)
	  kkSocket.close()
	} catch (e: ConnectException) {
	  println(e.message)
	  if (use_sem) sem.release()
	  return null
	}
	if (use_sem) sem.release()
	return if (response == "") {
	  println("recieved no responsed")
	  null
	} else {
	  println(
		"recieved response:${response}"
	  )
	  response
	}
  }

  fun send(pair: Pair<String, String>): String? { // return channel
	return send("${pair.first}:${pair.second}")
  }

  fun receive(message: String) = send(message)

  @Suppress("unused")
  fun activate() = send("ACTIVATE")


  fun are_you_running(name: String): String? {
	return receive("ARE_YOU_RUNNING:${name}")
  }

  @Suppress("unused")
  fun exit() = send("EXIT")
  fun go(value: String) = send("GO" to value)
  fun open(value: String) = send("OPEN" to value)
}

object InterAppInterface {
  private val senders = mutableMapOf<String, Sender>()
  operator fun get(value: String): Sender {
	return if (senders.keys.contains(value)) {
	  senders[value]!!
	} else {
	  senders[value] = Sender(value)
	  senders[value]!!
	}

  }
}

fun Sender.open(file: File) = open(file.absolutePath)


fun File.openWithPDF() = InterAppInterface["PDF"].open(this)

class NoServerResponseException(servername: String): Exception() {
  override val message = "No response from server: $servername"
}
