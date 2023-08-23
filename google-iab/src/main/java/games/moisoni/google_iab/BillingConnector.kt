package games.moisoni.google_iab

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.common.collect.ImmutableList
import games.moisoni.google_iab.enums.ErrorType
import games.moisoni.google_iab.enums.ProductType
import games.moisoni.google_iab.enums.PurchasedResult
import games.moisoni.google_iab.enums.SkuProductType
import games.moisoni.google_iab.enums.SupportState
import games.moisoni.google_iab.models.BillingResponse
import games.moisoni.google_iab.models.ProductInfo
import games.moisoni.google_iab.models.PurchaseInfo


class BillingConnector(context: Context, base64Key: String) {
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private val base64Key: String
    private var billingClient: BillingClient? = null
    private var billingEventListener: BillingEventListener? = null
    private var consumableIds: List<String>? = null
    private var nonConsumableIds: List<String>? = null
    private var subscriptionIds: List<String>? = null
    private val allProductList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
    private val fetchedProductInfoList: MutableList<ProductInfo> = ArrayList()
    private val purchasedProductsList: MutableList<PurchaseInfo> = ArrayList()
    private var shouldAutoAcknowledge = false
    private var shouldAutoConsume = false
    private var shouldEnableLogging = false
    private var isConnected = false
    private var fetchedPurchasedProducts = false

    /**
     * BillingConnector public constructor
     *
     * @param context   - is the application context
     * @param base64Key - is the public developer key from Play Console
     */
    init {
        init(context)
        this.base64Key = base64Key
    }

    /**
     * To initialize BillingConnector
     */
    private fun init(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener { billingResult: BillingResult, purchases: List<Purchase>? ->
                when (billingResult.responseCode) {
                    BillingResponseCode.OK -> if (purchases != null) {
                        processPurchases(ProductType.COMBINED, purchases, false)
                    }

                    BillingResponseCode.USER_CANCELED -> {
                        Log("User pressed back or canceled a dialog." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.USER_CANCELED, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.SERVICE_UNAVAILABLE -> {
                        Log("Network connection is down." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.SERVICE_UNAVAILABLE, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.BILLING_UNAVAILABLE -> {
                        Log("Billing API version is not supported for the type requested." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.BILLING_UNAVAILABLE, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.ITEM_UNAVAILABLE -> {
                        Log("Requested product is not available for purchase." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.ITEM_UNAVAILABLE, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.DEVELOPER_ERROR -> {
                        Log("Invalid arguments provided to the API." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.DEVELOPER_ERROR, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.ERROR -> {
                        Log("Fatal error during the API action." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.ERROR, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Log("Failure to purchase since item is already owned." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.ITEM_ALREADY_OWNED, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.ITEM_NOT_OWNED -> {
                        Log("Failure to consume since item is not owned." + " Response code: " + billingResult.responseCode)
                        findUiHandler().post {
                            billingEventListener!!.onBillingError(
                                this@BillingConnector,
                                BillingResponse(ErrorType.ITEM_NOT_OWNED, billingResult)
                            )
                        }
                    }

                    BillingResponseCode.SERVICE_DISCONNECTED, BillingResponseCode.SERVICE_TIMEOUT -> Log(
                        "Initialization error: service disconnected/timeout. Trying to reconnect..."
                    )

                    else -> Log(
                        "Initialization error: " + BillingResponse(
                            ErrorType.BILLING_ERROR,
                            billingResult
                        )
                    )
                }
            }
            .build()
    }

    /**
     * To attach an event listener to establish a bridge with the caller
     */
    fun setBillingEventListener(billingEventListener: BillingEventListener?) {
        this.billingEventListener = billingEventListener
    }

    /**
     * To set consumable products ids
     */
    fun setConsumableIds(consumableIds: List<String>?): BillingConnector {
        this.consumableIds = consumableIds
        return this
    }

    /**
     * To set non-consumable products ids
     */
    fun setNonConsumableIds(nonConsumableIds: List<String>?): BillingConnector {
        this.nonConsumableIds = nonConsumableIds
        return this
    }

    /**
     * To set subscription products ids
     */
    fun setSubscriptionIds(subscriptionIds: List<String>?): BillingConnector {
        this.subscriptionIds = subscriptionIds
        return this
    }

    /**
     * To auto acknowledge the purchase
     */
    fun autoAcknowledge(): BillingConnector {
        shouldAutoAcknowledge = true
        return this
    }

    /**
     * To auto consume the purchase
     */
    fun autoConsume(): BillingConnector {
        shouldAutoConsume = true
        return this
    }

    /**
     * To enable logging for debugging
     */
    fun enableLogging(): BillingConnector {
        shouldEnableLogging = true
        return this
    }

    /**
     * Returns the state of the billing client
     */
    val isReady: Boolean
        get() {
            if (!isConnected) {
                Log("Billing client is not ready because no connection is established yet")
            }
            if (!billingClient!!.isReady) {
                Log("Billing client is not ready yet")
            }
            return isConnected && billingClient!!.isReady && !fetchedProductInfoList.isEmpty()
        }

    /**
     * Returns a boolean state of the product
     *
     * @param productId - is the product id that has to be checked
     */
    private fun checkProductBeforeInteraction(productId: String?): Boolean {
        if (!isReady) {
            findUiHandler().post {
                billingEventListener!!.onBillingError(
                    this@BillingConnector, BillingResponse(
                        ErrorType.CLIENT_NOT_READY,
                        "Client is not ready yet", defaultResponseCode
                    )
                )
            }
        } else if (productId != null && fetchedProductInfoList.none { it: ProductInfo -> it.product == productId }) {
            findUiHandler().post {
                billingEventListener!!.onBillingError(
                    this@BillingConnector, BillingResponse(
                        ErrorType.PRODUCT_NOT_EXIST,
                        "The product id: $productId doesn't seem to exist on Play Console",
                        defaultResponseCode
                    )
                )
            }
        } else return isReady
        return false
    }

    /**
     * To connect the billing client with Play Console
     */
    fun connect(): BillingConnector {
        val productInAppList: MutableList<QueryProductDetailsParams.Product> = ArrayList()
        val productSubsList: MutableList<QueryProductDetailsParams.Product> = ArrayList()

        //set empty list to null so we only have to deal with lists that are null or not empty
        if (consumableIds == null || consumableIds!!.isEmpty()) {
            consumableIds = null
        } else {
            for (id in consumableIds!!) {
                productInAppList.add(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(
                        BillingClient.ProductType.INAPP
                    ).build()
                )
            }
        }
        if (nonConsumableIds == null || nonConsumableIds!!.isEmpty()) {
            nonConsumableIds = null
        } else {
            for (id in nonConsumableIds!!) {
                productInAppList.add(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(
                        BillingClient.ProductType.INAPP
                    ).build()
                )
            }
        }
        if (subscriptionIds == null || subscriptionIds!!.isEmpty()) {
            subscriptionIds = null
        } else {
            for (id in subscriptionIds!!) {
                productSubsList.add(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(
                        BillingClient.ProductType.SUBS
                    ).build()
                )
            }
        }
        allProductList.addAll(productInAppList)
        allProductList.addAll(productSubsList)

        //check if any list is provided
        require(allProductList.isNotEmpty()) { "At least one list of consumables, non-consumables or subscriptions is needed" }

        //check for duplicates product ids
        val allIdsSize = allProductList.size
        val allIdsSizeDistinct = allProductList.distinct().count()
        require(allIdsSize == allIdsSizeDistinct) { "The product id must appear only once in a list. Also, it must not be in different lists" }
        Log("Billing service: connecting...")
        if (!billingClient!!.isReady) {
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    isConnected = false
                    findUiHandler().post {
                        billingEventListener!!.onBillingError(
                            this@BillingConnector, BillingResponse(
                                ErrorType.CLIENT_DISCONNECTED,
                                "Billing service: disconnected", defaultResponseCode
                            )
                        )
                    }
                    Log("Billing service: Trying to reconnect...")
                    retryBillingClientConnection()
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnected = false
                    when (billingResult.responseCode) {
                        BillingResponseCode.OK -> {
                            isConnected = true
                            Log("Billing service: connected")

                            //query consumable and non-consumable product details
                            if (!productInAppList.isEmpty()) {
                                queryProductDetails(
                                    BillingClient.ProductType.INAPP,
                                    productInAppList
                                )
                            }

                            //query subscription product details
                            if (subscriptionIds != null) {
                                queryProductDetails(BillingClient.ProductType.SUBS, productSubsList)
                            }
                        }

                        BillingResponseCode.BILLING_UNAVAILABLE -> {
                            Log("Billing service: unavailable")
                            retryBillingClientConnection()
                        }

                        else -> {
                            Log("Billing service: error")
                            retryBillingClientConnection()
                        }
                    }
                }
            })
        }
        return this
    }

    /**
     * Retries the billing client connection with exponential backoff
     * Max out at the time specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS (15 minutes)
     */
    private fun retryBillingClientConnection() {
        findUiHandler().postDelayed({ connect() }, reconnectMilliseconds)
        reconnectMilliseconds =
            Math.min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
    }

    /**
     * Fires a query in Play Console to show products available to purchase
     */
    private fun queryProductDetails(
        productType: String,
        productList: List<QueryProductDetailsParams.Product>,
    ) {
        val productDetailsParams =
            QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient!!.queryProductDetailsAsync(productDetailsParams) { billingResult: BillingResult, productDetailsList: List<ProductDetails> ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                if (productDetailsList.isEmpty()) {
                    Log("Query Product Details: data not found. Make sure product ids are configured on Play Console")
                    findUiHandler().post {
                        billingEventListener!!.onBillingError(
                            this@BillingConnector, BillingResponse(
                                ErrorType.BILLING_ERROR,
                                "No product found", defaultResponseCode
                            )
                        )
                    }
                } else {
                    Log("Query Product Details: data found")
                    val fetchedProductInfo = productDetailsList.map {  generateProductInfo(it) }.toMutableList()

                    fetchedProductInfoList.addAll(fetchedProductInfo)
                    when (productType) {
                        BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS -> findUiHandler().post {
                            billingEventListener!!.onProductsFetched(
                                fetchedProductInfo
                            )
                        }

                        else -> throw IllegalStateException("Product type is not implemented")
                    }
                    val fetchedProductIds = fetchedProductInfo.map { it.product }

                    val productListIds = productList.map { it.zza() } //according to the documentation "zza" is the product id
                    val isFetched = fetchedProductIds.any { o: String -> productListIds.contains(o) }
                    if (isFetched) {
                        fetchPurchasedProducts()
                    }
                }
            } else {
                Log("Query Product Details: failed")
                findUiHandler().post {
                    billingEventListener!!.onBillingError(
                        this@BillingConnector, BillingResponse(
                            ErrorType.BILLING_ERROR, billingResult
                        )
                    )
                }
            }
        }
    }

    /**
     * Returns a new ProductInfo object containing the product type and product details
     *
     * @param productDetails - is the object provided by the billing client API
     */
    private fun generateProductInfo(productDetails: ProductDetails): ProductInfo {
        val skuProductType: SkuProductType = when (productDetails.productType) {
            BillingClient.ProductType.INAPP -> {
                val consumable = isProductIdConsumable(productDetails.productId)
                if (consumable) {
                    SkuProductType.CONSUMABLE
                } else {
                    SkuProductType.NON_CONSUMABLE
                }
            }

            BillingClient.ProductType.SUBS -> SkuProductType.SUBSCRIPTION
            else -> throw IllegalStateException("Product type is not implemented correctly")
        }
        return ProductInfo(skuProductType, productDetails)
    }

    private fun isProductIdConsumable(productId: String): Boolean {
        return if (consumableIds == null) {
            false
        } else consumableIds!!.contains(productId)
    }

    /**
     * Returns purchases details for currently owned items without a network request
     */
    private fun fetchPurchasedProducts() {
        if (billingClient!!.isReady) {
            billingClient!!.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ) { billingResult: BillingResult, purchases: List<Purchase> ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    if (purchases.isEmpty()) {
                        Log("Query IN-APP Purchases: the list is empty")
                    } else {
                        Log("Query IN-APP Purchases: data found and progress")
                    }
                    processPurchases(ProductType.INAPP, purchases, true)
                } else {
                    Log("Query IN-APP Purchases: failed")
                }
            }

            //query subscription purchases for supported devices
            if (isSubscriptionSupported === SupportState.SUPPORTED) {
                billingClient!!.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                        .build()
                ) { billingResult: BillingResult, purchases: List<Purchase> ->
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        if (purchases.isEmpty()) {
                            Log("Query SUBS Purchases: the list is empty")
                        } else {
                            Log("Query SUBS Purchases: data found and progress")
                        }
                        processPurchases(ProductType.SUBS, purchases, true)
                    } else {
                        Log("Query SUBS Purchases: failed")
                    }
                }
            }
        } else {
            findUiHandler().post {
                billingEventListener!!.onBillingError(
                    this@BillingConnector, BillingResponse(
                        ErrorType.FETCH_PURCHASED_PRODUCTS_ERROR,
                        "Billing client is not ready yet", defaultResponseCode
                    )
                )
            }
        }
    }

    /**
     * Before using subscriptions, device-support must be checked
     * Not all devices support subscriptions
     */
    val isSubscriptionSupported: SupportState
        get() {
            val response = billingClient!!.isFeatureSupported(FeatureType.SUBSCRIPTIONS)
            return when (response.responseCode) {
                BillingResponseCode.OK -> {
                    Log("Subscriptions support check: success")
                    SupportState.SUPPORTED
                }

                BillingResponseCode.SERVICE_DISCONNECTED -> {
                    Log("Subscriptions support check: disconnected. Trying to reconnect...")
                    SupportState.DISCONNECTED
                }

                else -> {
                    Log("Subscriptions support check: error -> " + response.responseCode + " " + response.debugMessage)
                    SupportState.NOT_SUPPORTED
                }
            }
        }

    /**
     * Checks purchases signature for more security
     */
    private fun processPurchases(
        productType: ProductType,
        allPurchases: List<Purchase>,
        purchasedProductsFetched: Boolean,
    ) {
        val signatureValidPurchases: MutableList<PurchaseInfo> = ArrayList()

        //create a list with signature valid purchases
        val validPurchases = allPurchases.filter { purchase: Purchase -> isPurchaseSignatureValid(purchase) }
        for (purchase in validPurchases) {

            //query all products as a list
            val purchasesProducts = purchase.products

            //loop through all products and progress for each product individually
            for (i in purchasesProducts.indices) {
                val purchaseProduct = purchasesProducts[i]
                val productInfo =
                    fetchedProductInfoList.firstOrNull { it: ProductInfo -> it.product == purchaseProduct }

                productInfo?.let {
                    val productDetails = it.productDetails
                    val purchaseInfo = PurchaseInfo(generateProductInfo(productDetails), purchase)
                    signatureValidPurchases.add(purchaseInfo)
                }
            }
        }
        if (purchasedProductsFetched) {
            fetchedPurchasedProducts = true
            findUiHandler().post {
                billingEventListener!!.onPurchasedProductsFetched(
                    productType,
                    signatureValidPurchases
                )
            }
        } else {
            findUiHandler().post {
                billingEventListener!!.onProductsPurchased(
                    signatureValidPurchases
                )
            }
        }
        purchasedProductsList.addAll(signatureValidPurchases)
        for (purchaseInfo in signatureValidPurchases) {
            if (shouldAutoConsume) {
                consumePurchase(purchaseInfo)
            }
            if (shouldAutoAcknowledge) {
                val isProductConsumable = purchaseInfo.skuProductType === SkuProductType.CONSUMABLE
                if (!isProductConsumable) {
                    acknowledgePurchase(purchaseInfo)
                }
            }
        }
    }

    /**
     * Consume consumable products so that the user can buy the item again
     *
     *
     * Consumable products might be bought/consumed by users multiple times (for eg. diamonds, coins etc)
     * They have to be consumed within 3 days otherwise Google will refund the products
     */
    fun consumePurchase(purchaseInfo: PurchaseInfo) {
        if (checkProductBeforeInteraction(purchaseInfo.product)) {
            if (purchaseInfo.skuProductType === SkuProductType.CONSUMABLE) {
                if (purchaseInfo.purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchaseInfo.purchase.purchaseToken).build()
                    billingClient!!.consumeAsync(consumeParams) { billingResult: BillingResult, purchaseToken: String? ->
                        if (billingResult.responseCode == BillingResponseCode.OK) {
                            purchasedProductsList.remove(purchaseInfo)
                            findUiHandler().post {
                                billingEventListener!!.onPurchaseConsumed(
                                    purchaseInfo
                                )
                            }
                        } else {
                            Log("Handling consumables: error during consumption attempt: " + billingResult.debugMessage)
                            findUiHandler().post {
                                billingEventListener!!.onBillingError(
                                    this@BillingConnector,
                                    BillingResponse(ErrorType.CONSUME_ERROR, billingResult)
                                )
                            }
                        }
                    }
                } else if (purchaseInfo.purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log(
                        "Handling consumables: purchase can not be consumed because the state is PENDING. " +
                                "A purchase can be consumed only when the state is PURCHASED"
                    )
                    findUiHandler().post {
                        billingEventListener!!.onBillingError(
                            this@BillingConnector, BillingResponse(
                                ErrorType.CONSUME_WARNING,
                                "Warning: purchase can not be consumed because the state is PENDING. Please consume the purchase later",
                                defaultResponseCode
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Acknowledge non-consumable products & subscriptions
     *
     *
     * This will avoid refunding for these products to users by Google
     */
    fun acknowledgePurchase(purchaseInfo: PurchaseInfo) {
        if (checkProductBeforeInteraction(purchaseInfo.product)) {
            when (purchaseInfo.skuProductType) {
                SkuProductType.NON_CONSUMABLE, SkuProductType.SUBSCRIPTION -> if (purchaseInfo.purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchaseInfo.purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchaseInfo.purchase.purchaseToken).build()
                        billingClient!!.acknowledgePurchase(acknowledgePurchaseParams) { billingResult: BillingResult ->
                            if (billingResult.responseCode == BillingResponseCode.OK) {
                                findUiHandler().post {
                                    billingEventListener!!.onPurchaseAcknowledged(
                                        purchaseInfo
                                    )
                                }
                            } else {
                                Log("Handling acknowledges: error during acknowledgment attempt: " + billingResult.debugMessage)
                                findUiHandler().post {
                                    billingEventListener!!.onBillingError(
                                        this@BillingConnector,
                                        BillingResponse(ErrorType.ACKNOWLEDGE_ERROR, billingResult)
                                    )
                                }
                            }
                        }
                    }
                } else if (purchaseInfo.purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log(
                        "Handling acknowledges: purchase can not be acknowledged because the state is PENDING. " +
                                "A purchase can be acknowledged only when the state is PURCHASED"
                    )
                    findUiHandler().post {
                        billingEventListener!!.onBillingError(
                            this@BillingConnector, BillingResponse(
                                ErrorType.ACKNOWLEDGE_WARNING,
                                "Warning: purchase can not be acknowledged because the state is PENDING. Please acknowledge the purchase later",
                                defaultResponseCode
                            )
                        )
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * Called to purchase a non-consumable/consumable product
     */
    fun purchase(activity: Activity, productId: String) {
        purchase(activity, productId, 0)
    }

    /**
     * Called to purchase a non-consumable/consumable product
     *
     *
     * The offer index represents the different offers in the subscription.
     */
    private fun purchase(activity: Activity, productId: String, selectedOfferIndex: Int) {
        if (checkProductBeforeInteraction(productId)) {
            val productInfo = fetchedProductInfoList.firstOrNull { it.product == productId }

            productInfo?.let {
                val productDetails = it.productDetails
                val productDetailsParamsList: ImmutableList<ProductDetailsParams> =
                    if (productDetails.productType == BillingClient.ProductType.SUBS && productDetails.subscriptionOfferDetails != null) {
                        //the offer index represents the different offers in the subscription
                        //offer index is only available for subscriptions starting with Google Billing v5+
                        ImmutableList.of(
                            ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(productDetails.subscriptionOfferDetails!![selectedOfferIndex].offerToken)
                                .build()
                        )
                    } else {
                        ImmutableList.of(
                            ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    }
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient!!.launchBillingFlow(activity, billingFlowParams)
            }
            if (productInfo == null) {
                Log("Billing client can not launch billing flow because product details are missing")
            }
        }
    }

    /**
     * Called to purchase a subscription with offers
     *
     *
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     *
     *
     * For subscription with only one base package, use subscribe(activity, productId) method or selectedOfferIndex = 0
     */
    fun subscribe(activity: Activity, productId: String, selectedOfferIndex: Int) {
        purchase(activity, productId, selectedOfferIndex)
    }

    /**
     * Called to purchase a simple subscription
     *
     *
     * To avoid confusion while trying to purchase a subscription
     * Does the same thing as purchase() method
     *
     *
     * For subscription with multiple offers, use subscribe(activity, productId, selectedOfferIndex) method
     */
    fun subscribe(activity: Activity, productId: String) {
        purchase(activity, productId)
    }

    /**
     * Called to cancel a subscription
     */
    fun unsubscribe(activity: Activity, productId: String) {
        try {
            val subscriptionUrl =
                "http://play.google.com/store/account/subscriptions?package=" + activity.packageName + "&sku=" + productId
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(subscriptionUrl)
            activity.startActivity(intent)
            activity.finish()
        } catch (e: Exception) {
            Log("Handling subscription cancellation: error while trying to unsubscribe")
            e.printStackTrace()
        }
    }

    /**
     * Checks purchase state synchronously
     */
    fun isPurchased(productInfo: ProductInfo): PurchasedResult {
        return checkPurchased(productInfo.product)
    }

    private fun checkPurchased(productId: String): PurchasedResult {
        return if (!isReady) {
            PurchasedResult.CLIENT_NOT_READY
        } else if (!fetchedPurchasedProducts) {
            PurchasedResult.PURCHASED_PRODUCTS_NOT_FETCHED_YET
        } else {
            for (purchaseInfo in purchasedProductsList) {
                if (purchaseInfo.product == productId) {
                    return PurchasedResult.YES
                }
            }
            PurchasedResult.NO
        }
    }

    /**
     * Checks purchase signature validity
     */
    private fun isPurchaseSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(base64Key, purchase.originalJson, purchase.signature)
    }

    /**
     * Returns the main thread for operations that need to be executed on the UI thread
     *
     *
     * BillingEventListener runs on it
     */
    private fun findUiHandler(): Handler {
        return Handler(Looper.getMainLooper())
    }

    /**
     * To print a log while debugging BillingConnector
     */
    private fun Log(debugMessage: String) {
        if (shouldEnableLogging) {
            android.util.Log.d(TAG, debugMessage)
        }
    }

    /**
     * Called to release the BillingClient instance
     *
     *
     * To avoid leaks this method should be called when BillingConnector is no longer needed
     */
    fun release() {
        if (billingClient != null && billingClient!!.isReady) {
            Log("BillingConnector instance release: ending connection...")
            billingClient!!.endConnection()
        }
    }

    companion object {
        private const val TAG = "BillingConnector"
        private const val defaultResponseCode = 99
        private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L
    }
}