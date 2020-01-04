package bebit.AsyncExample;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ReactiveShop {

  private String name;

  public ReactiveShop(String name) {
    this.name = name;
  }

  public double getPrice(String product) {
    return calculatePrice(product);
  }


  public Future<Double> getPriceAsync(String product) {
    return CompletableFuture.supplyAsync(() -> calculatePrice(product));

  }

  public String getPriceWithDiscound(String product) {
    double price = calculatePrice(product);
    Discount.Code code =
        Discount.Code.values()[new Random().nextInt(Discount.Code.values().length)];
    return String.format("%s:%.2f:%s", name, price, code);
  }

  private double calculatePrice(String product) {
    Delay.randomDelay();
    return new Random().nextDouble() * product.charAt(0) + product.charAt(1);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
