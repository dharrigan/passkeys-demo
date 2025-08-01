(ns passkeys.demo.api.security.webauthn.passkeys
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.domain.accounts.impl :as accounts]
   [passkeys.demo.core.errors.impl :refer [throw-unauthorized-exception]]
   [passkeys.demo.core.security.authentication-errors :as authentication-errors]
   [passkeys.demo.core.security.impl :as security]
   [passkeys.demo.core.utils.text :as text-utils])
  (:import
   [com.yubico.webauthn AssertionRequest AssertionResult FinishAssertionOptions FinishRegistrationOptions RegistrationResult RelyingParty StartAssertionOptions StartRegistrationOptions]
   [com.yubico.webauthn.data AuthenticatorSelectionCriteria ByteArray PublicKeyCredentialCreationOptions RelyingPartyIdentity ResidentKeyRequirement UserIdentity UserVerificationRequirement]
   [java.security SecureRandom]))

(set! *warn-on-reflection* true)

(defn ^:private random-bytes
  []
  (let [ba (byte-array 64)]
    (-> (SecureRandom/getInstanceStrong)
        (.nextBytes ba))
    (ByteArray. ba)))

(defn ^:private relying-party-identity
  ^RelyingPartyIdentity [{{{{:keys [relying-party-id relying-party-name]} :passkeys} :webauthn} :runtime-config :as system}]
  (-> (RelyingPartyIdentity/builder)
      (.id ^String relying-party-id)
      (.name ^String relying-party-name)
      (.build)))

(defn ^:private create-user-identity
  ^UserIdentity [{{:account/keys [first-name last-name email user-handle]} :principal :as account}]
  (-> (UserIdentity/builder)
      (.name ^String email)
      (.displayName (str first-name " " last-name))
      (.id (if user-handle (ByteArray/fromHex user-handle) (random-bytes)))
      (.build)))

(defn ^:private relying-party
  ^RelyingParty [system]
  (-> (RelyingParty/builder)
      (.identity (relying-party-identity system))
      (.credentialRepository (accounts/get-credential-repository system))
      (.build)))

(defn ^:private create-registration-request-options
  [user-identity system]
  (let [authenticator-selection (-> (AuthenticatorSelectionCriteria/builder)
                                    (.residentKey ResidentKeyRequirement/REQUIRED)
                                    (.userVerification UserVerificationRequirement/PREFERRED)
                                    (.build))
        registration-options (-> (StartRegistrationOptions/builder)
                                 (.user user-identity)
                                 (.authenticatorSelection authenticator-selection)
                                 (.build))]
    (.startRegistration (relying-party system) registration-options)))

(defn registration-start
  [account system]
  (let [user-identity (create-user-identity account)
        registration-request-options (create-registration-request-options user-identity system)
        registration-id (text-utils/random-id)]
    (accounts/save-passkey-registration-request-options registration-id ^PublicKeyCredentialCreationOptions registration-request-options system)
    {:registration-id registration-id :registration-request-options (.toCredentialsCreateJson ^PublicKeyCredentialCreationOptions registration-request-options)}))

(defn registration-verify
  [account {:keys [registration-id client-response] :as registration-response} system]
  (let [^PublicKeyCredentialCreationOptions registration-request-options (accounts/get-passkey-registration-request-options registration-id system)
        options (-> (FinishRegistrationOptions/builder)
                    (.request registration-request-options)
                    (.response client-response)
                    (.build))
        ^RegistrationResult registration-result (.finishRegistration (relying-party system) options)]
    (accounts/save-passkey-credential registration-id account registration-request-options registration-result system)))

(defn ^:private create-authentication-request-options
  [system]
  (let [assertion-options (-> (StartAssertionOptions/builder) (.build))]
    (.startAssertion ^RelyingParty (relying-party system) assertion-options)))

(defn authentication-start
  [system]
  (let [authentication-id (text-utils/random-id)
        authentication-request-options (create-authentication-request-options system)]
    (accounts/save-passkey-authentication-request-options authentication-id authentication-request-options system)
    {:authentication-id authentication-id :authentication-request-options (.toCredentialsGetJson ^AssertionRequest authentication-request-options)}))

(defn authentication-verify
  [{:keys [authentication-id client-response] :as authentication-response} system]
  (let [^AssertionRequest authentication-request-options (accounts/get-passkey-authentication-request-options authentication-id system)
        options (-> (FinishAssertionOptions/builder)
                    (.request authentication-request-options)
                    (.response client-response)
                    (.build))
        ^AssertionResult assertion-result (.finishAssertion (relying-party system) options)]
    (if (.isSuccess assertion-result)
      (let [user-handle (-> assertion-result .getCredential .getUserHandle .getHex)
            {:keys [account] :as session} (security/passkey-authentication {:user-handle user-handle} system)]
        (accounts/update-passkey-credential authentication-id account assertion-result system)
        session)
      (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials))))
