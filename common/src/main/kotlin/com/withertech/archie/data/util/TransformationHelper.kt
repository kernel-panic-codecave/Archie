package com.withertech.archie.data.util

import com.google.gson.*
import com.mojang.math.Axis
import com.mojang.math.Transformation
import net.minecraft.util.Mth
import net.minecraft.util.StringRepresentable
import org.joml.*
import java.lang.Math
import java.lang.reflect.Type
import kotlin.math.acos

object TransformationHelper
{
	fun quatFromXYZ(xyz: Vector3f, degrees: Boolean): Quaternionf
	{
		return quatFromXYZ(xyz.x, xyz.y, xyz.z, degrees)
	}

	fun quatFromXYZ(xyz: FloatArray, degrees: Boolean): Quaternionf
	{
		return quatFromXYZ(xyz[0], xyz[1], xyz[2], degrees)
	}

	fun quatFromXYZ(x: Float, y: Float, z: Float, degrees: Boolean): Quaternionf
	{
		val conversionFactor = if (degrees) Math.PI.toFloat() / 180 else 1f
		return Quaternionf().rotationXYZ(x * conversionFactor, y * conversionFactor, z * conversionFactor)
	}

	fun makeQuaternion(values: FloatArray): Quaternionf
	{
		return Quaternionf(values[0], values[1], values[2], values[3])
	}

	fun lerp(from: Vector3f?, to: Vector3f?, progress: Float): Vector3f
	{
		val res = Vector3f(from)
		res.lerp(to, progress)
		return res
	}

	private const val THRESHOLD = 0.9995

	fun slerp(v0: Quaternionfc, v1: Quaternionfc, t: Float): Quaternionf
	{
		// From https://en.wikipedia.org/w/index.php?title=Slerp&oldid=928959428
		// License: CC BY-SA 3.0 https://creativecommons.org/licenses/by-sa/3.0/

		// Compute the cosine of the angle between the two vectors.
		// If the dot product is negative, slerp won't take
		// the shorter path. Note that v1 and -v1 are equivalent when
		// the negation is applied to all four components. Fix by
		// reversing one quaternion.

		var v1 = v1
		var dot = v0.x() * v1.x() + v0.y() * v1.y() + v0.z() * v1.z() + v0.w() * v1.w()
		if (dot < 0.0f)
		{
			v1 = Quaternionf(-v1.x(), -v1.y(), -v1.z(), -v1.w())
			dot = -dot
		}

		// If the inputs are too close for comfort, linearly interpolate
		// and normalize the result.
		if (dot > THRESHOLD)
		{
			val x = Mth.lerp(t, v0.x(), v1.x())
			val y = Mth.lerp(t, v0.y(), v1.y())
			val z = Mth.lerp(t, v0.z(), v1.z())
			val w = Mth.lerp(t, v0.w(), v1.w())
			return Quaternionf(x, y, z, w)
		}

		// Since dot is in range [0, DOT_THRESHOLD], acos is safe
		val angle01 = acos(dot.toDouble()).toFloat()
		val angle0t = angle01 * t
		val sin0t = Mth.sin(angle0t)
		val sin01 = Mth.sin(angle01)
		val sin1t = Mth.sin(angle01 - angle0t)

		val s1 = sin0t / sin01
		val s0 = sin1t / sin01

		return Quaternionf(
			s0 * v0.x() + s1 * v1.x(),
			s0 * v0.y() + s1 * v1.y(),
			s0 * v0.z() + s1 * v1.z(),
			s0 * v0.w() + s1 * v1.w()
		)
	}

	fun slerp(one: Transformation, that: Transformation, progress: Float): Transformation
	{
		return Transformation(
			lerp(one.translation, that.translation, progress),
			slerp(one.leftRotation, that.leftRotation, progress),
			lerp(one.scale, that.scale, progress),
			slerp(one.rightRotation, that.rightRotation, progress)
		)
	}

	fun epsilonEquals(v1: Vector4f, v2: Vector4f, epsilon: Float): Boolean
	{
		return Mth.abs(v1.x() - v2.x()) < epsilon && Mth.abs(v1.y() - v2.y()) < epsilon && Mth.abs(
			v1.z() - v2.z()
		) < epsilon && Mth.abs(v1.w() - v2.w()) < epsilon
	}

	class Deserializer : JsonDeserializer<Transformation>
	{
		@Throws(JsonParseException::class)
		override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Transformation
		{
			if (json.isJsonPrimitive && json.asJsonPrimitive.isString)
			{
				val transform = json.asString
				if (transform == "identity")
				{
					return Transformation.identity()
				} else
				{
					throw JsonParseException("TRSR: unknown default string: $transform")
				}
			}
			if (json.isJsonArray)
			{
				// direct matrix array
				return Transformation(parseMatrix(json))
			}
			if (!json.isJsonObject) throw JsonParseException("TRSR: expected array or object, got: $json")
			val obj = json.asJsonObject
			val ret: Transformation
			if (obj.has("matrix"))
			{
				// matrix as a sole key
				ret = Transformation(parseMatrix(obj["matrix"]))
				if (obj.entrySet().size > 1)
				{
					throw JsonParseException("TRSR: can't combine matrix and other keys")
				}
				return ret
			}
			var translation: Vector3f? = null
			var leftRot: Quaternionf? = null
			var scale: Vector3f? = null
			var rightRot: Quaternionf? = null
			// TODO: Default origin is opposing corner, due to a mistake.
			// This should probably be replaced with center in future versions.
			var origin: Vector3f? =
				TransformOrigin.OPPOSING_CORNER.vector // TODO: Changing this to ORIGIN_CENTER breaks models, function content needs changing too -C
			val elements: MutableSet<String> = HashSet(obj.keySet())
			if (obj.has("translation"))
			{
				translation = Vector3f(parseFloatArray(obj["translation"], 3, "Translation"))
				elements.remove("translation")
			}
			if (obj.has("rotation"))
			{
				leftRot = parseRotation(obj["rotation"])
				elements.remove("rotation")
			} else if (obj.has("left_rotation"))
			{
				leftRot = parseRotation(obj["left_rotation"])
				elements.remove("left_rotation")
			}
			if (obj.has("scale"))
			{
				if (!obj["scale"].isJsonArray)
				{
					try
					{
						val s = obj["scale"].asNumber.toFloat()
						scale = Vector3f(s, s, s)
					} catch (ex: ClassCastException)
					{
						throw JsonParseException("TRSR scale: expected number or array, got: " + obj["scale"])
					}
				} else
				{
					scale = Vector3f(parseFloatArray(obj["scale"], 3, "Scale"))
				}
				elements.remove("scale")
			}
			if (obj.has("right_rotation"))
			{
				rightRot = parseRotation(obj["right_rotation"])
				elements.remove("right_rotation")
			} else if (obj.has("post-rotation"))
			{
				rightRot = parseRotation(obj["post-rotation"])
				elements.remove("post-rotation")
			}
			if (obj.has("origin"))
			{
				origin = parseOrigin(obj)
				elements.remove("origin")
			}
			if (!elements.isEmpty()) throw JsonParseException(
				"TRSR: can either have single 'matrix' key, or a combination of 'translation', 'rotation' OR 'left_rotation', 'scale', 'post-rotation' (legacy) OR 'right_rotation', 'origin'. Found: " + java.lang.String.join(
					", ",
					elements
				)
			)

			val matrix = Transformation(translation, leftRot, scale, rightRot)
			return matrix.applyOrigin(Vector3f(origin))
		}

		fun Transformation.isIdentity(): Boolean
		{
			return this@isIdentity == Transformation.identity()
		}

		fun Transformation.applyOrigin(origin: Vector3f): Transformation
		{
			val transform: Transformation = this@applyOrigin
			if (transform.isIdentity()) return Transformation.identity()

			val ret = transform.matrix
			val tmp = Matrix4f().translation(origin.x(), origin.y(), origin.z())
			tmp.mul(ret, ret)
			tmp.translation(-origin.x(), -origin.y(), -origin.z())
			ret.mul(tmp)
			return Transformation(ret)
		}

		companion object
		{
			private fun parseOrigin(obj: JsonObject): Vector3f?
			{
				var origin: Vector3f? = null

				// Two types supported: string ("center", "corner", "opposing-corner") and array ([x, y, z])
				val originElement = obj["origin"]
				if (originElement.isJsonArray)
				{
					origin = Vector3f(parseFloatArray(originElement, 3, "Origin"))
				} else if (originElement.isJsonPrimitive)
				{
					val originString = originElement.asString
					val originEnum = TransformOrigin.fromString(originString)
						?: throw JsonParseException("Origin: expected one of 'center', 'corner', 'opposing-corner'")
					origin = originEnum.vector
				} else
				{
					throw JsonParseException("Origin: expected an array or one of 'center', 'corner', 'opposing-corner'")
				}
				return origin
			}

			fun parseMatrix(e: JsonElement): Matrix4f
			{
				if (!e.isJsonArray) throw JsonParseException("Matrix: expected an array, got: $e")
				val m = e.asJsonArray
				if (m.size() != 3) throw JsonParseException("Matrix: expected an array of length 3, got: " + m.size())
				val matrix = Matrix4f()
				for (rowIdx in 0..2)
				{
					if (!m[rowIdx].isJsonArray) throw JsonParseException("Matrix row: expected an array, got: " + m[rowIdx])
					val r = m[rowIdx].asJsonArray
					if (r.size() != 4) throw JsonParseException("Matrix row: expected an array of length 4, got: " + r.size())
					for (columnIdx in 0..3)
					{
						try
						{
							matrix[columnIdx, rowIdx] = r[columnIdx].asNumber.toFloat()
						} catch (ex: ClassCastException)
						{
							throw JsonParseException("Matrix element: expected number, got: " + r[columnIdx])
						}
					}
				}
				// JOML's unsafe matrix component setter does not recalculate these properties, so the matrix would stay marked as identity
				matrix.determineProperties()
				return matrix
			}

			fun parseFloatArray(e: JsonElement, length: Int, prefix: String): FloatArray
			{
				if (!e.isJsonArray) throw JsonParseException("$prefix: expected an array, got: $e")
				val t = e.asJsonArray
				if (t.size() != length) throw JsonParseException(prefix + ": expected an array of length " + length + ", got: " + t.size())
				val ret = FloatArray(length)
				for (i in 0 until length)
				{
					try
					{
						ret[i] = t[i].asNumber.toFloat()
					} catch (ex: ClassCastException)
					{
						throw JsonParseException(prefix + " element: expected number, got: " + t[i])
					}
				}
				return ret
			}

			fun parseAxisRotation(e: JsonElement): Quaternionf
			{
				if (!e.isJsonObject) throw JsonParseException("Axis rotation: object expected, got: $e")
				val obj = e.asJsonObject
				if (obj.entrySet().size != 1) throw JsonParseException("Axis rotation: expected single axis object, got: $e")
				val entry = obj.entrySet().iterator().next()
				val ret: Quaternionf
				try
				{
					ret = if (entry.key == "x")
					{
						Axis.XP.rotationDegrees(entry.value.asNumber.toFloat())
					} else if (entry.key == "y")
					{
						Axis.YP.rotationDegrees(entry.value.asNumber.toFloat())
					} else if (entry.key == "z")
					{
						Axis.ZP.rotationDegrees(entry.value.asNumber.toFloat())
					} else throw JsonParseException("Axis rotation: expected single axis key, got: " + entry.key)
				} catch (ex: ClassCastException)
				{
					throw JsonParseException("Axis rotation value: expected number, got: " + entry.value)
				}
				return ret
			}

			fun parseRotation(e: JsonElement): Quaternionf
			{
				if (e.isJsonArray)
				{
					if (e.asJsonArray[0].isJsonObject)
					{
						val ret = Quaternionf()
						for (a in e.asJsonArray)
						{
							ret.mul(parseAxisRotation(a))
						}
						return ret
					} else if (e.isJsonArray)
					{
						val array = e.asJsonArray
						return if (array.size() == 3) //Vanilla rotation
							quatFromXYZ(parseFloatArray(e, 3, "Rotation"), true)
						else  // quaternion
							makeQuaternion(parseFloatArray(e, 4, "Rotation"))
					} else throw JsonParseException("Rotation: expected array or object, got: $e")
				} else if (e.isJsonObject)
				{
					return parseAxisRotation(e)
				} else throw JsonParseException("Rotation: expected array or object, got: $e")
			}
		}
	}

	enum class TransformOrigin(val vector: Vector3f, private val serialName: String) : StringRepresentable
	{
		CENTER(Vector3f(.5f, .5f, .5f), "center"),
		CORNER(Vector3f(), "corner"),
		OPPOSING_CORNER(Vector3f(1f, 1f, 1f), "opposing-corner");

		override fun getSerializedName(): String
		{
			return serialName
		}

		companion object
		{
			fun fromString(originName: String): TransformOrigin?
			{
				if (CENTER.serializedName == originName)
				{
					return CENTER
				}
				if (CORNER.serializedName == originName)
				{
					return CORNER
				}
				if (OPPOSING_CORNER.serializedName == originName)
				{
					return OPPOSING_CORNER
				}
				return null
			}
		}
	}
}
