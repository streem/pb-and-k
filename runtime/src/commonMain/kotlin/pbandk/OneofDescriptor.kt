package pbandk

import pbandk.gen.GeneratedOneOf
import pbandk.wkt.OneofOptions
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

public class OneofDescriptor<M : Message, O : Message.OneOf<*>> private constructor(
    getMessageDescriptor: () -> MessageDescriptor<M>,
    @ExperimentalProtoReflection
    public val name: String,
    internal val getValue: (M) -> O?,
    internal val setValue: (MutableMessage<M>, O?) -> Unit,
    @ExperimentalProtoReflection
    public val fields: FieldDescriptorSet<M>,
    // TODO: make this public once we're actually populating it correctly in the CodeGenerator
    internal val options: OneofOptions = OneofOptions.defaultInstance
) {
    // At the time that the [OneofDescriptor] constructor is called, the parent [MessageDescriptor] has not been
    // constructed yet. This is because this [OneofDescriptor] is one of the parameters that will be passed to the
    // [MessageDescriptor] constructor. To avoid the circular dependency, this property is declared lazy.
    internal val messageDescriptor: MessageDescriptor<M> by lazy(LazyThreadSafetyMode.PUBLICATION) { getMessageDescriptor() }

    internal fun mergeValues(message: M, otherMessage: M, destination: MutableMessage<M>) {
        val otherValue = getValue(otherMessage) ?: return
        val value = getValue(message)

        if (value == null || value::class != otherValue::class) {
            setValue(destination, otherValue)
        } else {
            @Suppress("UNCHECKED_CAST")
            val fd = (value as GeneratedOneOf<M, *>).fieldDescriptor
            fd.mergeValues(message, otherMessage, destination)
        }
    }

    public companion object {
        @PublicForGeneratedCode
        @Suppress("UNCHECKED_CAST")
        public fun <M : Message, MM : MutableMessage<M>, O : Message.OneOf<*>> of(
            messageDescriptor: KProperty0<MessageDescriptor<M>>,
            name: String,
            value: KProperty1<M, O?>,
            mutableValue: KMutableProperty1<MM, O?>,
            fields: Collection<FieldDescriptor<M, *>>,
            options: OneofOptions = OneofOptions.defaultInstance
        ): OneofDescriptor<M, O> = OneofDescriptor(
            getMessageDescriptor = messageDescriptor::get,
            name = name,
            getValue = value::get,
            setValue = mutableValue::set as (MutableMessage<M>, O?) -> Unit,
            fields = FieldDescriptorSet(fields),
            options = options
        )
    }
}