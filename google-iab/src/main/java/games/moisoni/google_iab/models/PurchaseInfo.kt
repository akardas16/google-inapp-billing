package games.moisoni.google_iab.models

import com.android.billingclient.api.AccountIdentifiers
import com.android.billingclient.api.Purchase
import games.moisoni.google_iab.enums.SkuProductType

class PurchaseInfo(private val productInfo: ProductInfo, val purchase: Purchase) {

    val skuProductType: SkuProductType = productInfo.skuProductType

    val product: String = productInfo.product
    val accountIdentifiers: AccountIdentifiers? = purchase.accountIdentifiers
    val products: List<String> = purchase.products
    val orderId: String? = purchase.orderId

    val purchaseToken: String = purchase.purchaseToken
    val originalJson: String = purchase.originalJson
    val developerPayload: String = purchase.developerPayload
    val packageName: String = purchase.packageName
    val signature: String = purchase.signature
    val quantity: Int = purchase.quantity
    val purchaseState: Int = purchase.purchaseState
    val purchaseTime: Long = purchase.purchaseTime

    val isAcknowledged: Boolean = purchase.isAcknowledged
    val isAutoRenewing: Boolean = purchase.isAutoRenewing

}