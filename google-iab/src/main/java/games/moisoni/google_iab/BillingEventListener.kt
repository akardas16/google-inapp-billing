package games.moisoni.google_iab

import games.moisoni.google_iab.enums.ProductType
import games.moisoni.google_iab.models.BillingResponse
import games.moisoni.google_iab.models.ProductInfo
import games.moisoni.google_iab.models.PurchaseInfo

interface BillingEventListener {
    /**
     * Callback will be triggered when products are queried for Play Console
     *
     * @param productDetails - a list with available products
     */
    fun onProductsFetched(productDetails: List<ProductInfo>)

    /**
     * Callback will be triggered when purchased products are queried from Play Console
     *
     * @param purchases   - a list with owned products
     * @param productType - the type of the product, either IN_APP or SUBS
     */
    fun onPurchasedProductsFetched(productType: ProductType, purchases: List<PurchaseInfo>)

    /**
     * Callback will be triggered when a product is purchased successfully
     *
     * @param purchases - a list with purchased products
     */
    fun onProductsPurchased(purchases: List<PurchaseInfo>)

    /**
     * Callback will be triggered when a purchase is acknowledged
     *
     * @param purchase - specifier of acknowledged purchase
     */
    fun onPurchaseAcknowledged(purchase: PurchaseInfo)

    /**
     * Callback will be triggered when a purchase is consumed
     *
     * @param purchase - specifier of consumed purchase
     */
    fun onPurchaseConsumed(purchase: PurchaseInfo)

    /**
     * Callback will be triggered when error occurs
     *
     * @param response - provides information about the error
     */
    fun onBillingError(billingConnector: BillingConnector, response: BillingResponse)
}