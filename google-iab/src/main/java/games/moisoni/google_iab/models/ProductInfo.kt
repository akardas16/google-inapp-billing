package games.moisoni.google_iab.models

import com.android.billingclient.api.ProductDetails
import games.moisoni.google_iab.enums.SkuProductType



class ProductInfo(val skuProductType: SkuProductType, val productDetails: ProductDetails) {
    val product: String = productDetails.productId
    val description: String = productDetails.description
    val title: String = productDetails.title
    val type: String = productDetails.productType
    val name: String = productDetails.name
    val oneTimePurchaseOfferFormattedPrice: String = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
    val oneTimePurchaseOfferPriceAmountMicros: Long = productDetails.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0
    val oneTimePurchaseOfferPriceCurrencyCode: String = productDetails.oneTimePurchaseOfferDetails?.priceCurrencyCode ?: ""
    private val subscriptionOfferDetails: MutableList<SubscriptionOfferDetails>

    init {

        val offerDetailsList = productDetails.subscriptionOfferDetails
        subscriptionOfferDetails = ArrayList()
        if (offerDetailsList != null) {
            for (offerDetails in offerDetailsList) {
                val newOfferDetails = createSubscriptionOfferDetails(offerDetails)
                subscriptionOfferDetails.add(newOfferDetails)
            }
        }
    }

    fun getSubscriptionOfferDetails(): List<SubscriptionOfferDetails> {
        return subscriptionOfferDetails
    }

    private fun createSubscriptionOfferDetails(offerDetails: ProductDetails.SubscriptionOfferDetails): SubscriptionOfferDetails {
        return SubscriptionOfferDetails(
            offerDetails.offerId!!,
            offerDetails.pricingPhases.pricingPhaseList,
            offerDetails.offerTags,
            offerDetails.offerToken,
            offerDetails.basePlanId
        )
    }
}