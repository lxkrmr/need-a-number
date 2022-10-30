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
