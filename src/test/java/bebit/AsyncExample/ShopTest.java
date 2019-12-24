package bebit.AsyncExample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ShopTest {

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
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    long retrievalTime = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Price Returned after " + retrievalTime + " msec.");

  }

  /**
   * Result: Done in 4013 msecs
   */
  @Test
  public void testMultipleShopSequential() {

    long start = System.nanoTime();
    System.out.println(findPrices("myPhone27S"));
    long duration = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Done in " + duration + " msecs");

  }

  /**
   * Done in 1010 msecs, when number of shop is equal to number of processor.
   */
  @Test
  public void testMultipleShopParallelStream() {

    long start = System.nanoTime();
    System.out.println(findPricesParallelSameAsAvailableProcessor("myPhone27S"));
    long duration = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Done in " + duration + " msecs");

  }

  /**
   * Done in 10042 msecs.Here number of shops=100 Number of processor=12 total time is 100/12
   */
  @Test
  public void testMultipleShopParallelStreamShopGreaterThanAvailableProcessor() {

    long start = System.nanoTime();
    System.out.println(findPricesParallelMoreProcessor("myPhone27S"));
    long duration = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Done in " + duration + " msecs");

  }

  /**
   * Done in 1023 msecs. 100 shops but completed in almost 1 second.
   */
  @Test
  public void testMultipleShopAsync() {

    long start = System.nanoTime();
    System.out.println(findPricesAsync("myPhone27S"));
    long duration = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Done in " + duration + " msecs");

  }

  @Test
  public void testSingleShopWithDiscount() {

    Shop shop = new Shop("my shop");
    long start = System.nanoTime();
    Future<Double> futurePrice = shop.getPriceAsync("product");
    long invocationTime = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Invocation return after " + invocationTime + " msec.");
    try {
      double price = futurePrice.get(5, TimeUnit.SECONDS);
      System.out.printf("Price is %.2f%n", price);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    long retrievalTime = (System.nanoTime() - start) / 1_000_000;
    System.out.println("Price Returned after " + retrievalTime + " msec.");

  }

  public List<String> findPrices(String product) {
    List<Shop> shops = Arrays.asList(new Shop("BestPrice"), new Shop("LetsSaveBig"),
        new Shop("MyFavoriteShop"), new Shop("BuyItAll"));

    return shops.stream()
        .map(shop -> String.format("%s price is %.2f%n", shop.getName(), shop.getPrice(product)))
        .collect(Collectors.toList());
  }

  public List<String> findPricesParallelSameAsAvailableProcessor(String product) {
    int numberOfProcessor = Runtime.getRuntime().availableProcessors();
    System.out.println("Number of processor=" + numberOfProcessor);
    List<Shop> shops = new ArrayList<>();
    for (int i = 1; i <= numberOfProcessor; i++) {
      shops.add(new Shop("Bebit" + i));
    }
    return shops.parallelStream()
        .map(shop -> String.format("%s price is %.2f%n", shop.getName(), shop.getPrice(product)))
        .collect(Collectors.toList());
  }

  public List<String> findPricesParallelMoreProcessor(String product) {
    int numberOfProcessor = Runtime.getRuntime().availableProcessors();
    System.out.println("Number of processor=" + numberOfProcessor);
    List<Shop> shops = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      shops.add(new Shop("Bebit" + i));
    }
    return shops.parallelStream()
        .map(shop -> String.format("%s price is %.2f%n", shop.getName(), shop.getPrice(product)))
        .collect(Collectors.toList());
  }

  public List<String> findPricesAsync(String product) {
    List<Shop> shops = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      shops.add(new Shop("Bebit" + i));
    }
    List<CompletableFuture<String>> futures = shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(
            () -> String.format("%s price is %.2f%n", shop.getName(), shop.getPrice(product)),
            Executors.newFixedThreadPool(100)))
        .collect(Collectors.toList());
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

}
