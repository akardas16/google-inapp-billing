package games.moisoni.google_inapp_billing

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import games.moisoni.google_iab.BillingConnector
import games.moisoni.google_iab.BillingEventListener
import games.moisoni.google_iab.enums.ErrorType
import games.moisoni.google_iab.enums.ProductType
import games.moisoni.google_iab.models.BillingResponse
import games.moisoni.google_iab.models.ProductInfo
import games.moisoni.google_iab.models.PurchaseInfo

/**
 * This is an example of how to implement a one-time product purchase
 * Below you'll see a simple "remove ads button" scenario
 *
 *
 * Following this logic, you'll be able to integrate any one-time product purchase or subscriptions
 *
 *
 * We have a boolean variable "userPrefersAdFree" that will be `true` only when the API successfully acknowledged the purchase
 * The state of the variable will be saved using SharedPreferences so we can retrieve it in other activities/fragments
 * Before showing the ads, we'll always check the value of the variable and proceed only if its value is set to `false`
 *
 *
 * The logic is simple and should be self-explanatory
 */
class RemoveAdsExampleActivity : AppCompatActivity() {
    private var billingConnector: BillingConnector? = null

    //this is the variable in which we'll store the status of the purchase
    //once we'll have the data stored, we can retrieve it in any activity or fragment,
    //to update the code and the UI accordingly to the user purchase
    private var userPrefersAdFree = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_ads_example)
        loadUserPreferences()
        initializeBillingClient()
        removeAds()
    }

    private fun loadUserPreferences() {
        //here we are loading the data into our variable
        //it's very important to call this before trying to access the variable so you'll have the correct status of the purchase
        //notice this is the first thing called in the "onCreate" method
        userPrefersAdFree = SharedPrefsHelper.getBoolean("userPrefersAdFree", false)
    }

    private fun initializeBillingClient() {
        val nonConsumableIds: MutableList<String> = ArrayList()
        nonConsumableIds.add(getString(R.string.remove_ads_play_console_id))
        billingConnector = BillingConnector(this, getString(R.string.license_key_play_console))
            .setNonConsumableIds(nonConsumableIds)
            .autoAcknowledge()
            .enableLogging()
            .connect()
        billingConnector!!.setBillingEventListener(object : BillingEventListener {
            override fun onProductsFetched(productDetails: List<ProductInfo>) {}

            //this IS the listener in which we can restore previous purchases
            override fun onPurchasedProductsFetched(
                productType: ProductType,
                purchases: List<PurchaseInfo>
            ) {
                var purchasedProduct: String
                var isAcknowledged: Boolean
                for (purchaseInfo in purchases) {
                    purchasedProduct = purchaseInfo!!.product
                    isAcknowledged = purchaseInfo.isAcknowledged
                    if (!userPrefersAdFree) {
                        if (purchasedProduct.equals(
                                getString(R.string.remove_ads_play_console_id),
                                ignoreCase = true
                            )
                        ) {
                            if (isAcknowledged) {

                                //here we are saving the purchase status into our "userPrefersAdFree" variable
                                SharedPrefsHelper.putBoolean("userPrefersAdFree", true)
                                Toast.makeText(
                                    this@RemoveAdsExampleActivity,
                                    "The previous purchase was successfully restored.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

            //this IS NOT the listener in which we'll give user entitlement for purchases (see ReadMe.md why)
            override fun onProductsPurchased(purchases: List<PurchaseInfo>) {}

            //this IS the listener in which we'll give user entitlement for purchases (the ReadMe.md explains why)
            override fun onPurchaseAcknowledged(purchase: PurchaseInfo) {
                val acknowledgedProduct = purchase.product
                if (acknowledgedProduct.equals(
                        getString(R.string.remove_ads_play_console_id),
                        ignoreCase = true
                    )
                ) {

                    //here we are saving the purchase status into our "userPrefersAdFree" variable
                    SharedPrefsHelper.putBoolean("userPrefersAdFree", true)
                    Toast.makeText(
                        this@RemoveAdsExampleActivity,
                        "The purchase was successfully made.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onPurchaseConsumed(purchase: PurchaseInfo) {}
            override fun onBillingError(
                billingConnector: BillingConnector,
                response: BillingResponse
            ) {
                when (response.errorType) {
                    ErrorType.ACKNOWLEDGE_WARNING ->                         //this response will be triggered when the purchase is still PENDING
                        Toast.makeText(
                            this@RemoveAdsExampleActivity,
                            "The transaction is still pending. Please come back later to receive the purchase!",
                            Toast.LENGTH_SHORT
                        ).show()

                    ErrorType.BILLING_UNAVAILABLE, ErrorType.SERVICE_UNAVAILABLE -> Toast.makeText(
                        this@RemoveAdsExampleActivity,
                        "Billing is unavailable at the moment. Check your internet connection!",
                        Toast.LENGTH_SHORT
                    ).show()

                    ErrorType.ERROR -> Toast.makeText(
                        this@RemoveAdsExampleActivity,
                        "Something happened, the transaction was canceled!",
                        Toast.LENGTH_SHORT
                    ).show()

                    else -> {}
                }
            }
        })
    }

    private fun removeAds() {
        val removeAdsButton = findViewById<Button>(R.id.remove_ads_button)
        removeAdsButton.setOnClickListener { v: View? ->
            billingConnector!!.purchase(
                this@RemoveAdsExampleActivity,
                getString(R.string.remove_ads_play_console_id)
            )
        }
    }
}