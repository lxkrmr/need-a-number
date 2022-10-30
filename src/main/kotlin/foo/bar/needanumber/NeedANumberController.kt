package foo.bar.needanumber

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
class NeedANumberController {

    // this endpoint performs as bad as the Web MVC
    @GetMapping("/need-a-number")
    suspend fun needANumber(): Int = Random.nextInt(1, 11)

    // performs as bad as (blocking) need-a-number plus warning of thread starvation
    @GetMapping("/need-a-number1")
    suspend fun needANumber1(): Int = Thread.sleep(1_000L).let { Random.nextInt(1, 11) }

    @GetMapping("/need-a-number2")
    suspend fun needANumber2(): Int = withContext(Dispatchers.IO) {
        Thread.sleep(1_000L).let { Random.nextInt(1, 11) }
    }

    // works better than Web MVC
    @GetMapping("/need-a-number3")
    suspend fun needANumber3(): Int = delay(1_000L).let { Random.nextInt(1, 11) }

    // works as good as neednumber3, does it make a difference to use withContext?
    @GetMapping("/need-a-number4")
    suspend fun needANumber4(): Int = withContext(Dispatchers.IO) { delay(1_000L).let { Random.nextInt(1, 11) } }
}
