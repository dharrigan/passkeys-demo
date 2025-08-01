(ns passkeys.demo.api.security.webauthn.spec
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def ^:private RegistrationVerifyRegistrationId
  [:map
   [:registration-id :string]])

(def ^:private AuthenticationVerifyAuthenticationId
  [:map
   [:authentication-id :string]])

(def registration-verify-registration-id RegistrationVerifyRegistrationId)
(def authentication-verify-authentication-id AuthenticationVerifyAuthenticationId)
