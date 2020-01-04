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

