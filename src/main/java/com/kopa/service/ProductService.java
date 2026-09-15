package com.kopa.service;

import com.kopa.model.Product;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Map<String, Product> productMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        seedProducts();
    }

    private void seedProducts() {
        List<String> stdSizes = List.of("Small", "Medium", "Large");
        List<String> stdMilks = List.of("Regular", "Oat", "Almond");
        List<String> stdExtras = List.of("Extra shot", "Caramel", "Vanilla", "Hazelnut");

        addProduct(Product.builder()
            .id("prod-latte-sig")
            .name("KOPA Signature Latte")
            .description("Rich espresso, silky micro-foamed milk and our signature salted caramel finish.")
            .category("Coffee")
            .price(4.50)
            .image("/images/signature_latte.jpg")
            .available(true)
            .dietary("Signature")
            .sizeOptions(stdSizes)
            .milkOptions(stdMilks)
            .extraOptions(stdExtras)
            .build());

        addProduct(Product.builder()
            .id("prod-coldbrew-18h")
            .name("Single-Origin Cold Brew")
            .description("Slow-steeped for 18 hours. Notes of dark chocolate, candied orange peel, and toasted hazelnut.")
            .category("Cold Drinks")
            .price(4.75)
            .image("/images/cold_brew.jpg")
            .available(true)
            .dietary("Single Origin")
            .sizeOptions(List.of("Medium", "Large"))
            .milkOptions(List.of("Black", "Oat Float", "Sweet Cream"))
            .extraOptions(List.of("Extra Shot", "Vanilla Sweet Cold Foam"))
            .build());

        addProduct(Product.builder()
            .id("prod-flatwhite")
            .name("Velvet Flat White")
            .description("Double ristretto extraction with micro-textured whole milk creating a glossy, rich cup.")
            .category("Coffee")
            .price(4.25)
            .image("https://images.unsplash.com/photo-1577968897966-3d4325b36b61?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("House Favorite")
            .sizeOptions(List.of("Regular (6oz)"))
            .milkOptions(stdMilks)
            .extraOptions(List.of("Extra shot"))
            .build());

        addProduct(Product.builder()
            .id("prod-cortado")
            .name("Spanish Cortado")
            .description("Equal parts velvety steamed milk and concentrated double espresso in a heavy gibraltar glass.")
            .category("Coffee")
            .price(4.00)
            .image("https://images.unsplash.com/photo-1534778101976-62847782c213?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Barista Choice")
            .sizeOptions(List.of("Standard (4oz)"))
            .milkOptions(stdMilks)
            .extraOptions(List.of("Demerara Sugar"))
            .build());

        addProduct(Product.builder()
            .id("prod-cascara-tonic")
            .name("Cascara Sparkling Tonic")
            .description("Sun-dried coffee cherry infusion with sparkling tonic, yuzu zest, and fresh slapped rosemary.")
            .category("Cold Drinks")
            .price(4.50)
            .image("https://images.unsplash.com/photo-1517256064527-09c73fc73e38?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Refreshing")
            .sizeOptions(List.of("Large"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Extra Yuzu", "Sparkling Water Top-up"))
            .build());

        addProduct(Product.builder()
            .id("prod-matcha-uji")
            .name("Ceremonial Uji Matcha Latte")
            .description("First harvest Uji green tea whisked with silky milk and gentle raw wildflower honey.")
            .category("Tea")
            .price(5.00)
            .image("https://images.unsplash.com/photo-1536256263959-770b48d82b0a?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Organic")
            .sizeOptions(stdSizes)
            .milkOptions(stdMilks)
            .extraOptions(List.of("Vanilla", "Extra Matcha Whisk"))
            .build());

        addProduct(Product.builder()
            .id("prod-earlgrey-lavender")
            .name("Smoky Lavender Earl Grey")
            .description("Bergamot black tea blossoms infused with organic French culinary lavender.")
            .category("Tea")
            .price(4.00)
            .image("https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Caffeine Medium")
            .sizeOptions(List.of("Teapot (16oz)"))
            .milkOptions(List.of("None", "Side of Steamed Oat Milk"))
            .extraOptions(List.of("Raw Honey"))
            .build());

        addProduct(Product.builder()
            .id("prod-avo-toast")
            .name("Truffled Avocado Toast")
            .description("House-toasted sourdough, whipped Haas avocado, poached organic egg, watermelon radish, micro herbs.")
            .category("Breakfast")
            .price(8.50)
            .image("https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Vegetarian")
            .sizeOptions(List.of("Standard"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Extra Poached Egg", "Smoked Bacon", "Gluten-Free Bread"))
            .build());

        addProduct(Product.builder()
            .id("prod-granola-bowl")
            .name("Spiced Maple Granola Bowl")
            .description("Toasted oats, roasted pecans, thick Greek yogurt, seasonal wild berries, and Ceylon cinnamon drizzle.")
            .category("Breakfast")
            .price(7.00)
            .image("https://images.unsplash.com/photo-1511690743698-d9d85f2fbf38?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Healthy")
            .sizeOptions(List.of("Regular"))
            .milkOptions(List.of("Greek Yogurt", "Coconut Chia Yogurt"))
            .extraOptions(List.of("Almond Butter", "Chia Seeds"))
            .build());

        addProduct(Product.builder()
            .id("prod-almond-croissant")
            .name("Artisan Almond Croissant")
            .description("Twice-baked flaky butter pastry filled with rich almond frangipane and topped with toasted almonds.")
            .category("Pastries")
            .price(4.25)
            .image("/images/hero_cafe.jpg")
            .available(true)
            .dietary("Bakery Fresh")
            .sizeOptions(List.of("Single"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Warm up", "Extra Almond Drizzle"))
            .build());

        addProduct(Product.builder()
            .id("prod-pain-chocolat")
            .name("Pain au Chocolat")
            .description("Crisp, golden laminated dough enveloping double Belgian 70% dark chocolate batons.")
            .category("Pastries")
            .price(3.80)
            .image("https://images.unsplash.com/photo-1555507036-ab1f4038808a?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Bakery Fresh")
            .sizeOptions(List.of("Single"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Warm up"))
            .build());

        addProduct(Product.builder()
            .id("prod-basque-cake")
            .name("Burnt Basque Espresso Cheesecake")
            .description("Caramelized rustic crust with an ultra-creamy interior infused with KOPA espresso reduction.")
            .category("Desserts")
            .price(6.50)
            .image("https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("House Specialty")
            .sizeOptions(List.of("Slice"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Caramel Sauce Side", "Dollop of Cream"))
            .build());

        addProduct(Product.builder()
            .id("prod-mocha-tiramisu")
            .name("Dark Mocha Tiramisu")
            .description("Espresso-soaked Savoiardi biscuits, whipped mascarpone cream, dusted with Valrhona bitter cocoa.")
            .category("Desserts")
            .price(6.00)
            .image("https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("House Recipe")
            .sizeOptions(List.of("Individual Jar"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Extra Cocoa Dust"))
            .build());

        addProduct(Product.builder()
            .id("prod-focaccia")
            .name("Rosemary Sea Salt Focaccia")
            .description("Warm wood-fired Italian olive oil focaccia served with whipped cultured black-sea-salt butter.")
            .category("Snacks")
            .price(4.50)
            .image("https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?auto=format&fit=crop&w=800&q=80")
            .available(true)
            .dietary("Vegan Option")
            .sizeOptions(List.of("Portion"))
            .milkOptions(Collections.emptyList())
            .extraOptions(List.of("Extra Garlic Confit Dip"))
            .build());
    }

    private void addProduct(Product p) {
        productMap.put(p.getId(), p);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(productMap.values());
    }

    public Optional<Product> getProductById(String id) {
        return Optional.ofNullable(productMap.get(id));
    }

    public List<Product> getProductsByCategory(String category) {
        return productMap.values().stream()
            .filter(p -> p.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return List.of("Coffee", "Cold Drinks", "Tea", "Breakfast", "Pastries", "Desserts", "Snacks");
    }
}
