package com.y9vad9.starix.foundation.validation.annotations

/**
 * The annotation that marks that API requires attention on what you do.
 */
@RequiresOptIn(
    message = "This is a delicate API and its use requires care."
)
public annotation class ValidationDelicateApi