(ns passkeys.demo.ui.middleware.exceptions
  {:author "David Harrigan"}
  (:require
   [clojure.string :refer [split]]
   [clojure.tools.logging :as log]
   [passkeys.demo.core.utils.i18n :refer [resolve-message]]
   [passkeys.demo.system.constants :refer [bad-request internal-server-error]]
   [passkeys.demo.ui.utils.htmx :as htmx-utils]
   [reitit.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(def ^:private double-colon (re-pattern "::"))

(defn ^:private split-error-message
  [error-message]
  (if (re-find double-colon error-message)
    (let [code-and-error (split error-message double-colon)]
      {:code (first code-and-error) :message (str (second code-and-error))})
    {:code nil :message error-message})) ;; This is an error we don't yet know how to handle, so just return the message with no code.

(defn ^:private format-platform-error
  [error message]
  (case error
    :account.credentials.expired message
    :account.expired message
    :account.locked message
    :account.not.enabled message
    :database.unavailable message
    :general.exception message
    :internal.server.error message
    :missing.or.invalid.basic.credentials message
    :missing.or.invalid.magic.link.credentials message
    :missing.or.invalid.session.cookie.credentials message
    :missing.or.invalid.webauthn.credentials message
    :service.unavailable message))

(defn format-ui-session-error
  [error message]
  (case error
    :401 message))

(defn handle-error
  [{:keys [locales] :as request} {:keys [error type] :as exception-data}]
  (let [resource-id (keyword (name type) (name error))
        resolved-error-message (resolve-message locales resource-id)
        {:keys [code message]} (split-error-message resolved-error-message)
        resolved-error (case type
                         :platform (format-platform-error error message)
                         :ui.session (format-ui-session-error error message))]
    {:code code :reference (random-uuid) :message resolved-error}))

(defn ^:private login-redirect
  [request]
  (if (get-in request [:headers "hx-request"])
    (-> (response/response {}) (htmx-utils/hx-redirect "/login?reason=401"))
    (response/redirect "/login")))

(defn ^:private exception-info-handler
  [exception-info request] ; exception-info and request both come from reitit.
  (let [{:keys [http-status error] :or {http-status 500} :as exception-data} (ex-data exception-info)
        body (-> (case error
                   ;;
                   ;; add other exception-info types and handlers here ↑ if appropriate
                   ;;
                   ;; finally, catch all.
                   ;;
                   (handle-error request exception-data))
                 (assoc :uri (:uri request)))]
    (case error
      :missing.or.invalid.basic.credentials (login-redirect request)
      :missing.or.invalid.magic.link.credentials (login-redirect request)
      :missing.or.invalid.webauthn.credentials (login-redirect request)
      :missing.or.invalid.session.cookie.credentials (login-redirect request)
      {:status http-status :body body})))

(defn ^:private exception-handler
  [error exception {:keys [locales] :as request}] ; exception and request come from reitit.
  (log/warn exception)
  (let [resource-id (keyword "platform" (name error))
        resolved-error-message (resolve-message locales resource-id)
        {:keys [code message]} (split-error-message resolved-error-message)
        body {:code code :reference (random-uuid) :message message :uri (:uri request)}]
    {:status internal-server-error :body body}))

(defn ^:private create-coercion-handler
  [status]
  (fn [exception {:keys [uri] :as request}]
    (log/warn exception)
    (let [{:keys [humanized] :as coercion-error} (coercion/encode-error (ex-data exception))]
      {:status status :body {:reference (random-uuid) :message humanized :uri uri}})))

(def exceptions-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers ;; reitit default handlers
    {;;
     ;; Catch these exceptions and deal using the exception-info-handler (below)
     ;;
     clojure.lang.ExceptionInfo exception-info-handler
     ;;
     ;; Catch these exceptions and deal using the exception handler (below)
     ;;
     java.io.IOException (partial exception-handler :service.unavailable)
     java.lang.IllegalArgumentException (partial exception-handler :service.unavailable)
     java.sql.SQLException (partial exception-handler :database.unavailable)
     ;;
     ::coercion/request-coercion (create-coercion-handler bad-request)
     ::coercion/response-coercion (create-coercion-handler internal-server-error)
     ;;
     ;; Catch everything else...
     ;;
     ::exception/default (partial exception-handler :general.exception)
     ;;
     ;; this ↓ wraps every handler (above), including the retit
     ;; default handlers, exception-info-handler and the exception-handler.
     ;;
     ::exception/wrap (fn [handler exception request]
                        (let [response (handler exception request)]
                          (log/warn response)
                          response))})))
