package pbandk.internal.types.primitive

import pbandk.InvalidProtocolBufferException
import pbandk.binary.BinaryFieldValueDecoder
import pbandk.json.JsonFieldValueEncoder
import pbandk.binary.BinaryFieldValueEncoder
import pbandk.internal.binary.Sizer
import pbandk.binary.WireType
import pbandk.internal.binary.WireValue
import pbandk.json.JsonFieldValueDecoder

internal object SFixed64 : PrimitiveValueType<Long>() {
    override val defaultValue: Long = 0L

    override fun isDefaultValue(value: Long) = value == 0L

    override val binaryWireType = WireType.FIXED64

    override fun binarySize(value: Long) = Sizer.sFixed64Size(value)

    override fun encodeToBinary(value: Long, encoder: BinaryFieldValueEncoder) {
        encoder.encodeI64(WireValue.I64.encodeSignedLong(value))
    }

    override fun decodeFromBinary(decoder: BinaryFieldValueDecoder): Long {
        return decoder.decodeI64().decodeSignedLong
    }

    override fun encodeToJson(value: Long, encoder: JsonFieldValueEncoder) {
        encoder.encodeNumberSignedLong(value)
    }

    override fun encodeToJsonMapKey(value: Long) = value.toString()

    override fun decodeFromJson(decoder: JsonFieldValueDecoder): Long = when (decoder) {
        is JsonFieldValueDecoder.Number -> decoder.decodeAsSignedLong()
        is JsonFieldValueDecoder.String -> decoder.decodeAsSignedLong()
        else -> throw InvalidProtocolBufferException("Unexpected JSON type for sfixed64 value: ${decoder.wireType.name}")
    }

    override fun decodeFromJsonMapKey(decoder: JsonFieldValueDecoder.String) = decoder.decodeAsSignedLong()
}