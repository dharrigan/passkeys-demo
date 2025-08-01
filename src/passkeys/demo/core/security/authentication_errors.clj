(ns passkeys.demo.core.security.authentication-errors
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def account-credentials-expired {:message "Credentials expired" :data {:error :account.credentials.expired}})
(def account-expired {:message "Account expired" :data {:error :account.expired}})
(def account-locked {:message "Account locked" :data {:error :account.locked}})
(def account-not-enabled {:message "Account not enabled" :data {:error :account.not.enabled}})

(def missing-or-invalid-basic-credentials {:message "Missing or invalid basic credentials" :data {:error :missing.or.invalid.basic.credentials}})
(def missing-or-invalid-magic-link-credentials {:message "Missing or invalid magic link credentials" :data {:error :missing.or.invalid.magic.link.credentials}})
(def missing-or-invalid-session-cookie-credentials {:message "Missing or invalid cookie credentials" :data {:error :missing.or.invalid.session.cookie.credentials}})
(def missing-or-invalid-webauthn-credentials {:message "Missing or invalid webauthn credentials" :data {:error :missing.or.invalid.webauthn.credentials}})
