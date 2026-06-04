package com.ecommerce.api.config;

import com.ecommerce.api.domain.*;
import com.ecommerce.api.repository.AddressRepository;
import com.ecommerce.api.repository.CategoryRepository;
import com.ecommerce.api.repository.CouponRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String IMG_BASE = "https://picsum.photos/seed/shopflow-";

    private record CategorySeed(String name, String slug) {}

    private record ProductSeed(String name, String description, String price, int stock, String categorySlug, String imageSeed) {}

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed("Aksesuar", "aksesuar"),
            new CategorySeed("Bilgisayar", "bilgisayar"),
            new CategorySeed("Mobil", "mobil"),
            new CategorySeed("Giyim", "giyim"),
            new CategorySeed("Ayakkabı", "ayakkabi"),
            new CategorySeed("Ev & Yaşam", "ev-yasam"),
            new CategorySeed("Spor & Outdoor", "spor"),
            new CategorySeed("Kozmetik", "kozmetik"),
            new CategorySeed("Kitap & Hobi", "kitap"),
            new CategorySeed("Oyun & Konsol", "oyun"),
            new CategorySeed("Ses & Müzik", "ses")
    );

    private static final List<ProductSeed> PRODUCT_SEEDS = List.of(
            // Aksesuar
            new ProductSeed("Wireless Mouse", "Ergonomic wireless mouse with USB receiver", "299.99", 50, "aksesuar", "wireless-mouse"),
            new ProductSeed("USB-C Hub", "7-in-1 USB-C hub with HDMI and SD card reader", "599.50", 40, "aksesuar", "usb-c-hub"),
            new ProductSeed("Webcam HD", "1080p webcam with built-in microphone", "749.00", 22, "aksesuar", "webcam-hd"),
            new ProductSeed("Powerbank 20000mAh", "Hızlı şarj destekli taşınabilir güç kaynağı", "649.00", 45, "aksesuar", "powerbank-20k"),
            new ProductSeed("Telefon Kılıfı Premium", "Darbe emici şeffaf silikon kılıf", "149.90", 120, "aksesuar", "phone-case"),
            new ProductSeed("MagSafe Şarj Standı", "Masaüstü manyetik kablosuz şarj standı", "899.00", 35, "aksesuar", "magsafe-stand"),
            new ProductSeed("Laptop Sırt Çantası 15\"", "Su geçirmez bölmeli iş çantası", "549.00", 28, "aksesuar", "laptop-bag"),
            new ProductSeed("USB-C Kablo 2m", "100W PD destekli örgülü kablo", "129.00", 200, "aksesuar", "usbc-cable"),
            new ProductSeed("Kablosuz Şarj Cihazı", "15W Qi uyumlu hızlı şarj pedi", "399.00", 60, "aksesuar", "wireless-charger"),
            new ProductSeed("SSD Taşıma Kutusu", "M.2 NVMe uyumlu alüminyum kutu", "279.00", 40, "aksesuar", "ssd-enclosure"),

            // Bilgisayar
            new ProductSeed("Mechanical Keyboard", "RGB mechanical keyboard, blue switches", "1499.00", 25, "bilgisayar", "mech-keyboard"),
            new ProductSeed("Laptop Stand", "Aluminum adjustable laptop stand", "449.00", 30, "bilgisayar", "laptop-stand"),
            new ProductSeed("27\" Monitor", "QHD IPS display, 144Hz", "5499.00", 15, "bilgisayar", "monitor-27"),
            new ProductSeed("Gaming Mouse Pad XXL", "Kaymaz tabanlı geniş oyun mouse pad", "199.00", 80, "bilgisayar", "mousepad-xxl"),
            new ProductSeed("SSD 1TB NVMe", "PCIe 4.0 yüksek hızlı dahili disk", "2899.00", 40, "bilgisayar", "ssd-1tb"),
            new ProductSeed("RAM 16GB DDR5", "5600MHz masaüstü bellek modülü", "1899.00", 35, "bilgisayar", "ram-ddr5"),
            new ProductSeed("Mekanik Klavye 60%", "Kompakt hot-swap mekanik klavye", "1299.00", 30, "bilgisayar", "keyboard-60"),
            new ProductSeed("USB-C Docking Station", "Çift monitör destekli profesyonel dock", "3499.00", 18, "bilgisayar", "docking-station"),
            new ProductSeed("Webcam 4K Pro", "Otomatik kadrajlı 4K streaming kamerası", "2199.00", 20, "bilgisayar", "webcam-4k"),
            new ProductSeed("Mini PC Intel i5", "16GB RAM, 512GB SSD mini bilgisayar", "8999.00", 12, "bilgisayar", "mini-pc"),

            // Mobil
            new ProductSeed("Bluetooth Earbuds", "Active noise cancelling, 24h battery", "899.00", 35, "mobil", "earbuds-anc"),
            new ProductSeed("Phone Tripod", "Flexible tripod for smartphones", "199.00", 60, "mobil", "phone-tripod"),
            new ProductSeed("Ekran Koruyucu Cam", "9H sertlikte tam kaplama cam", "89.90", 150, "mobil", "screen-glass"),
            new ProductSeed("Araç Telefon Tutucu", "Manyetik vent mount telefon tutucu", "179.00", 90, "mobil", "car-mount"),
            new ProductSeed("Mobil Oyun Kolu", "Bluetooth düşük gecikmeli oyun kolu", "749.00", 45, "mobil", "mobile-controller"),
            new ProductSeed("Taşınabilir Mini Projektör", "1080p LED mini projeksiyon cihazı", "4299.00", 15, "mobil", "mini-projector"),
            new ProductSeed("Selfie Stick Tripod", "Uzaktan kumandalı 3 eksenli tripod", "249.00", 70, "mobil", "selfie-stick"),
            new ProductSeed("Akıllı Saat Kordonu", "Silikon spor kordonu, çoklu renk", "99.00", 110, "mobil", "watch-strap"),
            new ProductSeed("Type-C Hub Telefon", "Telefon için OTG ve HDMI hub", "449.00", 55, "mobil", "phone-hub"),
            new ProductSeed("Tablet Stand Ayarlanabilir", "Alüminyum katlanır tablet standı", "329.00", 50, "mobil", "tablet-stand"),

            // Giyim
            new ProductSeed("Pamuklu Basic Tişört", "Regular fit %100 pamuk günlük tişört", "199.90", 100, "giyim", "tshirt-basic"),
            new ProductSeed("Slim Fit Kot Pantolon", "Esnek kumaş erkek slim fit jean", "449.90", 80, "giyim", "jeans-slim"),
            new ProductSeed("Kapüşonlu Sweatshirt", "Unisex oversize kapüşonlu sweat", "399.00", 75, "giyim", "hoodie"),
            new ProductSeed("Yaz Elbisesi Çiçekli", "Midi boy günlük yaz elbisesi", "549.00", 60, "giyim", "summer-dress"),
            new ProductSeed("Erkek Polo Yaka", "Nefes alabilir pique polo tişört", "279.00", 90, "giyim", "polo-shirt"),
            new ProductSeed("Kadın Blazer Ceket", "Ofis ve günlük kullanım blazer", "699.00", 45, "giyim", "blazer"),
            new ProductSeed("Polar Mont", "Su itici hafif kış montu", "899.00", 40, "giyim", "fleece-jacket"),
            new ProductSeed("Denim Ceket", "Klasik kesim mavi denim ceket", "649.00", 50, "giyim", "denim-jacket"),

            // Ayakkabı
            new ProductSeed("Koşu Ayakkabısı Erkek", "Hafif tabanlı nefes alan koşu ayakkabısı", "899.00", 55, "ayakkabi", "running-shoe"),
            new ProductSeed("Kadın Babet", "Günlük deri kadın babet", "449.00", 70, "ayakkabi", "ballet-flat"),
            new ProductSeed("Sneaker Unisex Beyaz", "Günlük beyaz spor ayakkabı", "799.00", 65, "ayakkabi", "sneaker-white"),
            new ProductSeed("Deri Kış Botu", "Su geçirmez erkek kış botu", "1199.00", 35, "ayakkabi", "winter-boot"),
            new ProductSeed("Ev Terliği", "Hafif memory foam ev terliği", "149.90", 120, "ayakkabi", "slipper"),
            new ProductSeed("Basketbol Ayakkabısı", "Yüksek bilek destekli basketbol ayakkabısı", "1299.00", 30, "ayakkabi", "basketball-shoe"),
            new ProductSeed("Yaz Sandaleti", "Rahat tabanlı unisex sandalet", "349.00", 80, "ayakkabi", "sandal"),
            new ProductSeed("Trekking Bot", "Outdoor su geçirmez trekking botu", "1499.00", 25, "ayakkabi", "trekking-boot"),

            // Ev & Yaşam
            new ProductSeed("Yorgan Çift Kişilik", "4 mevsim mikrofiber yorgan", "599.00", 40, "ev-yasam", "duvet"),
            new ProductSeed("Nevresim Takımı Cotton", "Çift kişilik pamuk nevresim seti", "449.00", 50, "ev-yasam", "bedding"),
            new ProductSeed("Mutfak Robotu", "1000W çok fonksiyonlu mutfak robotu", "3499.00", 20, "ev-yasam", "food-processor"),
            new ProductSeed("Filtre Kahve Makinesi", "Cam demlikli filtre kahve makinesi", "899.00", 30, "ev-yasam", "coffee-maker"),
            new ProductSeed("LED Masa Lambası", "3 modlu göz yormayan LED lamba", "299.00", 60, "ev-yasam", "desk-lamp"),
            new ProductSeed("Termos 500ml", "24 saat sıcak tutan paslanmaz termos", "249.00", 85, "ev-yasam", "thermos"),
            new ProductSeed("Bambu Kesme Tahtası", "Doğal bambu mutfak kesme tahtası", "179.00", 70, "ev-yasam", "cutting-board"),
            new ProductSeed("Dekoratif Yastık", "Kadife kılıflı dekor yastığı 45x45", "129.00", 95, "ev-yasam", "decorative-pillow"),

            // Spor
            new ProductSeed("Yoga Matı 6mm", "Kaymaz yüzeyli profesyonel yoga matı", "249.00", 75, "spor", "yoga-mat"),
            new ProductSeed("Dambıl Set 2x5kg", "Neopren kaplı ev fitness dambıl seti", "399.00", 40, "spor", "dumbbell-set"),
            new ProductSeed("Spor Çantası 40L", "Ayakkabı bölmeli su geçirmez spor çantası", "449.00", 55, "spor", "gym-bag"),
            new ProductSeed("Futbol Topu Pro", "FIFA onaylı maç futbol topu", "299.00", 60, "spor", "football"),
            new ProductSeed("Bisiklet Kaskı", "Havalandırmalı hafif bisiklet kaskı", "549.00", 45, "spor", "bike-helmet"),
            new ProductSeed("Resistance Band Set", "5 parça direnç bandı antrenman seti", "199.00", 90, "spor", "resistance-bands"),
            new ProductSeed("Spor Matarası 750ml", "BPA free sızdırmaz spor matarası", "99.00", 130, "spor", "sport-bottle"),
            new ProductSeed("Koşu Kemeri", "Telefon bölmeli koşu bel kemeri", "179.00", 80, "spor", "running-belt"),

            // Kozmetik
            new ProductSeed("Nemlendirici Krem 50ml", "Hyaluronik asitli günlük nemlendirici", "249.00", 100, "kozmetik", "moisturizer"),
            new ProductSeed("Güneş Kremi SPF50", "Yağsız formül yüz güneş kremi", "199.00", 110, "kozmetik", "sunscreen"),
            new ProductSeed("Mat Ruj Kırmızı", "Uzun süre kalıcı mat finish ruj", "179.00", 85, "kozmetik", "lipstick"),
            new ProductSeed("Erkek Parfüm 100ml", "Odunsu notalı erkek parfüm", "699.00", 50, "kozmetik", "perfume-men"),
            new ProductSeed("Kadın Parfüm 50ml", "Çiçeksi notalı kadın parfüm", "749.00", 45, "kozmetik", "perfume-women"),
            new ProductSeed("Saç Bakım Şampuanı", "Onarıcı keratin şampuan 400ml", "149.00", 120, "kozmetik", "shampoo"),
            new ProductSeed("Yüz Temizleme Jeli", "Hassas ciltler için temizleme jeli", "129.00", 115, "kozmetik", "face-wash"),
            new ProductSeed("Makyaj Fırça Seti", "10 parça profesyonel fırça seti", "299.00", 70, "kozmetik", "makeup-brushes"),

            // Kitap & Hobi
            new ProductSeed("Atomic Habits (Türkçe)", "James Clear - Alışkanlıklar üzerine bestseller", "89.90", 80, "kitap", "book-atomic"),
            new ProductSeed("Sapiens Tarih", "Yuval Noah Harari - İnsanlık tarihi", "99.90", 75, "kitap", "book-sapiens"),
            new ProductSeed("Resim Boyama Seti", "Tuval, fırça ve boya seti", "349.00", 40, "kitap", "painting-set"),
            new ProductSeed("Puzzle 1000 Parça", "Manzara temalı 1000 parça puzzle", "199.00", 55, "kitap", "puzzle-1000"),
            new ProductSeed("Masa Oyunu Monopoly", "Klasik aile masa oyunu", "449.00", 35, "kitap", "monopoly"),
            new ProductSeed("Defter A5 Çizgili", "Sert kapak 120 yaprak defter", "49.90", 200, "kitap", "notebook-a5"),
            new ProductSeed("Roller Kalem Seti", "2'li premium roller kalem seti", "129.00", 90, "kitap", "pen-set"),
            new ProductSeed("Çocuk Hikaye Kitabı", "Resimli 3-6 yaş hikaye kitabı", "59.90", 100, "kitap", "kids-book"),

            // Oyun & Konsol
            new ProductSeed("PS5 DualSense Kol", "PlayStation 5 kablosuz oyun kolu", "2499.00", 25, "oyun", "ps5-controller"),
            new ProductSeed("Gaming Headset 7.1", "RGB surround sound oyuncu kulaklığı", "899.00", 40, "oyun", "gaming-headset"),
            new ProductSeed("Gaming Mouse RGB", "16000 DPI programlanabilir oyun mouse", "649.00", 50, "oyun", "gaming-mouse"),
            new ProductSeed("Nintendo Switch Kılıf", "Taşıma ve koruma kılıfı", "299.00", 45, "oyun", "switch-case"),
            new ProductSeed("RGB Mouse Pad XL", "Geniş kaymaz RGB oyun mouse pad", "249.00", 70, "oyun", "rgb-mousepad"),
            new ProductSeed("PlayStation Oyun CD", "Aksiyon-macera PS5 oyunu", "799.00", 35, "oyun", "ps5-game"),
            new ProductSeed("Steam Deck Koruyucu", "Silikon koruyucu kılıf", "199.00", 40, "oyun", "steam-deck-case"),
            new ProductSeed("Arcade Fight Stick", "PC ve konsol uyumlu fight stick", "1799.00", 15, "oyun", "fight-stick"),

            // Ses & Müzik
            new ProductSeed("Bluetooth Hoparlör", "360° ses taşınabilir hoparlör", "549.00", 55, "ses", "bt-speaker"),
            new ProductSeed("Studio Kulaklık", "Kapalı kulaklık stüdyo monitör kulaklık", "1299.00", 30, "ses", "studio-headphones"),
            new ProductSeed("USB Condenser Mikrofon", "Podcast ve yayın için mikrofon", "899.00", 35, "ses", "usb-mic"),
            new ProductSeed("Soundbar 2.1", "Bluetooth destekli TV soundbar", "2199.00", 20, "ses", "soundbar"),
            new ProductSeed("Plak Çalar Retro", "3 hızlı ahşap görünümlü pikap", "3499.00", 12, "ses", "turntable"),
            new ProductSeed("In-Ear Monitor", "Profesyonel sahne kulaklığı", "749.00", 40, "ses", "iem-earphones"),
            new ProductSeed("Karaoke Mikrofon Set", "Bluetooth karaoke mikrofon + hoparlör", "499.00", 45, "ses", "karaoke-mic"),
            new ProductSeed("Mini Amfi", "Gitar ve mikrofon için mini amfi", "999.00", 25, "ses", "mini-amp")
    );

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDemoAddress();
        Map<String, Category> categories = seedCategories();
        seedProducts(categories);
        seedCoupons();
    }

    private void seedAdmin() {
        String adminEmail = "admin@shop.com";
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        userRepository.save(User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode("admin123"))
                .fullName("Shop Admin")
                .role(Role.ADMIN)
                .build());

        log.info("Demo admin: {} / admin123", adminEmail);
    }

    private void seedDemoAddress() {
        userRepository.findByEmail("admin@shop.com").ifPresent(user -> {
            if (addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(user.getId()).isEmpty()) {
                addressRepository.save(Address.builder()
                        .user(user)
                        .title("Ev")
                        .fullName("Shop Admin")
                        .phone("05551234567")
                        .city("İstanbul")
                        .district("Kadıköy")
                        .addressLine("Caferağa Mah. Demo Sok. No:1")
                        .postalCode("34710")
                        .defaultAddress(true)
                        .build());
                log.info("Demo address seeded for admin");
            }
        });
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> bySlug = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getSlug, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        for (CategorySeed seed : CATEGORY_SEEDS) {
            bySlug.computeIfAbsent(seed.slug(), slug -> categoryRepository.save(
                    Category.builder().name(seed.name()).slug(seed.slug()).build()
            ));
        }

        log.info("Categories ready: {}", bySlug.size());
        return bySlug;
    }

    private void seedProducts(Map<String, Category> categories) {
        int created = 0;
        int updated = 0;

        for (ProductSeed seed : PRODUCT_SEEDS) {
            Category category = categories.get(seed.categorySlug());
            if (category == null) {
                log.warn("Skipping product {} — category {} missing", seed.name(), seed.categorySlug());
                continue;
            }

            String imageUrl = imageUrl(seed.imageSeed());
            var existing = productRepository.findByName(seed.name());
            if (existing.isPresent()) {
                Product product = existing.get();
                if (shouldRefreshImage(product.getImageUrl())) {
                    product.setImageUrl(imageUrl);
                    productRepository.save(product);
                    updated++;
                }
                continue;
            }

            productRepository.save(product(
                    seed.name(),
                    seed.description(),
                    seed.price(),
                    seed.stock(),
                    category,
                    imageUrl
            ));
            created++;
        }

        markFeaturedProducts();
        log.info("Catalog ready — {} categories, {} products (+{} new, {} images refreshed)",
                categories.size(), productRepository.count(), created, updated);
    }

    private void markFeaturedProducts() {
        var products = productRepository.findAll();
        int marked = 0;
        for (int i = 0; i < products.size(); i++) {
            if (i % 8 == 0) {
                Product p = products.get(i);
                if (!p.isFeatured()) {
                    p.setFeatured(true);
                    productRepository.save(p);
                    marked++;
                }
            }
        }
        if (marked > 0) {
            log.info("Marked {} products as featured", marked);
        }
    }

    private static boolean shouldRefreshImage(String imageUrl) {
        return imageUrl == null
                || imageUrl.isBlank()
                || imageUrl.contains("unsplash.com");
    }

    private static String imageUrl(String seed) {
        return IMG_BASE + seed + "/600/450";
    }

    private Product product(String name, String description, String price, int stock, Category category, String imageUrl) {
        return Product.builder()
                .name(name)
                .description(description)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .category(category)
                .imageUrl(imageUrl)
                .lowStockThreshold(5)
                .build();
    }

    private void seedCoupons() {
        if (couponRepository.count() > 0) return;
        couponRepository.saveAll(List.of(
                Coupon.builder().code("WELCOME10").discountPercent(10)
                        .minOrderAmount(new BigDecimal("100")).maxUses(100)
                        .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS)).build(),
                Coupon.builder().code("SUMMER20").discountPercent(20)
                        .minOrderAmount(new BigDecimal("500")).maxUses(50)
                        .expiresAt(Instant.now().plus(90, ChronoUnit.DAYS)).build()
        ));
        log.info("Demo coupons: WELCOME10, SUMMER20");
    }
}
