package com.example.emergencywardrobe.config;

import com.example.emergencywardrobe.entity.*;
import com.example.emergencywardrobe.entity.ProductVarient;
import com.example.emergencywardrobe.repository.CategoryRepository;
import com.example.emergencywardrobe.repository.ProductRepository;
import com.example.emergencywardrobe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
                      UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();

        if (categoryRepository.count() > 0) {
            return;
        }

        Category kits = categoryRepository.save(new Category("Emergency Kits"));
        Category dresses = categoryRepository.save(new Category("Dresses"));
        Category kurtis = categoryRepository.save(new Category("Kurtis"));
        Category footwear = categoryRepository.save(new Category("Footwear"));
        Category accessories = categoryRepository.save(new Category("Accessories"));

        buyOnly(kits, "Period Emergency Kit",
                "Sanitary pads, safety pins, hair ties, tissues, disposal bags.",
                "149.00", 50, "period-kit");
        buyOnly(kits, "Wardrobe Rescue Kit",
                "Fashion tape, safety pins, mini sewing kit, hair pins, stain remover.",
                "199.00", 40, "wardrobe-kit");
        buyOnly(kits, "Event Emergency Kit",
                "Fashion tape, safety pins, hair clips, sewing kit, stain remover, tissues.",
                "249.00", 35, "event-kit");
        buyOnly(kits, "Travel Emergency Kit",
                "Travel-size essentials for wardrobe mishaps on the go.",
                "179.00", 30, "travel-kit");
        buyOnly(kits, "Beauty Emergency Kit",
                "Blotting sheets, mini deodorant, lip balm, bobby pins, makeup wipes.",
                "229.00", 30, "beauty-kit");

        Product blackDress = rentOnly(dresses, "Black Evening Dress",
                "Elegant black evening dress, available for rent.",
                "399.00", "1000.00", 5, "black-dress");
        blackDress.getVariants().add(new ProductVarient(blackDress, "M", 3));
        blackDress.getVariants().add(new ProductVarient(blackDress, "L", 2));
        productRepository.save(blackDress);

        Product redGown = rentOnly(dresses, "Red Party Gown",
                "Statement red gown for evening events, rent for the night.",
                "499.00", "1200.00", 4, "red-gown");
        redGown.getVariants().add(new ProductVarient(redGown, "S", 1));
        redGown.getVariants().add(new ProductVarient(redGown, "M", 2));
        redGown.getVariants().add(new ProductVarient(redGown, "L", 1));
        productRepository.save(redGown);

        Product floralDress = buyAndRent(dresses, "Floral Summer Dress",
                "Light floral dress, good for both keeping and one-off wear.",
                "899.00", "249.00", "600.00", 6, "floral-dress");
        floralDress.getVariants().add(new ProductVarient(floralDress, "S", 2));
        floralDress.getVariants().add(new ProductVarient(floralDress, "M", 3));
        floralDress.getVariants().add(new ProductVarient(floralDress, "L", 1));
        productRepository.save(floralDress);

        Product kurtiSet = buyAndRent(kurtis, "Embroidered Kurti Set",
                "Embroidered kurti with matching dupatta.",
                "699.00", "199.00", "400.00", 7, "kurti-set");
        kurtiSet.getVariants().add(new ProductVarient(kurtiSet, "S", 2));
        kurtiSet.getVariants().add(new ProductVarient(kurtiSet, "M", 3));
        kurtiSet.getVariants().add(new ProductVarient(kurtiSet, "L", 2));
        kurtiSet.getVariants().add(new ProductVarient(kurtiSet, "XL", 1));
        productRepository.save(kurtiSet);

        Product heels = buyAndRent(footwear, "Classic Black Heels",
                "Comfortable block heels. Buy or rent.",
                "1299.00", "149.00", "500.00", 8, "black-heels");
        heels.getVariants().add(new ProductVarient(heels, "6", 3));
        heels.getVariants().add(new ProductVarient(heels, "7", 3));
        heels.getVariants().add(new ProductVarient(heels, "8", 2));
        productRepository.save(heels);

        Product sandals = buyOnly(footwear, "Strappy Sandals",
                "Everyday strappy sandals, neutral tone.",
                "999.00", 10, "sandals");
        sandals.getVariants().add(new ProductVarient(sandals, "5", 3));
        sandals.getVariants().add(new ProductVarient(sandals, "6", 3));
        sandals.getVariants().add(new ProductVarient(sandals, "7", 2));
        sandals.getVariants().add(new ProductVarient(sandals, "8", 2));
        productRepository.save(sandals);

        Product flats = buyOnly(footwear, "Casual Flats",
                "All-day comfortable flats.",
                "799.00", 12, "flats");
        flats.getVariants().add(new ProductVarient(flats, "5", 3));
        flats.getVariants().add(new ProductVarient(flats, "6", 3));
        flats.getVariants().add(new ProductVarient(flats, "7", 3));
        flats.getVariants().add(new ProductVarient(flats, "8", 2));
        flats.getVariants().add(new ProductVarient(flats, "9", 1));
        productRepository.save(flats);

        buyOnly(accessories, "Statement Clutch Bag",
                "Compact evening clutch, fits phone and essentials.",
                "599.00", 15, "clutch-bag");
        buyOnly(accessories, "Pearl Jewelry Set",
                "Necklace and earring set, pairs with most outfits.",
                "449.00", 15, "pearl-set");
    }

    private Product buyOnly(Category category, String name, String description,
                            String price, int stock, String imageSeed) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setPurchasable(true);
        product.setRentable(false);
        product.setImageUrl(imageUrl(imageSeed));
        return productRepository.save(product);
    }

    private Product rentOnly(Category category, String name, String description,
                             String rentalPricePerDay, String securityDeposit, int stock, String imageSeed) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setDescription(description);
        product.setRentalPricePerDay(new BigDecimal(rentalPricePerDay));
        product.setSecurityDeposit(new BigDecimal(securityDeposit));
        product.setStock(stock);
        product.setPurchasable(false);
        product.setRentable(true);
        product.setImageUrl(imageUrl(imageSeed));
        return productRepository.save(product);
    }

    private Product buyAndRent(Category category, String name, String description, String price,
                               String rentalPricePerDay, String securityDeposit, int stock, String imageSeed) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setRentalPricePerDay(new BigDecimal(rentalPricePerDay));
        product.setSecurityDeposit(new BigDecimal(securityDeposit));
        product.setStock(stock);
        product.setPurchasable(true);
        product.setRentable(true);
        product.setImageUrl(imageUrl(imageSeed));
        return productRepository.save(product);
    }

    private String imageUrl(String seed) {
        return "https://picsum.photos/seed/" + seed + "/500/625";
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = new User(
                "Admin",
                adminEmail,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN
        );
        userRepository.save(admin);

        System.out.println("Seeded demo admin account: " + adminEmail
                + " (change ADMIN_PASSWORD env var before deploying publicly)");
    }
}