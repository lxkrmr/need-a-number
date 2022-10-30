package foo.bar.needanumber

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.random.Random

@RestController
class NeedANumberController {

    @GetMapping("/need-a-number")
    fun needANumber(): Int = Random.nextInt(1, 11)

    @GetMapping("/need-a-number1")
    fun needANumber1(): Int = Thread.sleep(1_000L).let { Random.nextInt(1, 11) }
}
