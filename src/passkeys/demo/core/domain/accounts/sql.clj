(ns passkeys.demo.core.domain.accounts.sql
  {:author "David Harrigan"}
  (:require
   [honey.sql :as sql]
   [honey.sql.helpers :as helpers :refer [from select where join insert-into values]])
  (:import
   [com.yubico.webauthn RegistrationResult AssertionResult]
   [com.yubico.webauthn.data PublicKeyCredentialCreationOptions]))

(set! *warn-on-reflection* true)

(defn get-account-by-email
  [email]
  (-> (select :*)
      (from :account)
      (where
       [:= :email email])
      sql/format))

(defn get-account-by-user-handle
  [user-handle]
  (-> (select :*)
      (from :account)
      (where
       [:= :user-handle user-handle])
      sql/format))

(defn insert-credential
  [^PublicKeyCredentialCreationOptions registration-request-options ^RegistrationResult registration-result]
  (let [is-discoverable (-> registration-result .isDiscoverable) ;; may be unknown - not true or false, simply unknown!"
        is-discoverable' (when (.isPresent is-discoverable) (-> is-discoverable .get))]
    (-> (insert-into :credentials)
        (values
         [{:user-handle (-> registration-request-options .getUser .getId .getHex)
           :credential-id (-> registration-result .getKeyId .getId .getHex)
           :public-key-cose (-> registration-result .getPublicKeyCose .getHex)
           :signature-count (-> registration-result .getSignatureCount)
           :is-user-verified (-> registration-result .isUserVerified)
           :is-discoverable is-discoverable'
           :is-backup-eligible (-> registration-result .isBackupEligible)
           :is-backed-up (.isBackedUp registration-result)}])
        sql/format)))

(defn update-credential
  [credential-id ^AssertionResult assertion-result]
  (-> (helpers/update :credentials)
      (helpers/set {:signature-count (-> assertion-result .getSignatureCount)
                    :is-user-verified (-> assertion-result .isUserVerified)
                    :is-backup-eligible (-> assertion-result .isBackupEligible)
                    :is-backed-up (-> assertion-result .isBackedUp)})
      (where
       [:= :credential-id credential-id])
      sql/format))

(defn get-credential-ids-for-email
  [email]
  (-> (select :c.credential-id)
      (from [:credentials :c])
      (join [:account :a] [:= :a.user-handle :c.user-handle])
      (where
       [:= :a.email email])
      sql/format))

(defn get-user-handle-for-email
  [email]
  (-> (select :user-handle)
      (from :account)
      (where
       [:= :email email])
      sql/format))

(defn get-email-for-user-handle
  [user-handle]
  (-> (select :email)
      (from :account)
      (where
       [:= :user-handle user-handle])
      sql/format))

(defn lookup-credential-and-account
  [credential-id user-handle]
  (-> (select :a.* :c.*)
      (from [:account :a])
      (join [:credentials :c] [:= :c.user-handle :a.user-handle])
      (where
       [:= :a.user-handle user-handle]
       [:= :c.credential-id credential-id])
      sql/format))

(defn lookup-all-credentials
  [credential-id]
  (-> (select :*)
      (from :credentials)
      (where
       [:= :credential_id credential-id])
      sql/format))
