package pbandk.internal.types

import pbandk.FieldDescriptor
import pbandk.FieldDescriptorSet
import pbandk.InvalidProtocolBufferException
import pbandk.Message
import pbandk.UnknownField
import pbandk.gen.messageDescriptor
import pbandk.internal.binary.BinaryFieldDecoder
import pbandk.internal.binary.BinaryFieldEncoder
import pbandk.binary.BinaryFieldValueDecoder
import pbandk.binary.BinaryFieldValueEncoder
import pbandk.internal.binary.Sizer
import pbandk.internal.binary.Tag
import pbandk.binary.WireType
import pbandk.json.JsonFieldValueDecoder
import pbandk.json.JsonFieldValueEncoder
import pbandk.internal.types.wkt.WktValueType
import pbandk.internal.types.wkt.customJsonMappings
import pbandk.types.ValueType

internal fun <M : Message> FieldDescriptorSet<M>.findByJsonName(keyDecoder: JsonFieldValueDecoder.String): FieldDescriptor<M, *>? {
    val key = keyDecoder.decodeAsString()
    val fd = this.firstOrNull { key in listOf(it.jsonName, it.name) }
    if (fd == null && !keyDecoder.jsonConfig.ignoreUnknownFieldsInInput) {
        throw InvalidProtocolBufferException("Unknown field name and ignoreUnknownFieldsInInput=false: $key")
    }
    return fd
}

internal open class MessageValueType<M : Message>(val companion: Message.Companion<M>) : ValueType<M> {
    override val defaultValue get() = companion.defaultInstance

    override fun isDefaultValue(value: M) = false

    override fun mergeValues(currentValue: M, newValue: M): M {
        @Suppress("UNCHECKED_CAST")
        return currentValue.plus(newValue) as M
    }

    override val binaryWireType = WireType.LENGTH_DELIMITED

    override fun binarySize(value: M) = Sizer.messageSize(value)

    internal fun encodeToBinaryNoLength(value: M, fieldEncoder: BinaryFieldEncoder) {
        for (fieldDescriptor in value.messageDescriptor.fields) {
            fieldDescriptor.encodeToBinary(fieldEncoder, value)
        }
        for (field in value.unknownFields.values) {
            field.values.forEach {
                fieldEncoder.encodeField(Tag(field.fieldNum, WireType(it.wireType))) { valueEncoder ->
                    valueEncoder.encodeUnknownField(it.wireValue)
                }
            }
        }
    }

    override fun encodeToBinary(value: M, encoder: BinaryFieldValueEncoder) {
        encoder.encodeLenFields(value.protoSize) { fieldEncoder ->
            encodeToBinaryNoLength(value, fieldEncoder)
        }
    }

    internal fun decodeFromBinaryNoLength(fieldDecoder: BinaryFieldDecoder): M {
        return companion.descriptor.builder {
            val fieldDescriptors = companion.descriptor.fields
            do {
                val fieldFound = fieldDecoder.decodeField { tag, valueDecoder ->
                    val fieldNum = tag.fieldNumber
                    val wireType = tag.wireType
                    val fd = fieldDescriptors[fieldNum]

                    if (fd == null || !fd.fieldType.allowsBinaryWireType(wireType)) {
                        val unknownFieldValue = valueDecoder.decodeUnknownField(tag) ?: return@decodeField
                        unknownFields[fieldNum] = unknownFields[fieldNum]?.let { prevValue ->
                            // TODO: make parsing of repeated unknown fields more efficient by not creating a copy of
                            //  the list with each new element.
                            prevValue.copy(values = prevValue.values + unknownFieldValue)
                        } ?: UnknownField(fieldNum, listOf(unknownFieldValue))
                        return@decodeField
                    }

                    fd.decodeFromBinary(tag, valueDecoder, this)
                }
            } while (fieldFound)
        }
    }

    override fun decodeFromBinary(decoder: BinaryFieldValueDecoder): M {
        return decoder.decodeLenFields { fieldDecoder ->
            decodeFromBinaryNoLength(fieldDecoder)
        }
    }

    override fun decodeFromJson(decoder: JsonFieldValueDecoder): M {
        // TODO: do we need to check for WKT types that get encoded to strings instead of objects here?
        if (decoder !is JsonFieldValueDecoder.Object) {
            throw InvalidProtocolBufferException("Unexpected JSON type for message value: ${decoder.wireType.name}")
        }

        return decoder.decodeFields { fieldDecoder ->
            companion.descriptor.builder {
                val fieldDescriptors = companion.descriptor.fields
                fieldDecoder.forEachField(fieldDescriptors::findByJsonName) { fd, valueDecoder ->
                    if (fd != null) {
                        fd.decodeFromJson(valueDecoder, this)
                    } else {
                        valueDecoder.skipValue()
                    }
                }
            }
        }
    }

    override fun encodeToJson(value: M, encoder: JsonFieldValueEncoder) {
        @Suppress("UNCHECKED_CAST")
        val customValueType = customJsonMappings[value.messageDescriptor] as? WktValueType<*, M>?

        if (customValueType != null) {
            customValueType.encodeMessageToJson(value, encoder)
        } else {
            encoder.encodeObject { fieldEncoder ->
                value.messageDescriptor.fields.forEach { fieldDescriptor ->
                    fieldDescriptor.encodeToJson(fieldEncoder, value)
                }
            }
        }
    }

    override fun encodeToJsonMapKey(value: M) =
        throw UnsupportedOperationException("messages cannot be used as map keys")

    override fun decodeFromJsonMapKey(decoder: JsonFieldValueDecoder.String) =
        throw UnsupportedOperationException("messages cannot be used as map keys")
}