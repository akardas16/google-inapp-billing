package games.moisoni.google_iab.models

import com.android.billingclient.api.ProductDetails.PricingPhase

class SubscriptionOfferDetails(
    val offerId: String,
    pricingPhases: List<PricingPhase>?,
    val offerTags: List<String>,
    val offerToken: String,
    val basePlanId: String
) {
    private val pricingPhases: MutableList<PricingPhases>

    init {
        val pricingPhaseList = pricingPhases
        this.pricingPhases = ArrayList()
        if (pricingPhaseList != null) {
            for (pricingPhase in pricingPhaseList) {
                val newPricingPhase = createPricingPhase(pricingPhase)
                this.pricingPhases.add(newPricingPhase)
            }
        }
    }

    fun getPricingPhases(): List<PricingPhases> {
        return pricingPhases
    }

    private fun createPricingPhase(pricingPhase: PricingPhase): PricingPhases {
        return PricingPhases(
            pricingPhase.formattedPrice,
            pricingPhase.priceAmountMicros,
            pricingPhase.priceCurrencyCode,
            pricingPhase.billingPeriod,
            pricingPhase.billingCycleCount,
            pricingPhase.recurrenceMode
        )
    }

    inner class PricingPhases(
        val formattedPrice: String,
        val priceAmountMicros: Long,
        val priceCurrencyCode: String,
        val billingPeriod: String,
        val billingCycleCount: Int,
        val recurrenceMode: Int
    )
}
