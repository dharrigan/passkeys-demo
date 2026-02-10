(ns passkeys.demo.ui.middleware.csrf
  {:author "David Harrigan"}
  (:require
   [clojure.string :refer [blank?]]
   [clojure.tools.logging :as log]
   [passkeys.demo.core.errors.impl :refer [throw-bad-request-exception]]
   [passkeys.demo.core.security.request-errors :as request-errors]
   [passkeys.demo.system.config.impl :as config]))

(set! *warn-on-reflection* true)

(def ^:private safe-methods #{:get :head :options})
(def ^:private sec-fetch-site-header "sec-fetch-site")
(def ^:private origin-header "origin")
(def ^:private allowed-sec-fetch-site-values #{"same-origin" "same-site" "none"})

(defn ^:private get-sec-fetch-site-header
  [{:keys [headers] :as request}]
  (get headers sec-fetch-site-header (get headers "Sec-Fetch-Site")))

(defn ^:private get-origin-header
  [{:keys [headers] :as request}]
  (get headers origin-header (get headers "Origin")))

(defn ^:private allowed-sec-fetch-site
  [request]
  (when-let [sec-fetch-site (get-sec-fetch-site-header request)] ;; if no sec-fetch-site header is found, drop through to checking the origin
    (if (contains? allowed-sec-fetch-site-values sec-fetch-site)
      (do (log/debugf "Sec-Fetch-Site header '%s' is contained within allowed sec-fetch-site values '%s'." sec-fetch-site allowed-sec-fetch-site-values)
          true)
      (do (log/debugf "Sec-Fetch-Site header '%s' is NOT contained within allowed sec-fetch-site values '%s'." sec-fetch-site allowed-sec-fetch-site-values)
          (throw-bad-request-exception request-errors/missing-or-invalid-sec-fetch-site-header)))))

(defn ^:private allowed-origin
  [request system]
  (when-let [origin (get-origin-header request)] ;; if no origin header is found, drop through to pre-2020 browser/script request
    (let [allowed-origins (config/allowed-origins system)]
      (if (contains? allowed-origins origin)
        (do (log/debugf "Origin header '%s' is contained within allowed origins '%s'." origin allowed-origins)
            true)
        (do (log/debugf "Origin header '%s' is NOT contained within allowed origins '%s'." origin allowed-origins)
            (throw-bad-request-exception request-errors/missing-or-invalid-origin-header))))))

(defn ^:private no-sec-fetch-site-and-origin-headers
  [request]
  (and (blank? (get-sec-fetch-site-header request)) (blank? (get-origin-header request))))

;;
;; If neither Sec-Fetch-Site *OR* Origin headers are present, this is a request
;; that is same-origin or not a browser request (i.e., a script - and therefore not
;; vunerable to CSRF attacks).
;;
(defn ^:private request-allowed
  [{:keys [request-method] :as request} system]
  (or (contains? safe-methods request-method)
      (allowed-sec-fetch-site request)
      (allowed-origin request system)
      (no-sec-fetch-site-and-origin-headers request)))

(defn ^:private with-csrf
  [handler]
  (fn [{:keys [system] :as request}]
    (if (request-allowed request system)
      (handler request)
      (do (log/debug "Request is potentially unsafe! Request denied!")
          (throw-bad-request-exception request-errors/unsafe-request)))))

;;
;; https://blog.miguelgrinberg.com/post/csrf-protection-without-tokens-or-hidden-form-fields
;;
;; https://words.filippo.io/csrf/#protecting-against-csrf-in-2025
;;
(def csrf-middleware
  {:name ::csrf
   :wrap with-csrf})
