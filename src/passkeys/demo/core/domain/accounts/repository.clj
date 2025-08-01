(ns passkeys.demo.core.domain.accounts.repository
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [passkeys.demo.core.cache.impl :as cache]
   [passkeys.demo.core.database.impl :as db]
   [passkeys.demo.core.domain.accounts.sql :as sql]
   [passkeys.demo.core.errors.impl :refer [throw-unauthorized-exception]]
   [passkeys.demo.core.security.authentication-errors :as authentication-errors]
   [passkeys.demo.system.constants :refer [passkeys-demo-passkey-authentication-request passkeys-demo-passkey-registration-request]])
  (:import
   [com.yubico.webauthn AssertionRequest AssertionResult CredentialRepository RegisteredCredential RegistrationResult]
   [com.yubico.webauthn.data ByteArray PublicKeyCredentialCreationOptions PublicKeyCredentialDescriptor]
   [java.util Collection HashSet Optional]))

(set! *warn-on-reflection* true)

(defn get-account-by-email
  [email {{:keys [passkeys-demo-db]} :components :as system}]
  (log/debugf "Get account using email '%s'." email)
  (if-let [result (->> (sql/get-account-by-email email)
                       (db/select passkeys-demo-db))]
    (do (log/debugf "Got account using email '%s'." email)
        result)
    (log/warnf "No account exists with email '%s'." email)))

(defn get-account-by-user-handle
  [user-handle {{:keys [passkeys-demo-db]} :components :as system}]
  (log/debugf "Get account using user handle '%s'." user-handle)
  (if-let [result (->> (sql/get-account-by-user-handle user-handle)
                       (db/select passkeys-demo-db))]
    (do (log/debugf "Got account using user handle '%s'." user-handle)
        result)
    (log/warnf "No account exists with user handle '%s'." user-handle)))

(defn save-passkey-registration-request-options
  [registration-id ^PublicKeyCredentialCreationOptions registration-request-options {{{:keys [connection ttl-seconds]} :passkeys-cache} :components :as system}]
  (let [registration-request-options-json (.toJson registration-request-options)]
    (-> (str passkeys-demo-passkey-registration-request registration-id)
        (cache/redis-put registration-request-options-json {:expiry-seconds ttl-seconds} connection))
    (log/debugf "Saved passkey registration request options for registration id '%s' in the passkeys cache." registration-id)))

(defn get-passkey-registration-request-options
  [registration-id {{{:keys [connection]} :passkeys-cache} :components :as system}]
  (when-let [registration-request-options (some-> (str passkeys-demo-passkey-registration-request registration-id)
                                                  (cache/redis-get connection))]
    (log/tracef "Found passkey registration request options matching registration id '%s' in the passkeys cache." registration-id)
    (PublicKeyCredentialCreationOptions/fromJson registration-request-options)))

(defn save-passkey-authentication-request-options
  [authentication-id ^AssertionRequest authentication-request-options {{{:keys [connection ttl-seconds]} :passkeys-cache} :components :as system}]
  (let [authentication-request-options-json (.toJson authentication-request-options)]
    (-> (str passkeys-demo-passkey-authentication-request authentication-id)
        (cache/redis-put authentication-request-options-json {:expiry-seconds ttl-seconds} connection))
    (log/debugf "Saved passkey authentication request options for authentication id '%s' in the passkeys cache." authentication-id)))

(defn get-passkey-authentication-request-options
  [authentication-id {{{:keys [connection]} :passkeys-cache} :components :as system}]
  (when-let [authentication-request-options (some-> (str passkeys-demo-passkey-authentication-request authentication-id)
                                                    (cache/redis-get connection))]
    (log/tracef "Found passkey authentication request options matching authentication id '%s' in the passkeys cache." authentication-id)
    (AssertionRequest/fromJson authentication-request-options)))

(defn save-passkey-credential
  [registration-id {{:account/keys [email]} :principal :as account} ^PublicKeyCredentialCreationOptions registration-request-options ^RegistrationResult registration-result system]
  (let [{{{:keys [connection]} :passkeys-cache :keys [passkeys-demo-db]} :components} system]
    (log/debugf "Saving passkey credentials for '%s'." email)
    (let [result (->> (sql/insert-credential registration-request-options registration-result)
                      (db/execute passkeys-demo-db))]
      (-> (str passkeys-demo-passkey-registration-request registration-id)
          (cache/redis-del connection))
      (log/debugf "Saved passkey credentials for '%s' and removed registration request from the passkeys cache." email)
      result)))

(defn update-passkey-credential
  [authentication-id {{:account/keys [email]} :principal :as account} ^AssertionResult assertion-result system]
  (let [{{{:keys [connection]} :passkeys-cache :keys [passkeys-demo-db]} :components} system
        credential-id (-> assertion-result .getCredential .getCredentialId .getHex)]
    (log/debugf "Updating passkey credentials for '%s'." email)
    (when-let [result (->> (sql/update-credential credential-id assertion-result)
                           (db/execute passkeys-demo-db))]
      (-> (str passkeys-demo-passkey-authentication-request authentication-id)
          (cache/redis-del connection))
      (log/debugf "Updated passkey credential for '%s' and removed authentication request from the passkeys cache." email)
      result)))

(defn ^:private build-credential
  ^RegisteredCredential [{:credentials/keys [credential-id public-key-cose signature-count] :account/keys [user-handle] :as result}]
  (-> (RegisteredCredential/builder)
      (.credentialId (ByteArray/fromHex credential-id))
      (.userHandle (ByteArray/fromHex user-handle))
      (.publicKeyCose (ByteArray/fromHex public-key-cose))
      (.signatureCount signature-count)
      (.build)))

(defn ^:private build-credential-id
  ^PublicKeyCredentialDescriptor [{:credentials/keys [credential-id] :as result}]
  (-> (PublicKeyCredentialDescriptor/builder)
      (.id (ByteArray/fromHex credential-id))
      (.build)))

(defn get-credential-repository
  [{{:keys [passkeys-demo-db]} :components :as system}]
  (reify CredentialRepository
    (getCredentialIdsForUsername [_this email]
      (log/debugf "Looking up credential id(s) belonging to '%s'." email)
      (if-let [results (seq (->> (sql/get-credential-ids-for-email email)
                                 (db/select-many passkeys-demo-db)))]
        (do (log/debugf "Got '%d' credential id(s) belonging to '%s'." (count results) email)
            (->> results
                 (map build-credential-id)
                 (^[Collection] HashSet/new)))
        (do (log/debugf "No credential id(s) found belonging to '%s'." email)
            (HashSet/new))))
    (getUserHandleForUsername [_this email]
      (log/debugf "Looking up user handle belonging to '%s'." email)
      (if-let [{:account/keys [user-handle] :as result} (->> (sql/get-user-handle-for-email email)
                                                             (db/select passkeys-demo-db))]
        (do (log/debugf "Got user handle '%s' belonging to '%s'." user-handle email)
            (Optional/of (ByteArray/fromHex user-handle)))
        (do (log/debugf "No user handle found belonging to email '%s'." email)
            (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials))))
    (getUsernameForUserHandle [_this user-handle]
      (let [user-handle' (.getHex user-handle)]
        (log/debugf "Looking up email belonging to user handle '%s'." user-handle')
        (if-let [{:account/keys [email] :as result} (->> (sql/get-email-for-user-handle user-handle')
                                                         (db/select passkeys-demo-db))]
          (do (log/debugf "Got email '%s' belonging to user handle '%s'." email user-handle')
              (Optional/of email))
          (do (log/debugf "No email found belonging to user handle '%s'." user-handle')
              (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials)))))
    (lookup [_this credential-id user-handle]
      (let [credential-id' (.getHex credential-id)
            user-handle' (.getHex user-handle)]
        (log/debugf "Looking up credential with credential id '%s' associated with (account) user handle '%s'." credential-id' user-handle')
        (if-let [{:credentials/keys [credential-id] :account/keys [email] :as result} (->> (sql/lookup-credential-and-account credential-id' user-handle')
                                                                                           (db/select passkeys-demo-db))]
          (do (log/debugf "Found credential id '%s' associated with (account) user handle '%s'. It belongs to '%s'." credential-id user-handle' email)
              (Optional/of (build-credential result)))
          (do (log/debugf "No credential found matching credential id '%s' associated with (account) user handle '%s'." credential-id' user-handle')
              (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials)))))
    (lookupAll [_this credential-id]
      (let [credential-id' (.getHex credential-id)]
        (log/debugf "Looking up all credentials matching credential id '%s'. There should only be 0 or 1 result(s) returned." credential-id')
        (if-let [results (seq (->> (sql/lookup-all-credentials credential-id')
                                   (db/select passkeys-demo-db)))]
          ;; this every only return 0 or 1 result (according to the CredentialRepository javadoc)
          (do (log/debugf "Got '%d' credential(s) matching credential id '%s'." (count results) credential-id')
              (->> results
                   (map :credentials/credential-id)
                   (map #(ByteArray/fromHex %))
                   (^[Collection] HashSet/new)))
          (do (log/debugf "No credential(s) found matching credential id '%s'." credential-id')
              (HashSet/new)))))))
