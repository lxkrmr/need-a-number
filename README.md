# KISS: blocking vs non-blocking

## quick start

If you don't (yet) want to read my blabla and instead prefer to tinker around (first), then do this:

* clone the repository to your local system  
* start the application  
   * I use Intellij because I just need to press the play button  
   * if you prefer the CLI then this known command is for you:
     * `./gradlew bootRun`  
* start artillery
   * artillery is a npm package that is going to do the requests for us
   * navigate into the artillery folder in your terminal
   * look into `simple-test-of-need-a-number.yaml` there you can change the endpoint and other stuff
   * run this command 
     * `./node_modules/.bin/artillery run simple-test-of-need-a-number.yaml`
   * I don't know much about artillery as I just care about a simple solution, but if you care more than go to:
     * https://www.artillery.io/docs

## blocking vs. non-blocking - who or what is blocked?

Just to make sure that we are on the same page - let us briefly define blocking.
Or even better let us role-play.

Imagine we are living in our application.  
We are a very crucial part, as we are threads.  
As threads, we are able to perform insanely well, and we love to get work done.
Sadly our number is limited.  
If we try to make too many threads, 
then the system that is hosting us will crash, 
and we will die as well.  
We don't like IO, 
because waiting for the DB or other web services is slow, 
and we have to idle around, 
waiting for the answer.  
A waste of our time.
So everytime the application needs to do some IO, 
we become idle and thus become blocked as we are still living in the "old" word.

In the old world we as threads or bounded directly to a request.
Which is nice, as the request can tell us details and we can store them for it.
But when to many requests are incoming,
then our limited number and our strong coupling to a request becomes a problem.
Because when our number is exceeded then we can't do shit,
choas will rise and if nothing changes then the system will go down.

So ... in the new world, 
we started with the fact that most often we as threads are idling a lot of time.
And so some creators came up with the idea that we could do just other work, if we start to idle.
The strict link between request and thread is now broken.

THE END?

## should we invest into non-blocking code?

Yes! Thanks for reading. Have a great day.  
Of course I am joking.  

This question came up recently in our team.  
And I know it already from other teams and other products.  
So why isn't the answer just a `Yes`?
Because writing, reading and debugging non-blocking code is an additional pain.  
Everytime I worked with Spring Webflux it felt like ... why ... noooo ...

So why isn't the answer just a `No`?
Because we care!  
We want that our applications are healthy and serve the customers as intended.
AND we switched to `Kotlin` where `coroutines` doing a far better job then this ugly `Webflux` stuff.

Therefor let us recheck to question!

## setting the mood

### let's KISS

#### * My meaning of KISS

I read KISS as Keep It Simple & Stupid. (not Keep It Simple, Stupid)  
I love this principle, because there are a lot of rabbit holes out there and all of them are calling to us.  
But not this time!

#### * The application

This time we are using a simple web application which is not special at all.     
In the beginning this application will be blocking and afterwards we will make it non-blocking.  

In this application we only have one controller.
Each endpoint will return a number between 1 and 10 (incl.)

There will be NO real IO!  
Meaning, no DB, no additional requests
Instead we will use `Thread.sleep` and `delay` to simulate IO.  
Why? Because!  

#### * But `<enter anything you think you know better>`

Most likely you are right, but I don't care - sorry.  
All I want to do is tinker around.  
And all of the stuff we are going to do, just make sense in comparison to each other.
So please don't get stuck by specific numbers.  
Instead let us use this numbers to compare the results.  
And if you still think that I missed something important, then please feel free to fork the project and create a PR.

## Action

Let's do it - finally.

### Old (blocking) world - Need a number - pure calculation

```Kotlin
    @GetMapping("/need-a-number")
    fun needANumber(): Int = Random.nextInt(1, 11)
```

The first endpoint is just returning a random number between 1 and 10.
There is no (simulated) IO.
Let us see how many request per second we can send with artillery,  
so that each request is still successfully.

```
http.codes.200: ................................................................ 27000
http.request_rate: ............................................................. 450/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 0
  max: ......................................................................... 200
  median: ...................................................................... 0
  p95: ......................................................................... 1
  p99: ......................................................................... 1
```

On my system - I was able to send 450 requests per second for 60 seconds to the application and all requests where successful.

### Old (blocking) world - Need a number - with simulated IO

```Kotlin
    @GetMapping("/need-a-number1")
    fun needANumber1(): Int = Thread.sleep(1_000L).let { Random.nextInt(1, 11) }
```

This time we simulated IO by sending the current thread to sleep for 1 seconds before returning the random number.

```
errors.ECONNRESET: ............................................................. 5965
errors.EPIPE: .................................................................. 342
errors.ETIMEDOUT: .............................................................. 17293
http.codes.200: ................................................................ 3400
http.request_rate: ............................................................. 394/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 1000
  max: ......................................................................... 9921
  median: ...................................................................... 5487.5
  p95: ......................................................................... 9801.2
  p99: ......................................................................... 9999.2
```

1 second can already make a real difference.  
Only 3_400 out of 27_000 requests were successful!  
In the real world we would now use something like K8s and scale our application to ensure that we can serve all requests.

### Switching to the new world

I prepared a new branch where I replaced the Spring Web dependency with Spring WebFlux  
and the NeedANumberController is now using Kotlin coroutines.  
Please make sure that you are switching the branch.

```
git checkout the-new-world
```

### New (non-blocking) world - Need a number - pure calculation

```Kotlin
    @GetMapping("/need-a-number")
    suspend fun needANumber(): Int = Random.nextInt(1, 11)
```

Let us go one step back and use artillery to check if something changed when we have an endpoint without any IO.  
In the controller we are now using the `suspend` keyword from Kotlin coroutines.  
Which is much nicer in my opinion then the Mono, Flux stuff we have to use in Java-Land.

```
http.codes.200: ................................................................ 27000
http.request_rate: ............................................................. 450/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 0
  max: ......................................................................... 145
  median: ...................................................................... 0
  p95: ......................................................................... 1
  p99: ......................................................................... 2
```

As we can see, **no difference** than in the old (blocking) world.  

```
learning #1:

Switching from blocking to non-blocking will not upgrade your resources like CPU, memory and Co.
Sounds obvious, but maybe it helps to know that the limitations of your system will stay the same.
We "only" change the way how our resources are used.
Therefor it can happen that it makes no difference.
We can't expect some magical improvment.

So if your current (blocking) application is fine (healthy and performant)   
then there is no need (yet) to switch worlds.
```

### New (non-blocking) world - Need a number - with simulated IO

Now let us see what we have to do, when we want to benefit from the non-blocking world.  
**Spoiler**: We will start with endpoints that look non-blocking but still suck.

```Kotlin
    // performs as bad as (blocking) need-a-number plus warning of thread starvation
    @GetMapping("/need-a-number1")
    suspend fun needANumber1(): Int = Thread.sleep(1_000L).let { Random.nextInt(1, 11) }
```

This endpoint looks similar to the one we already know from the blocking world.  
Again we are simulating IO by using a Thread.sleep.  
But if you use Intellij then you'll get a warning like this on the Thread.sleep() method:
```
Possibly blocking call in non-blocking context could lead to thread starvation
```

This means "you fucked up, this won't work and is dangerous".  
We will see what we have to do instead with the next endpoints.

```
errors.ECONNRESET: ............................................................. 322
errors.EPIPE: .................................................................. 2
errors.ETIMEDOUT: .............................................................. 26600
http.codes.200: ................................................................ 76
http.request_rate: ............................................................. 394/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 1000
  max: ......................................................................... 9764
  median: ...................................................................... 5826.9
  p95: ......................................................................... 9801.2
  p99: ......................................................................... 9801.2

```

No surprise, our results are awful.  
Only 76 out of 27_000 were successful.  
I don't think our customers will love it.

### New (non-blocking) world - Need a number - with simulated IO and fix from Intellij

Intellij is nice and provides a solution to this warning:
```
Possibly blocking call in non-blocking context could lead to thread starvation
```

```Kotlin
    @GetMapping("/need-a-number2")
    suspend fun needANumber2(): Int = withContext(Dispatchers.IO) {
        Thread.sleep(1_000L).let { Random.nextInt(1, 11) }
    }
```

Our endpoint is now using the `withContext`.
Let us check the results, before we discuss what is going on.

```
errors.EADDRNOTAVAIL: .......................................................... 2302
errors.ETIMEDOUT: .............................................................. 23906
http.codes.200: ................................................................ 792
http.request_rate: ............................................................. 394/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 1001
  max: ......................................................................... 9992
  median: ...................................................................... 6064.7
  p95: ......................................................................... 9801.2
  p99: ......................................................................... 9999.2
```

Better bad still awful.  
Only 792 requests out of 27_000 requests were successfully.  
But we did what Intellij told us, didn't we?  
Yes, but we are still doing one big mistake.  
We are mixing a non-blocking environment with still blocking code.  
Or in other words, `suspend` and `withContext` can't convert shit into gold.
The problem is the Thread.sleep as it still blocking the thread.
But our environment is expecting that we code our IO in a way that idle time will cause a suspension.

```
learning #2:

No keyword like "suspend" or "withContext" can transform blocking-code into non-blocking.
```

### New (non-blocking) world - Need a number - with simulated IO and real non-blocking code

```Kotlin
    @GetMapping("/need-a-number3")
    suspend fun needANumber3(): Int = delay(1_000L).let { Random.nextInt(1, 11) }
```

Our endpoint is now using `delay` instead of `Thread.sleep`.  
Let us check the results.  

```
http.codes.200: ................................................................ 27000
http.request_rate: ............................................................. 450/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 1000
  max: ......................................................................... 1271
  median: ...................................................................... 1002.4
  p95: ......................................................................... 1107.9
  p99: ......................................................................... 1130.2
```

Nice! 100% success (fyi: lucky run, sometimes a few requests like ~100 will still fail)  
Okay what is the difference between `delay` and `Thread.sleep`?  

`delay` is a suspendable function from Kotlin coroutines.  
So the code now says: 
```
Oh I have to wait for a second,  
please suspend me,  
the Thread is free for someone else,  
I will continue in a sec."
```

With `Thread.sleep` the code instead says:
```
Oh I habve to wait for a second,
but the Thread is mine.
Noone can use it in the meantime.
```

And this attitude of `Thread.sleep` becomes dangerous,  
because in a non-blocking world we assume that we need fewer threads overall as they can do other things  
as soon as we do (non-blocking) IO.
So in the end we request fewer threads, but still do blocking stuff and thus chaos is just around the corner.

```
learning #3:

If you want to do non-blocking, then make sure that all your code is non-blocking!
For example in Spring use WebClient instead of RestTemplate to talk with other services.
And also your DB should be connected in a non-blocking way, see https://r2dbc.io/

Otherwise your IO will act like Thread.sleep and
will still block threads which are now even more limited (as we though we need fewer).
```

### New (non-blocking) world - Need a number - with simulated IO and real non-blocking code - version 2

I also have this endpoint:

```Kotlin
    // works as good as need a number3, does it make a difference to use withContext?
    @GetMapping("/need-a-number4")
    suspend fun needANumber4(): Int = withContext(Dispatchers.IO) { delay(1_000L).let { Random.nextInt(1, 11) } }
```

At this point I'm also still learning and wondering if we should use `withContext`?  
I don't know yet.  

```
http.codes.200: ................................................................ 27000
http.request_rate: ............................................................. 450/sec
http.requests: ................................................................. 27000
http.response_time:
  min: ......................................................................... 992
  max: ......................................................................... 1143
  median: ...................................................................... 1002.4
  p95: ......................................................................... 1107.9
  p99: ......................................................................... 1130.2
```

Also, nice!

## Summary

* If your current (blocking) application is doing well, don't bother. But please proove it and not assume it ;)
* Kotlin coroutines are the nices way I see so far to implement non-blocking code.
* When you want to go non-blocking, then make sure that you go all the way (WebClient, IO)

## Things we missed

This simple example did not speak about
* doing things async
* error handling in coroutines
* testing with coroutines
* much much more

Thus this is just the beginning ...
BTW: Will project Loom solve our pain? https://wiki.openjdk.org/display/loom/Main
