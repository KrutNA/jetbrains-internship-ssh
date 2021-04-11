import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.system.exitProcess

const val EXPECTED_ARGUMENTS_ERROR_CODE: Int = 1
const val UNKNOWN_ARGUMENT_ERROR_CODE: Int = 2
const val PORT_PARSING_ERROR_CODE: Int = -1
const val PORT_OUT_OF_BOUNDS_ERROR_CODE: Int = -2
const val COULD_NOT_START_SERVER_ERROR_CODE: Int = 3
const val COULD_NOT_CONNECT_CLIENT_ERROR_CODE: Int = 4
const val UNKNOWN_HOST_ERROR_CODE: Int = 5
const val COULD_NOT_ESTABLISH_CONNECTION_ERROR_CODE = 6

const val MAX_PORT: Int = 65535

val MULTIPLIER: Double = 1.0 / sqrt(5.0)
val PHI: Double = (1.0 + sqrt(5.0)) / 2.0

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Expected arguments:\n" +
                "Start flag '--client'/'--server'\n" +
                "Client: host port\n" +
                "Server: port")
        exitProcess(EXPECTED_ARGUMENTS_ERROR_CODE)
    }

    when (args[0]) {
        "--server" -> startServer(args)?.let { exitProcess(it) }
        "--client" -> startClient(args)?.let { exitProcess(it) }
        else -> exitProcess(UNKNOWN_ARGUMENT_ERROR_CODE)
    }

    exitProcess(0)
}

fun readPort(port_str: String): Int {
    val port = port_str.toIntOrNull() ?: return  PORT_PARSING_ERROR_CODE
    // Could be user UShort for port instead of Int but it's experimental
    // In case of using UShort we doesn't need this check
    if (port < 0 || port > MAX_PORT) {
        return PORT_OUT_OF_BOUNDS_ERROR_CODE
    }
    return port
}

fun startClient(args: Array<String>): Int? {
    val host = args[1]
    val port = readPort(args[2])
    if (port < 0) {
        return port
    }

    val socket: Socket = try {
        Socket(host, port)
    } catch (e: UnknownHostException) {
        return UNKNOWN_HOST_ERROR_CODE
    } catch (e: IOException) {
        return COULD_NOT_CONNECT_CLIENT_ERROR_CODE
    }

    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    val writer = socket.getOutputStream()

    while (true) {
        val line = readLine() !!

        if (line.isBlank()) {
            socket.close()
            break
        }

        val valueUnchecked = line.toIntOrNull() ?: continue
        val value = if (valueUnchecked >= 0) {
            valueUnchecked
        } else {
            continue
        }

        writer.write(value)
        val result = reader.readLine()

        println(result)
    }

    return null
}

fun startServer(args: Array<String>): Int? {
    val port = readPort(args[1])
    if (port < 0) {
        return port
    }

    val socket: ServerSocket = try {
        ServerSocket(port)
    } catch (e: IOException) {
        return COULD_NOT_START_SERVER_ERROR_CODE
    }

    println("Server started!")
    while (true) {
        println("Waiting for connection.")
        val connection = try {
            socket.accept()
        } catch (e: IOException) {
            return COULD_NOT_ESTABLISH_CONNECTION_ERROR_CODE
        }
        println("Connection accepted.")

        Server(connection).run()
    }

    @Suppress("UNREACHABLE_CODE")
    return null
}

class Server(private val socket: Socket): Thread() {
    override fun run() {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer = socket.getOutputStream()

        while (!socket.isClosed) {
            val value = reader.read()

            // Could be used hashmap creation on sever startup for first 93 values
            // In case of most effective and accurate result
            // 93 because of size of 64-bit integer

            val sign = if (value % 2 == 0) { 1.0 } else { -1.0 }
            val phiPowered = PHI.pow(value.toDouble())

            val result = MULTIPLIER * (phiPowered - sign / phiPowered)
            writer.write("$result\n".toByteArray())
        }
    }
}