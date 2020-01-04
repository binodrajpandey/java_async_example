package bebit.AsyncExample;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ReactiveShopTest {

  @Test
  void test() {
    List<ReactiveShop> shops = new ArrayList<>();
    for (int i = 1; i <= 100; i++) {
      shops.add(new ReactiveShop("Bebit" + i));
    }
    Stream<CompletableFuture<String>> futurePrices = shops.stream()
        .map(shop -> CompletableFuture.supplyAsync(() -> shop.getPriceWithDiscound("product"),
            Executors.newFixedThreadPool(100)))
        .map(future -> future.thenApply(Quote::parse))
        .map(future -> future.thenCompose(quote -> CompletableFuture
            .supplyAsync(() -> Discount.applyDiscount(quote), Executors.newFixedThreadPool(100))));
    Stream<CompletableFuture<Void>> voids =
        futurePrices.map(future -> future.thenAccept(System.out::println));
    CompletableFuture[] arrays = voids.toArray(size -> new CompletableFuture[size]);
    CompletableFuture.allOf(arrays).join();
  }

}
