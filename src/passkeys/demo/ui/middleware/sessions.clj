(ns passkeys.demo.ui.middleware.sessions
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.cache.impl :as cache]
   [passkeys.demo.core.utils.text :as text-utils]
   [passkeys.demo.system.config.impl :as config]
   [passkeys.demo.system.constants :refer [passkeys-demo-session]]
   [passkeys.demo.ui.utils.cookie :as cookie-utils]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.store :refer [SessionStore]]))

(set! *warn-on-reflection* true)

(def ^:private one-hour-in-seconds (* 1000 60 60)) ;; 3600
(def ^:private session-expiry-in-seconds {:expiry-seconds one-hour-in-seconds})

(defn ^:private get-session
  [session-id {{{:keys [connection]} :sessions-cache} :components :as system}]
  (-> (str passkeys-demo-session session-id)
      (cache/redis-get connection)))

(defn ^:private save-session
  [session-id token {{{:keys [connection]} :sessions-cache} :components :as system}]
  (-> (str passkeys-demo-session session-id)
      (cache/redis-put token session-expiry-in-seconds connection)))

(defn ^:private delete-session
  [session-id {{{:keys [connection]} :sessions-cache} :components :as system}]
  (-> (str passkeys-demo-session session-id)
      (cache/redis-del connection)))

(deftype ^:private RedisStore [system]
  SessionStore
  (read-session [_store key]
    (and key (get-session key system)))
  (write-session [_store key token]
    (let [session-id (or key (text-utils/random-id))]
      (save-session session-id token system)
      session-id))
  (delete-session [_store key]
    (delete-session key system)))

(defn ^:private with-session
  [handler system]
  (let [{{:keys [cookie-name]} :session} (config/cookies system)]
    (wrap-session handler {:store (->RedisStore system)
                           :cookie-name cookie-name
                           :cookie-attrs (cookie-utils/cookie-defaults one-hour-in-seconds)})))

(def sessions-middleware
  {:name ::sessions
   :wrap with-session})
