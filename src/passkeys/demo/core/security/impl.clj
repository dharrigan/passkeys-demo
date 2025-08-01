(ns passkeys.demo.core.security.impl
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [passkeys.demo.core.domain.accounts.impl :as accounts]
   [passkeys.demo.core.errors.impl :as errors :refer [throw-unauthorized-exception]]
   [passkeys.demo.core.security.authentication-errors :as authentication-errors]
   [passkeys.demo.core.security.magic-links.impl :as magic-links]
   [passkeys.demo.core.security.model.principal :as principal]
   [passkeys.demo.core.session.impl :as session]
   [passkeys.demo.core.utils.text :as text-utils]))

(set! *warn-on-reflection* true)

(defn ^:private get-account-by-email
  [email system]
  (some-> (accounts/get-account-by-email email system)
          (principal/->Account)))

(defn ^:private get-account-by-user-handle
  [user-handle system]
  (some-> (accounts/get-account-by-user-handle user-handle system)
          (principal/->Account)))

(defn get-account-via-magic-link
  [{:keys [token] :as credentials} system]
  (magic-links/get-magic-link token system))

(defn ^:private get-account-via-session
  [{:keys [session-id] :as credentials} system]
  (and session-id (:account (session/get-session session-id system))))

(defn ^:private post-authentication-checks
  [{:keys [is-enabled is-account-expired is-account-locked has-credentials-expired] :as account}]
  (condp = true
    (not is-enabled) (throw-unauthorized-exception authentication-errors/account-not-enabled)
    is-account-locked (throw-unauthorized-exception authentication-errors/account-locked)
    is-account-expired (throw-unauthorized-exception authentication-errors/account-expired)
    has-credentials-expired (throw-unauthorized-exception authentication-errors/account-credentials-expired)
    account)) ;; else

(defn create-session
  ([account system] (create-session account nil system))
  ([account previous-session-id system]
   (let [id (principal/id account)
         session-id (if-not previous-session-id (text-utils/random-id) previous-session-id)
         searchable-session-id (str session-id ":" id)
         session {:session-id session-id :account account}]
     (session/save-session {:session-id searchable-session-id :session session} system))))

(defn basic-authentication
  [{:keys [username] :as credentials} system]
  (if-let [account (get-account-by-email username system)]
    (if (post-authentication-checks (principal/permissions account))
      (create-session account system)
      (do (log/warnf "Account '%s' has post-authentication checks. Denying login!" username)
          {:account :failed-checks})) ;; Let the middleware know we haven't validated the account...see middleware for more information.
    {:account :not-found})) ;; Let the middleware know we haven't found the account...see middleware for more information.

(defn magic-link-authentication
  [{:keys [token] :as credentials} system]
  (if-let [account (get-account-via-magic-link credentials system)]
    (let [email (principal/email account)
          account' (get-account-by-email email system)] ;; have to reload the account from the database as perhaps their permissions have changed!
      (if (post-authentication-checks (principal/permissions account'))
        (create-session account' system)
        (do (log/warnf "Account '%s' has failed post-authentication checks. Denying login!" email)
            (throw-unauthorized-exception authentication-errors/missing-or-invalid-magic-link-credentials))))
    (do (log/warnf "No magic link found for token '%s'. Denying login!" token)
        (throw-unauthorized-exception authentication-errors/missing-or-invalid-magic-link-credentials))))

(defn passkey-authentication
  [{:keys [user-handle] :as credentials} system]
  (if-let [account (get-account-by-user-handle user-handle system)]
    (let [email (principal/email account)]
      (if (post-authentication-checks (principal/permissions account))
        (create-session account system)
        (do (log/warnf "Account '%s' has failed post-authentication checks. Denying login!" email)
            (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials))))
    (do (log/warnf "No account found for user handle '%s'. Denying login!" user-handle)
        (throw-unauthorized-exception authentication-errors/missing-or-invalid-webauthn-credentials))))

(defn session-cookie-authentication
  [{:keys [session-id] :as credentials} system]
  (if-let [account (get-account-via-session credentials system)]
    (let [email (principal/email account)
          account' (get-account-by-email email system)] ;; have to reload the account from the database as perhaps their permissions have changed!
      (if (post-authentication-checks (principal/permissions account'))
        (create-session account' session-id system)
        (do (log/warnf "Account '%s' has failed post-authentication checks. Denying login!" email)
            (throw-unauthorized-exception authentication-errors/missing-or-invalid-session-cookie-credentials))))
    (do (log/warnf "No session found for session id '%s'. Denying login!" session-id)
        (throw-unauthorized-exception authentication-errors/missing-or-invalid-session-cookie-credentials))))
