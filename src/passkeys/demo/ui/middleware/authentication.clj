(ns passkeys.demo.ui.middleware.authentication
  {:author "David Harrigan"}
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.backends.httpbasic :refer [http-basic-backend]]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [buddy.auth.protocols :as proto]
   [clojure.string :refer [blank?]]
   [clojure.tools.logging :as log]
   [passkeys.demo.core.errors.impl :refer [throw-unauthorized-exception]]
   [passkeys.demo.core.security.authentication-errors :as authentication-errors]
   [passkeys.demo.core.security.impl :as security]
   [passkeys.demo.core.security.magic-links.impl :as magic-links]
   [passkeys.demo.core.security.model.principal :as principal]
   [passkeys.demo.ui.utils.cookie :as cookie-utils]))

(set! *warn-on-reflection* true)

;; BASIC

(defn ^:private basic-authentication
  [{:keys [system] :as request} {:keys [username] :as credentials}]
  (when (blank? username)
    (throw-unauthorized-exception authentication-errors/missing-or-invalid-basic-credentials))
  (log/debugf "Performing BASIC authentication for account '%s'." username)
  (security/basic-authentication credentials system))

(defn ^:private with-basic-authentication
  [handler]
  (let [authenticator (http-basic-backend {:realm "passkeys.demo" :authfn basic-authentication})]
    (wrap-authentication handler authenticator)))

(defn ^:private with-post-basic-authentication
  [handler]
  (fn [{{:keys [account]} :identity :keys [system] :as request}]
    (if (contains? #{:not-found :failed-checks} account)
      ;; Pass control back to the handler. No magic link email will be sent, but an "If the account exists..." message *will* be displayed on the webpage.
      ;; This is to guard against account enumeration. The attacker doesn't know if the account exists or not as the message will always be displayed regardless.
      (handler request)
      (do (log/debugf "Performing POST BASIC authentication for account '%s'." (principal/email account))
          (if (authenticated? request)
            (do (log/debugf "POST BASIC authentication completed for account '%s'. Credentials valid, proceeding..." (principal/email account))
                (let [magic-link-token (magic-links/create-and-send-magic-link account system)]
                  (handler (assoc request :magic-link-token magic-link-token))))
            (throw-unauthorized-exception authentication-errors/missing-or-invalid-basic-credentials))))))

;; MAGIC LINKS

(defn ^:private magic-link-backend
  [{:keys [authfn]}]
  (reify
    proto/IAuthentication
    (-parse [_ {{{:keys [token]} :query} :parameters :as request}]
      {:token token})
    (-authenticate [_ request credentials]
      (authfn request credentials))))

(defn ^:private magic-link-authentication
  [{:keys [system] :as request} {:keys [token] :as credentials}]
  (when (blank? token)
    (throw-unauthorized-exception authentication-errors/missing-or-invalid-magic-link-credentials))
  (log/debugf "Performing MAGIC LINK authentication for token '%s'." token)
  (security/magic-link-authentication credentials system))

(defn ^:private with-magic-link-authentication
  [handler]
  (let [authenticator (magic-link-backend {:authfn magic-link-authentication})]
    (wrap-authentication handler authenticator)))

(defn ^:private with-post-magic-link-authentication
  [handler]
  (fn [{{:keys [account]} :identity :as request}]
    (log/debugf "Performing POST MAGIC LINK authentication for account '%s'." (principal/email account))
    (if (authenticated? request)
      (do (log/debugf "POST MAGIC LINK authentication completed for account '%s'. Credentials valid, proceeding..." (principal/email account))
          (handler request))
      (throw-unauthorized-exception authentication-errors/missing-or-invalid-magic-link-credentials))))

;; COOKIES (I.E., SESSIONS FOR LOGGED IN ACCOUNTS)

(defn ^:private cookie-backend
  [{:keys [authfn]}]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      {:session-id (cookie-utils/get-session-cookie request)})
    (-authenticate [_ request credentials]
      (authfn request credentials))))

(defn ^:private session-cookie-authentication
  [{:keys [system] :as request} {:keys [session-id] :as credentials}]
  (when (blank? session-id)
    (throw-unauthorized-exception authentication-errors/missing-or-invalid-session-cookie-credentials))
  (log/debugf "Performing SESSION COOKIE authentication for session '%s'." session-id)
  (security/session-cookie-authentication credentials system))

(defn ^:private with-session-cookie-authentication
  [handler]
  (let [authenticator (cookie-backend {:authfn session-cookie-authentication})]
    (wrap-authentication handler authenticator)))

(defn ^:private with-post-session-cookie-authentication
  [handler]
  (fn [{{:keys [account]} :identity :as request}]
    (log/debugf "Performing POST SESSION COOKIE authentication for account '%s'." (principal/email account))
    (if (authenticated? request)
      (do (log/debugf "POST SESSION COOKIE authentication completed for account '%s'. Credentials valid, proceeding..." (principal/email account))
          (handler request))
      (throw-unauthorized-exception authentication-errors/missing-or-invalid-session-cookie-credentials))))

;; WRAPPERS

(def ^:private wrap-basic-authentication-middleware
  {:name ::basic-authentication
   :wrap with-basic-authentication})

(def ^:private wrap-post-basic-authentication-middleware
  {:name ::post-basic-authentication
   :wrap with-post-basic-authentication})

(def wrap-magic-link-authentication-middleware
  {:name ::magic-link-authentication
   :wrap with-magic-link-authentication})

(def ^:private wrap-post-magic-link-authentication-middleware
  {:name ::post-magic-link-authentication
   :wrap with-post-magic-link-authentication})

(def ^:private wrap-session-cookie-authentication-middleware
  {:name ::session-cookie-authentication
   :wrap with-session-cookie-authentication})

(def ^:private wrap-post-session-cookie-authentication-middleware
  {:name ::post-session-cookie-authentication
   :wrap with-post-session-cookie-authentication})

(def basic-authentication-middleware [wrap-basic-authentication-middleware wrap-post-basic-authentication-middleware])
(def cookie-authentication-middleware [wrap-session-cookie-authentication-middleware wrap-post-session-cookie-authentication-middleware])
(def magic-link-authentication-middleware [wrap-magic-link-authentication-middleware wrap-post-magic-link-authentication-middleware])
