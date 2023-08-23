package games.moisoni.google_iab.models

class ProductInfoDetails {
    class OneTimePurchaseOfferDetails(
        var formattedPrice: String,
        var priceAmountMicros: Long,
        var priceCurrencyCode: String
    )

    inner class SubscriptionOfferDetails
}
