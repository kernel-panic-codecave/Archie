package net.kernelpanicsoft.archie.util

import dev.architectury.platform.Platform
import dev.architectury.utils.Env
import dev.architectury.utils.EnvExecutor
import dev.architectury.utils.GameInstance
import net.minecraft.client.Minecraft
import net.minecraft.server.MinecraftServer
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

inline val isClient: Boolean get() = Platform.getEnvironment() == Env.CLIENT
inline val isServer: Boolean get() = Platform.getEnvironment() == Env.SERVER

/**
 * The running [Minecraft], or `null` where there is none.
 *
 * Nullable because it genuinely is: there are two ordinary situations with no client behind them -
 * a dedicated server, and a **data run**, which loads mods on the client distribution and never
 * constructs a game.
 *
 * That it was ever declared otherwise is an accident of annotation, not a judgement about
 * reliability. `GameInstance.getServer()` carries `@Nullable`, so [minecraftServer] had no choice
 * but to be honest; `getClient()` carries nothing, so it arrives as a platform type that Kotlin
 * will let you call whatever you like - and calling it non-null laundered the `null` into a type
 * promising it could not happen. The two are equally absent, equally often.
 *
 * Code that only ever runs while a client is drawing wants [requireMinecraftClient]; code that
 * merely *might* be on a client wants [withClientInstance].
 */
inline val minecraftClient: Minecraft? get() = GameInstance.getClient()

/**
 * The running [Minecraft], for code that cannot execute without one - drawing a widget, handling a
 * click, reading the options behind a tooltip.
 *
 * Throws if there is none, and says so: an assertion naming the situation beats the bare
 * `NullPointerException` from somewhere further down that a laundered platform type produces. It is
 * the deliberate choice of the two, which is the point of it having a name.
 */
inline val requireMinecraftClient: Minecraft
	get() = requireNotNull(GameInstance.getClient()) {
		"No Minecraft instance - this is a dedicated server or a data run. Use withClientInstance for work that may run without a client."
	}

/**
 * Whether there is a real [Minecraft] behind this physical client yet - see [withMinecraftClient],
 * which is how you should almost always spend this.
 */
inline val hasMinecraftClient: Boolean get() = GameInstance.getClient() != null

/**
 * Runs [block] against the running [Minecraft], on the physical client and only once there is a
 * game to run it against - see [withMinecraftServer] for the server-side counterpart.
 *
 * The distinction [onClient] does not draw: that one answers "is this the client distribution",
 * and a **data run** is exactly that distribution with no game behind it - it loads mods to collect
 * their providers and never constructs a [Minecraft]. So an `onClient` block *does* execute in a
 * data run, and anything in it reaching for [minecraftClient], or for a registry that resolves
 * through one, finds `null` and throws straight out of mod construction - killing the run before a
 * single provider executes, for every mod in the load.
 *
 * Handing the instance over as a receiver is what makes that impossible to get wrong: there is no
 * way to write the body without having gone through the check first.
 *
 * The two guards answer different questions and both are load-bearing. [onClient] is about the
 * *distribution*: [Minecraft] is not in a dedicated server jar, so naming it there fails to link at
 * all. The null check is about the *instance*, and is what matters everywhere the class does load -
 * which includes a data run, and includes server-side code in single player, where the integrated
 * server runs inside the client's own JVM and can reach this perfectly well.
 */
inline fun withMinecraftClient(crossinline block: Minecraft.() -> Unit) {
	onClient { GameInstance.getClient()?.block() }
}

inline val minecraftServer: MinecraftServer? get() = GameInstance.getServer()

/**
 * The running [MinecraftServer], for code that cannot execute without one - a block entity tick, a
 * command, anything reached from a `ServerLevel`.
 *
 * [requireMinecraftClient]'s counterpart, and the same trade: an assertion naming the situation,
 * rather than a `NullPointerException` from wherever the value was first dereferenced. Reach for
 * [minecraftServer] itself where its absence is an ordinary answer - a client that has not opened a
 * world has no server, and that is not an error.
 */
inline val requireMinecraftServer: MinecraftServer
	get() = requireNotNull(GameInstance.getServer()) {
		"No MinecraftServer - no world is running. Use minecraftServer where its absence is a legitimate answer."
	}

/**
 * Whether a [MinecraftServer] is running - integrated or dedicated alike, since a single-player
 * client has one just as truly as a server jar does. See [withMinecraftServer], which is how you
 * should almost always spend this.
 */
inline val hasMinecraftServer: Boolean get() = GameInstance.getServer() != null

/**
 * Runs [block] against the running [MinecraftServer], if there is one.
 *
 * [withMinecraftClient]'s counterpart, with one deliberate difference: no [onServer] around it.
 * That would be wrong rather than merely redundant - [onServer] means "this is the *dedicated
 * server distribution*", and a single-player client is running a perfectly real integrated server
 * that such a guard would skip.
 *
 * The client one carries an [onClient] for a reason that has no counterpart here, and it is about
 * which jar is loaded rather than which logical side is running: [Minecraft] does not exist on a
 * dedicated server distribution, so merely naming it there fails to link. [MinecraftServer] ships
 * on both, so there is nothing to keep out and a null check is the whole of what is needed.
 */
inline fun withMinecraftServer(crossinline block: MinecraftServer.() -> Unit) {
	GameInstance.getServer()?.block()
}