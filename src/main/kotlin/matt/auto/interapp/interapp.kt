@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package matt.auto.interapp

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import matt.auto.activateByPid
import matt.async.waitFor
import matt.key.ACTIVATE
import matt.key.ARE_YOU_RUNNING
import matt.key.EXIT
import matt.kjlib.socket.InterAppInterface
import matt.kjlib.socket.MY_INTER_APP_SEM
import matt.kjlib.socket.port
import matt.kjlib.socket.reader.SocketReader
import matt.kjlib.socket.reader.readTextBeforeTimeout
import matt.file.commons.VAL_JSON_FILE
import matt.klib.constants.ValJson
import matt.klib.lang.go
import matt.file.log.DefaultLogger
import matt.file.log.Logger
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.system.exitProcess


const val SLEEP_PERIOD = 100.toLong() //ms
const val PRINT_PERIOD = 10_000 //ms

@Suppress("unused")
val I_PERIOD = (PRINT_PERIOD/SLEEP_PERIOD).toInt()


fun ServerSocket.acceptOrTimeout(): Socket? {
  return try {
	accept()
  } catch (e: SocketTimeoutException) {
	null
  }
}

fun readSocketLines(
  port: Int,
  delayMS: Long = 100
) = flow {
  tryCreatingSocket(port).use { server ->
	server.soTimeout = delayMS.toInt()
	server.use {
	  while (!server.isClosed) {
		server.acceptOrTimeout()?.go { client ->
		  println("getting sReader")
		  val sReader = SocketReader(client)
		  println("reading lines")
		  do {
			println("reading line")
			val line = sReader.readLineOrSuspend(delayMS)
			//			println("got line: $line")
			if (line != null) {
			  emit(line)
			  println("emitted line")
			}

		  } while (line != null)

		  println("out of readSocketLines loop")
		  server.close()
		  //		  emitAll(
		  //			client.bufferedReader().lineFlow(delayMS = delayMS)
		  //		  )
		  //		  println("emitted all")
		} ?: delay(delayMS)
	  }
	}
  }
}

fun tryCreatingSocket(port: Int) = try {
  print("serving $port ...")
  ServerSocket(port).apply {
	println("started")
  }
} catch (e: BindException) {
  println("")
  println("port was $port")
  print("checking lsof...")
  val s = ProcessBuilder(
	"bash", "lsof -t -i tcp:${port}"
  ).start().let { it.inputStream.bufferedReader().readText() + it.errorStream.bufferedReader().readText() }
  println(" $s")
  e.printStackTrace()
  exitProcess(1)
}


class InterAppListener(
  prt: Int,
  val actions: Map<String, (String)->Unit>,
  val continueOp: InterAppListener.()->Boolean = { true },
  val suspendingRead: Boolean = true,
  private val log: Logger = DefaultLogger
) {
  constructor(name: String, actions: Map<String, (String)->Unit>): this(port(name), actions)


  private val _elementList = mutableListOf<String>()

  val elementList: List<String>
	get() = _elementList

  val elementList2: List<String> = mutableListOf()

  val serverSocket = tryCreatingSocket(prt)

  fun coreLoop() {
	log += "starting coreLoop"
	var continueRunning = true
	val debugAllSocksPleaseDontClose = mutableListOf<Socket>()

	/*necessary so the serverSocket.accept() doesn't block forever, which causes apps and the idea plugin to hang*/
	serverSocket.soTimeout = 100

	serverSocket.use {
	  log += "using serverSocket"
	  while (continueRunning && continueOp()) {
		val clientSocket = try {
		  serverSocket.accept()
		} catch (e: SocketTimeoutException) {
		  continue
		}
		debugAllSocksPleaseDontClose.add(clientSocket)
		val out = clientSocket.getOutputStream()
		log += ("SOCKET_CHANNEL=${clientSocket.channel}")
		MY_INTER_APP_SEM.acquire()
		log += "got sem"
		val signal = clientSocket.readTextBeforeTimeout(2000, suspend = suspendingRead).trim()
		if (signal.isBlank()) {
		  log += ("signal is blank...")
		}
		if (signal.isNotBlank()) {
		  log += ("signal: $signal")
		  when (signal) {
			EXIT     -> {
			  log += ("got quit signal")
			  continueRunning = false
			  clientSocket.close()
			  debugAllSocksPleaseDontClose.remove(clientSocket)
			}

			ACTIVATE -> {
			  log += ("got activate signal")
			  val pid = ProcessHandle.current().pid()
			  activateByPid(pid)
			  clientSocket.close()
			  debugAllSocksPleaseDontClose.remove(clientSocket)
			}

			"HERE!"  -> Unit
			else     -> {
			  val key = signal.substringBefore(":")
			  val value = signal.substringAfter(":")
			  log += ("other signal (length=${signal.length}) (key=$key,value=$value)")
			  if (key == ARE_YOU_RUNNING) {

				out.write("Here!\r\n".encodeToByteArray())
				out.flush()

				log += ("told them that im here!")
			  } else {
				val action = actions.entries.firstOrNull { it.key == key }
				if (action == null) {
				  log += ("found no action with key \"$key\"")
				} else {
				  log += ("found action with key \"$key\". executing.")
				  action.value(value)
				}
			  }
			}
		  }
		}
		MY_INTER_APP_SEM.release()
	  }
	  log += ("out of while loop, closing server socket")
	}
	log += ("Out of while loop, exiting")
  }
}


fun activateThisProcess() = activateByPid(ProcessHandle.current().pid())

@Suppress("unused")
private class Request(
  val returnAddress: String, val key: String
)



@Suppress("unused")
fun waitFor(service: String, me: String) {
  println("waiting for ${service}...")
  waitFor { InterAppInterface[service].areYouRunning(me) != null }
  println("for response from ${service}! moving on")
}
