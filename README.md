# java_async_example
## Implementing an asynchronous API
To start implementing the best-price-finder application, let’s begin by defining the API that each
single shop should provide. First, a shop declares a method that returns the price of a product
given its name:
```console
public class Shop {
public double getPrice(String product) {
// to be implemented
}
}
```
The internal implementation of this method would query the shop’s database but probably also
perform other time-consuming tasks, such as contacting various other external services (for
example, the shop’s suppliers or manufacturer-related promotional discounts). To fake such a
long-running method execution, in the rest of this chapter we simply use a delay method, which
introduces an artificial delay of 1 second, defined in the following listing.

```console
public static void delay() {
try {
Thread.sleep(1000L);
} catch (InterruptedException e) {
throw new RuntimeException(e);
}
}
```
For the purpose of this chapter, the getPrice method can be modeled by calling delay and then
returning a randomly calculated value for the price, as shown in the next listing. The code for
returning a randomly calculated price may look like a bit of a hack. It randomizes the price
based on the product name by using the result of charAt as a number.

## Introducing a simulated delay in the getPrice method.
```console
public double getPrice(String product) {
return calculatePrice(product);
}
private double calculatePrice(String product) {
delay();
return random.nextDouble() * product.charAt(0) + product.charAt(1);
}
```
This implies that when the consumer of this API (in this case, the best-price-finder application)
invokes this method, it will remain blocked and then idle for 1 second while waiting for its
synchronous completion. This is unacceptable, especially considering that the best-price-finder
application will have to repeat this operation for all the shops in its network. In the subsequent
sections of this chapter, you’ll discover how you can resolve this problem by consuming this
synchronous API in an asynchronous way. But for the purpose of learning how to design an
asynchronous API, we continue this section by pretending to be on the other side of the
barricade: you’re a wise shop owner who realizes how painful this synchronous API is for its
users and you want to rewrite it as an asynchronous API to make your customers’ lives easier.

## Converting a synchronous method into an asynchronous one
To achieve this you first have to turn the getPrice method into a getPriceAsync method and
change its return value:
```console
public Future<Double> getPriceAsync(String product) { ... }
```
As we mentioned in the introduction of this chapter, the java.util.concurrent .Future interface
was introduced in Java 5 to represent the result of an asynchronous computation (that is, the
caller thread is allowed to proceed without blocking). This means a Future is just a handle for a
value that isn’t yet available but can be retrieved by invoking its get method after its
computation has finally terminated. As a result, the getPriceAsync method can return
immediately, giving the caller thread a chance to perform other useful computations in the
meantime. The new CompletableFuture class gives you various possibilities to implement this
method in an easy way, for example, as shown in the next listing.

## Implementing the getPriceAsync method
```console
 public Future<Double> getPriceAsync(String product) {
   CompletableFuture<Double> futurePrice = new CompletableFuture<>();
   new Thread(() -> {
   double price = calculatePrice(product);
   futurePrice.complete(price);
   }).start();
   return futurePrice;
   }
   ```
   Here you create an instance of CompletableFuture, representing an asynchronous computation
and containing a result when it becomes available. Then you fork a different Thread that will
perform the actual price calculation and return the Future instance without waiting for that
long-lasting calculation to terminate. When the price of the requested product is finally available,
you can complete the Completable-Future using its complete method to set the value. Obviously
this feature also explains the name of this new Future implementation. A client of this API can
invoke it, as shown in the next listing.

```console
@Test
  public void testSingleShop() {

    Shop shop = new Shop("my shop");
    long start = System.nanoTime();
    Future<Double> futurePrice = shop.getPriceAsync("product");
    long invocationTime = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Invocation return after " + invocationTime + " msec.");
    try {
      double price = futurePrice.get(5, TimeUnit.SECONDS);
      System.out.printf("Price is %.2f%n", price);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      e.printStackTrace();
    }
    long retrievalTime = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Price Returned after " + retrievalTime + " msec.");

  }
  ```
As you can see, the client asks the shop to get the price of a certain product. Because the shop
provides an asynchronous API, this invocation almost immediately returns the Future, through
which the client can retrieve the product’s price at a later time. This allows the client to do other
tasks, like querying other shops, instead of remaining blocked waiting for the first shop to
produce the requested result. Later, when there are no other meaningful jobs that the client
could do without having the product price, it can invoke get on the Future. By doing so the client
either unwraps the value contained in the Future (if the asynchronous task is already finished)
or remains blocked until that value is available. The output produced by the code in listing 11.5
could be something like this:
Invocation returned after 43 msecs
Price is 123.26
Price returned after 1045 msecs
You can see that the invocation of the getPriceAsync method returns far sooner than when the
price calculation eventually finishes. In section 11.4 you’ll learn that it’s also possible for the
client to avoid any risk of being blocked. Instead it can just be notified when the Future is
completed, and execute a callback code, defined through a lambda expression or a method
reference, only when the result of the computation is available. For now we’ll address another
problem: how to correctly manage the possibility of an error occurring during the execution of
the asynchronous task.

## Dealing with errors
The code we developed so far works correctly if everything goes smoothly. But what happens if
the price calculation generates an error? Unfortunately, in this case you’ll get a particularly
negative outcome: the exception raised to signal the error will remain confined in the thread,
which is trying to calculate the product price, and will ultimately kill it. As a consequence, the
client will remain blocked forever, waiting for the result of the get method to arrive.
The client can prevent this problem by using an overloaded version of the get method that also
accepts a timeout. It’s a good practice to always use a timeout to avoid similar situations
elsewhere in your code. This way the client will at least avoid waiting indefinitely, but when the
timeout expires, it will just be notified with a TimeoutException. As a consequence, it won’t have
a chance to discover what really caused that failure inside the thread that was trying to calculate
the product price. To make the client aware of the reason the shop wasn’t able to provide the
price of the requested product, you have to propagate the Exception that caused the problem
inside the CompletableFuture through its completeExceptionally method. This refines listing
11.4 to give the code shown in the listing that follows.

## Propagating an error inside the CompletableFuture
```console
 public Future<Double> getPriceAsync(String product) {
   CompletableFuture<Double> futurePrice = new CompletableFuture<>();
   new Thread(() -> {
   try {
   double price = calculatePrice(product);
   futurePrice.complete(price);
   } catch (Exception ex) {
   futurePrice.completeExceptionally(ex);
   }
  
   }).start();
   return futurePrice;
   }
   ```
   The client will now be notified with an ExecutionException (which takes an Exception
parameter containing the cause—the original Exception thrown by the price calculation method).
So, for example, if that method throws a RuntimeException saying “product not available,” the
client will get an ExecutionException like the following:
```
java.util.concurrent.ExecutionException: java.lang.RuntimeException: product not available
at java.util.concurrent.CompletableFuture.get(CompletableFuture.java:2237)
at lambdasinaction.chap11.AsyncShopClient.main(AsyncShopClient.java:14)
... 5 more
```

## Creating a CompletableFuture with the supplyAsync factory method
Until now you’ve created CompletableFutures and completed them programmatically, when it
seemed convenient to do so, but the CompletableFuture class itself comes with lots of handy
factory methods that can make this process far easier and less verbose. For example, the
supplyAsync method can let you rewrite the getPriceAsync method in listing 11.4 with a single
statement, as shown in the following listing.

## Creating a CompletableFuture with the supplyAsync factory
method
```console
public Future<Double> getPriceAsync(String product) {
return CompletableFuture.supplyAsync(() -> calculatePrice(product));
}
```
The supplyAsync method accepts a Supplier as argument and returns a Completable-Future that
will be asynchronously completed with the value obtained by invoking that Supplier. This
Supplier will be run by one of the Executors in the ForkJoinPool, but you can specify a different
Executor by passing it as a second argument to the overloaded version of this method. More
generally, it’s possible to optionally pass an Executor to all other CompletableFuture factory
methods, and you’ll use this capability in section 11.3.4, where we demonstrate that using an
Executor that fits the characteristics of your application can have a positive effect on its
performance.
Also note that the CompletableFuture returned by the getPriceAsync method in listing 11.7 is
totally equivalent to the one you created and completed manually in listing 11.6, meaning it
provides the same error management you carefully added.
For the rest of this chapter, we’ll suppose you sadly have no control over the API implemented
by the Shop class and that it provides only synchronous blocking methods. This is also what
typically happens when you want to consume an HTTP API provided by some service. You’ll
learn how it’s still possible to query multiple shops asynchronously, thus avoiding becoming blocked on a single request and thereby increasing the performance and the throughput of your
best-price-finder application.
## Make your code non-blocking
So you’ve been asked to develop a best-price-finder application, and all the shops you have to
query provide only the same synchronous API implemented as shown at the beginning of
section 11.2. In other words, you have a list of shops, like this one
```console
List<Shop> shops = Arrays.asList(new Shop("BestPrice"),
new Shop("LetsSaveBig"),
new Shop("MyFavoriteShop"),
new Shop("BuyItAll"));
```
You have to implement a method with the following signature, that given the name of a product
returns a List of strings, where each string contains the name of a shop and the price of the
requested product in that shop:
```console
public List<String> findPrices(String product);
```
Your first idea will probably be to use the Stream features you learned in chapters 4, 5, and 6. You may be tempted to write something like what’s shown in the next listing (yes, it’s good if
you’re already thinking this first solution is bad!).

## A findPrices implementation sequentially querying all the
shops
```console
public List<String> findPrices(String product) {
return shops.stream()
.map(shop -> String.format("%s price is %.2f",
shop.getName(), shop.getPrice(product)))
.collect(toList());
}
```
Okay, this was straightforward. Now try to put the method findPrices to work with the only
product you want madly these days (yes, you guessed it; it’s the myPhone27S). In addition,
record how long the method takes to run, as shown in the following listing; this will let you
compare its performance with the improved method we develop later.
## Checking findPrices correctness and performance
```console
long start = System.nanoTime();
System.out.println(findPrices("myPhone27S"));
long duration = (System.nanoTime() - start) / 1_000_000;
System.out.println("Done in " + duration + " msecs");
```
The code in listing 11.9 produces output like this:
```
[BestPrice price is 123.26, LetsSaveBig price is 169.47, MyFavoriteShop price is 214.13, BuyItAll
price is 184.74]
```
As you may have expected, the time taken by the findPrices method to run is just a few
milliseconds longer than 4 seconds, because the four shops are queried sequentially and
blocking one after the other, and each of them takes 1 second to calculate the price of the
requested product. How can you improve on this result?
## Parallelizing requests using a parallel Stream
After reading chapter 7, the first and quickest improvement that should occur to you would be to
avoid this sequential computation using a parallel Stream instead of a sequential, as shown in
the next listing.

## Parallelizing the findPrices method
```console
 @Test
  public void testMultipleShopParallelStream() {

    long start = System.nanoTime();
    int numberOfProcessor = Runtime.getRuntime().availableProcessors();
    System.out.println("Number of processor=" + numberOfProcessor);
    List<Shop> shops = new ArrayList<>();
    for (int i = 1; i <= numberOfProcessor; i++) {
      shops.add(new Shop("Bebit" + i));
    }
    List<String> prices = shops.parallelStream()
        .map(shop -> String.format("%s price is %.2f%n", shop.getName(), shop.getPrice("product")))
        .collect(Collectors.toList());
    System.out.println(prices);
    long duration = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Done in " + duration + " msecs");

  }
  ```
  Well done! It looks like this was a simple but very effective idea: now the four different shops are
queried in parallel, so it takes in total just a bit more than a second to complete. Can you do even
better? Let’s try to turn all the synchronous invocations to the different shops in the findPrices
method into asynchronous invocations, using what you learned so far about
CompletableFutures.

## Making asynchronous requests with CompletableFutures
You saw that you can use the factory method supplyAsync to create Completable-Future objects.
Let’s use it:
```console
List<CompletableFuture<String>> priceFutures =
shops.stream()
.map(shop -> CompletableFuture.supplyAsync(
() -> String.format("%s price is %.2f",
shop.getName(), shop.getPrice(product))))
.collect(toList());
```
Using this approach, you obtain a List<CompletableFuture<String>>, where each
CompletableFuture in the List will contain the String name of a shop when its computation is
completed. But because the findPrices method you’re trying to reimplement using
CompletableFutures has to return just a List<String>, you’ll have to wait for the completion of
all these futures and extract the value they contain before returning the List. To achieve this result, you can apply a second map operation to the original
List<CompletableFuture<String>>, invoking a join on all the futures in the List and then
waiting for their completion one by one. Note that the join method of the CompletableFuture
class has the same meaning as the get method also declared in the Future interface, with the
only difference being that join doesn’t throw any checked exception. By using it you don’t have
to bloat the lambda expression passed to this second map with a try/catch block. Putting
everything together, you can rewrite the findPrices method as follows.
