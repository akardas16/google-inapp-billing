package games.moisoni.google_iab.models

import com.android.billingclient.api.BillingResult
import games.moisoni.google_iab.enums.ErrorType

class BillingResponse(val errorType: ErrorType, val debugMessage: String, val responseCode: Int) {

    constructor(errorType: ErrorType, billingResult: BillingResult) : this(
        errorType,
        billingResult.debugMessage,
        billingResult.responseCode
    )

    override fun toString(): String {
        return "BillingResponse: Error type: " + errorType +
                " Response code: " + responseCode + " Message: " + debugMessage
    }
}