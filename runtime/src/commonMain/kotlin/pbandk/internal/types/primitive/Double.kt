package pbandk.internal.types.primitive

import pbandk.InvalidProtocolBufferException
import pbandk.binary.BinaryFieldValueDecoder
import pbandk.binary.BinaryFieldValueEncoder
import pbandk.internal.binary.Sizer
import pbandk.binary.WireType
import pbandk.internal.binary.WireValue
import pbandk.json.JsonFieldValueDecoder
import pbandk.json.JsonFieldValueEncoder
import kotlin.Double

internal object Double : PrimitiveValueType<Double>() {
    override val defaultValue: Double = 0.0

    override fun isDefaultValue(value: Double) = value == 0.0

    override val binaryWireType = WireType.FIXED64

    override fun binarySize(value: Double) = Sizer.doubleSize(value)

    override fun encodeToBinary(value: Double, encoder: BinaryFieldValueEncoder) {
        encoder.encodeI64(WireValue.I64.encodeDouble(value))
    }

    override fun decodeFromBinary(decoder: BinaryFieldValueDecoder): Double {
        return decoder.decodeI64().decodeDouble
    }

    override fun encodeToJson(value: Double, encoder: JsonFieldValueEncoder) {
        encoder.encodeNumberDouble(value)
    }

    override fun encodeToJsonMapKey(value: Double) =
        throw UnsupportedOperationException("Double values cannot be used as map keys")

    override fun decodeFromJson(decoder: JsonFieldValueDecoder) = when (decoder) {
        is JsonFieldValueDecoder.String -> decoder.decodeAsDouble()
        is JsonFieldValueDecoder.Number -> decoder.decodeAsDouble()
        else -> throw InvalidProtocolBufferException("Unexpected JSON type for double value: ${decoder.wireType.name}")
    }

    override fun decodeFromJsonMapKey(decoder: JsonFieldValueDecoder.String) =
        throw UnsupportedOperationException("Double values cannot be used as map keys")
}