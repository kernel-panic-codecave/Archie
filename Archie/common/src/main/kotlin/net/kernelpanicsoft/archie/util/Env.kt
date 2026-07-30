package net.kernelpanicsoft.archie.util

import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import java.util.Optional
import java.util.function.Supplier

/**
 * Runs [client] on the physical client and [server] on a dedicated server, returning whichever
 * ran. Only the branch matching the current [Env] is ever class-loaded, so [client] can safely
 * reference client-only classes even when this is called from common code.
 */
inline fun <T> foldEnv(crossinline client: () -> T, crossinline server: () -> T): T = EnvExecutor.getEnvSpecific<T>({ Supplier {
	client()
}}, { Supplier {
	server()
}})

/**
 * Runs [client] and returns its result, only on the physical client; returns [Optional.empty]
 * on a dedicated server without ever class-loading [client].
 */
inline fun <T> onClient(crossinline client: () -> T): Optional<T> = EnvExecutor.getInEnv<T>(Env.CLIENT) { Supplier { client() } }

/**
 * Runs [server] and returns its result, only on a dedicated server; returns [Optional.empty]
 * on the physical client without ever class-loading [server].
 */
inline fun <T> onServer(crossinline server: () -> T): Optional<T> = EnvExecutor.getInEnv<T>(Env.SERVER) { Supplier { server() } }