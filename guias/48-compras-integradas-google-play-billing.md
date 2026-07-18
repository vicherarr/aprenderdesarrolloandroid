# Guía 48 — Monetización y Compras Integradas (`Google Play Billing Library`)

Objetivo: implementar transacciones financieras seguras en Android utilizando Google Play Billing Library para la venta de suscripciones recurrentes y productos digitales dentro de la aplicación.

---

## 1. El ciclo de vida de Google Play Billing

Google exige que todas las compras de bienes digitales en apps distribuidas por Google Play utilicen exclusivamente su pasarela oficial de pagos:

1. **Conexión**: Conectar `BillingClient` con Google Play Services.
2. **Consulta**: Consultar la lista de productos/suscripciones disponibles y sus precios locales.
3. **Flujo de Pago**: Lanzar la pantalla nativa de facturación de Google Play.
4. **Verificación y Entrega**: Verificar el token de compra en tu servidor y otorgar el producto (*Acknowledge Purchase*).

---

## 2. Dependencias (`build.gradle.kts`)

```kotlin
dependencies {
    implementation("com.android.billingclient:billing-ktx:6.2.0")
}
```

---

## 3. Conexión y Compra de Productos (`BillingClient`)

```kotlin
class GestorFacturacion(private val activity: Activity) {
    private val billingClient = BillingClient.newBuilder(activity)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    procesarCompraExitosa(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()

    fun iniciarConexion(onConectado: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConectado()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Reintentar conexión
            }
        })
    }

    fun lanzarFlujoCompra(productDetails: ProductDetails) {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun procesarCompraExitosa(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        // ¡Compra confirmada! Activar funciones Premium
                    }
                }
            }
        }
    }
}
```

---

## Fuentes consultadas (18-07-2026)

- Integración de Google Play Billing en Android: <https://developer.android.com/google/play/billing/integrate>
