package pbandk

import pbandk.testpb.Bar
import pbandk.testpb.Foo
import kotlin.test.Test
import kotlin.test.assertEquals

class CopyTest {
    @Test
    fun testCopyMethod() {
        val bar = Bar {
            foos = listOf(
                Foo { `val` = "hi" },
                Foo { `val` = "there" }
            )
            singleFoo = Foo { `val` = "bye" }
        }

        assertEquals(bar.foos, bar.copy(singleFoo = Foo { `val` = "test" }).foos)
        assertEquals(bar.singleFoo, bar.copy(foos = emptyList()).singleFoo)

        assertEquals(Foo { `val` = "test" }, bar.copy(singleFoo = Foo { `val` = "test" }).singleFoo)
    }
}