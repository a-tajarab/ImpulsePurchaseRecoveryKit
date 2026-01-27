package com.example.impulsepurchaserecoverykit.debug

import kotlin.random.Random
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

object ReceiptGenerator {
    enum class Category {GROCERY, CAFE, RETAIL, ONLINE}
    fun generateBatch(count: Int): List<ReceiptData> {
        return (1..count).map { seed -> generateOne(seed) }
    }

    private fun generateOne(seed: Int): ReceiptData {
        val rng = Random(seed)

        val category = Category.entries[rng.nextInt(Category.entries.size)]
        val now = LocalDateTime.now()

        return randomReceipt(
            category = category,
            now = now,
            daysBackRange = 1..120,
            rng = rng
        )
    }

    private fun randomReceipt(
        category: Category,
        now: LocalDateTime,
        daysBackRange: IntRange,
        rng: Random
    ): ReceiptData {

        val stores = when (category) {
            Category.GROCERY -> listOf(
                "Tesco", "ASDA", "Sainsbury's", "Morrisons", "Aldi", "Lidl", "Waitrose", "M&S Food"
            )
            Category.CAFE -> listOf(
                "Dunkin'", "Costa Coffee", "Greggs", "Starbucks", "Pret A Manger"
            )
            Category.RETAIL -> listOf(
                "Primark", "H&M", "Zara", "JD Sports", "Next", "Matalan"
            )
            Category.ONLINE -> listOf(
                "Amazon", "ASOS", "Nike.com", "adidas.co.uk", "eBay", "Argos", "SHEIN", "Temu"
            )
        }

        val store = stores.random(rng)

        val dt = now
            .minusDays(rng.nextInt(daysBackRange.first, daysBackRange.last + 1).toLong())
            .withHour(rng.nextInt(9, 21))
            .withMinute(listOf(0, 5, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 55).random(rng))
            .withSecond(0)

        val dateStr = dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm"))

        val address = when (category) {
            Category.ONLINE -> listOf("Order Confirmation", "Delivery: London, UK")
            else -> listOf("High Street", "London, W1")
        }

        val phone = if (category == Category.ONLINE) null else "0207 ${rng.nextInt(100, 999)} ${rng.nextInt(1000, 9999)}"

        val items = when (category) {
            Category.GROCERY -> randomItems(
                rng,
                listOf("Milk","Bread","Eggs","Chicken","Cereal","Tomatoes","Apples","Bananas","Cheese","Yogurt","Pasta","Rice"),
                5..12,
                0.75..7.50
            )
            Category.CAFE -> randomItems(
                rng,
                listOf("Coffee","Latte","Cappuccino","Donuts","Bagel w/ Cream Cheese","Orange Juice","Croissant","Muffin"),
                2..6,
                1.50..6.50
            )
            Category.RETAIL -> randomItems(
                rng,
                listOf("T-Shirt","Jeans","Hoodie","Trainers","Socks","Cap","Dress","Skirt","Jacket","Sunglasses"),
                1..5,
                4.99..59.99
            )
            Category.ONLINE -> randomItems(
                rng,
                listOf("T-Shirt","Sneakers","Phone Case","Headphones","Hoodie","Jeans","Skincare Set","Water Bottle","Backpack"),
                1..6,
                3.99..89.99
            )
        }

        val subtotal = round2(items.sumOf { it.price })

        // simple tax model: groceries low tax, others higher
        val taxRate = when (category) {
            Category.GROCERY -> 0.00
            Category.CAFE -> 0.00
            Category.RETAIL -> 0.20
            Category.ONLINE -> 0.20
        }

        val tax = round2(subtotal * taxRate)
        val total = round2(subtotal + tax)

        val paymentMethod = listOf("VISA", "MASTERCARD", "AMEX", "Debit").random(rng)
        val last4 = rng.nextInt(1000, 9999).toString()

        return ReceiptData(
            storeName = store,
            addressLines = address,
            phone = phone,
            dateTime = dateStr,
            items = items,
            subtotal = subtotal,
            tax = tax,
            total = total,
            paymentMethod = paymentMethod,
            last4 = last4
        )
    }

    private fun randomItems(
        rng: Random,
        names: List<String>,
        countRange: IntRange,
        priceRange: ClosedFloatingPointRange<Double>
    ): List<ReceiptItem> {
        val count = rng.nextInt(countRange.first, countRange.last + 1)
        return (1..count).map {
            val name = names.random(rng)
            val raw = rng.nextDouble(priceRange.start, priceRange.endInclusive)
            ReceiptItem(name, round2(raw))
        }
    }

    private fun round2(x: Double): Double = round(x * 100) / 100.0
}